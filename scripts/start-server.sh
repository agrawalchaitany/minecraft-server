#!/usr/bin/env bash
set -euo pipefail

log() { echo "[start-server] $*"; }
fail() { log "ERROR: $*"; exit 1; }

cd /data

case "${EULA:-false}" in
  true|TRUE|True) echo "eula=true" > eula.txt ;;
  *) log "You must accept the Minecraft EULA (https://aka.ms/MinecraftEULA) by setting EULA=TRUE in .env"; exit 1 ;;
esac

# RCON is internal (safe saves, backups, console). Without a configured password, generate one and keep it in
# /data so the backup container and later restarts use the same value.
if [ -z "${RCON_PASSWORD:-}" ]; then
  if [ ! -s .rcon-password ]; then
    (umask 077; head -c 24 /dev/urandom | od -An -tx1 | tr -d ' \n' > .rcon-password)
    log "Generated an internal RCON password (stored in /data/.rcon-password)"
  fi
  RCON_PASSWORD="$(cat .rcon-password)"
  export RCON_PASSWORD
fi
if [ "$RCON_PASSWORD" = "change-me-to-a-long-random-string" ]; then fail "Set RCON_PASSWORD in .env to your own random value, or leave it empty to generate one"; fi
# server.properties treats backslashes as escapes and trims spaces, so such passwords would silently not match.
case "$RCON_PASSWORD" in
  *\\*|" "*|*" ") fail "RCON_PASSWORD must not contain backslashes or start/end with spaces (try: openssl rand -hex 24)" ;;
esac

# Validate settings up front: a typo here would otherwise become a crash loop or a silently disabled feature.
MEMORY="${MEMORY:-2G}"
[[ "$MEMORY" =~ ^[0-9]+[MmGg]$ ]] || fail "MEMORY must look like 2G or 1536M (got '$MEMORY')"
for var in WATCHDOG_INTERVAL WATCHDOG_FAILURES; do
  [[ "${!var:-1}" =~ ^[0-9]+$ ]] || fail "$var must be a whole number (got '${!var}')"
done
if [ "${PLUGIN_AUTO_UPDATE:-false}" = "true" ]; then
  [[ "${PLUGIN_UPDATE_TIME:-04:00}" =~ ^([01][0-9]|2[0-3]):[0-5][0-9]$ ]] \
    || fail "PLUGIN_UPDATE_TIME must be HH:MM in 24-hour time (got '$PLUGIN_UPDATE_TIME')"
fi

touch server.properties
# Non-ASCII characters are written as \uXXXX escapes, which every Minecraft version reads correctly.
escape_unicode() { perl -CSA -e 'my $v = $ARGV[0]; $v =~ s/([^\x00-\x7f])/ord($1) > 0xFFFF
  ? sprintf("\\u%04x\\u%04x", 0xD800 + ((ord($1) - 0x10000) >> 10), 0xDC00 + ((ord($1) - 0x10000) & 0x3FF))
  : sprintf("\\u%04x", ord($1))/ge; print $v' "$1"; }
set_prop() {
  KEY="$1" VALUE="$(escape_unicode "$2")" awk '
    BEGIN { k = ENVIRON["KEY"]; v = ENVIRON["VALUE"]; done = 0 }
    index($0, k "=") == 1 { print k "=" v; done = 1; next }
    { print }
    END { if (!done) print k "=" v }
  ' server.properties > server.properties.tmp
  mv server.properties.tmp server.properties
}
set_from_env() { if [ -n "${!1:-}" ]; then set_prop "$2" "${!1}"; fi; }

set_prop server-port 25565
# The container must listen on all interfaces, or the published port and the healthcheck can't reach it.
set_prop server-ip ""
set_prop enable-rcon true
set_prop rcon.port "${RCON_PORT:-25575}"
set_prop rcon.password "$RCON_PASSWORD"
set_from_env MOTD motd
set_from_env MAX_PLAYERS max-players
set_from_env DIFFICULTY difficulty
set_from_env GAMEMODE gamemode
set_from_env ONLINE_MODE online-mode
set_from_env VIEW_DISTANCE view-distance
set_from_env SIMULATION_DISTANCE simulation-distance
set_from_env LEVEL_NAME level-name
set_from_env SEED level-seed
set_from_env PVP pvp
set_from_env WHITELIST white-list
set_from_env WHITELIST enforce-whitelist

# Memory: heap is fixed (Xms = Xmx) so the JVM never has to grow/shrink mid-game.
mem_mb=${MEMORY%[MmGg]}
case "$MEMORY" in *[Gg]) mem_mb=$((mem_mb * 1024)) ;; esac

# The heap plus JVM overhead must fit in the container limit, or the kernel kills the server (exit 137) in a loop.
limit_bytes="$(cat /sys/fs/cgroup/memory.max 2>/dev/null || echo max)"
if [[ "$limit_bytes" =~ ^[0-9]+$ ]]; then
  limit_mb=$((limit_bytes / 1048576))
  if [ $((mem_mb + 512)) -gt "$limit_mb" ]; then
    fail "MEMORY=$MEMORY does not fit in the container limit of ${limit_mb}M. Set CONTAINER_MEMORY to at least $(( (mem_mb + 1024) / 1024 ))G, or lower MEMORY."
  fi
fi

# Aikar's G1GC flags; the large-heap variant is recommended at 12G and above.
if [ "$mem_mb" -ge 12288 ]; then
  g1="-XX:G1NewSizePercent=40 -XX:G1MaxNewSizePercent=50 -XX:G1HeapRegionSize=16M -XX:G1ReservePercent=15 -XX:InitiatingHeapOccupancyPercent=20"
else
  g1="-XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20 -XX:InitiatingHeapOccupancyPercent=15"
fi

# shellcheck disable=SC2206
jvm=(
  -Xms"$MEMORY" -Xmx"$MEMORY"
  -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200
  -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch
  $g1
  -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 -XX:G1MixedGCLiveThresholdPercent=90
  -XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem
  -XX:MaxTenuringThreshold=1
  -XX:+ExitOnOutOfMemoryError
  ${JVM_OPTS:-}
)

# shellcheck source=/dev/null
. /opt/minecraft/paper-version.env

# Worlds can be upgraded but not safely downgraded: back up before upgrades, refuse downgrades.
check_world_version() {
  local level recorded newest
  level="$(sed -n 's/^level-name=//p' server.properties)"
  recorded="$(cat .mc-version 2>/dev/null || true)"
  if [ -f "${level:-world}/level.dat" ] && [ -n "$recorded" ] && [ "$recorded" != "$PAPER_VERSION" ]; then
    newest="$(printf '%s\n%s\n' "$recorded" "$PAPER_VERSION" | sort -V | tail -n1)"
    if [ "$newest" = "$recorded" ]; then
      if [ "${ALLOW_DOWNGRADE:-false}" != "true" ]; then
        log "This world was last run on Minecraft $recorded, but the server is now $PAPER_VERSION."
        log "Opening a world in an older version can corrupt it, so the server will not start."
        log "Set MC_VERSION=$recorded in .env (or restore a backup made on $PAPER_VERSION) and run 'make up'."
        exit 1
      fi
      log "WARNING: downgrading world from $recorded to $PAPER_VERSION because ALLOW_DOWNGRADE=true"
    else
      log "Upgrading world from Minecraft $recorded to $PAPER_VERSION; taking a backup first"
      backup-now
    fi
  fi
  echo "$PAPER_VERSION" > .mc-version
}
check_world_version

# Online and offline mode give players different UUIDs; when the mode changes, move their data across.
migrate_players() {
  [ "${MIGRATE_PLAYER_DATA:-true}" = "true" ] || return 0
  local mode=online state=.player-uuid-mode rc=0
  grep -qx 'online-mode=false' server.properties && mode=offline
  [ -f "$state" ] && [ "$(cat "$state")" = "$mode" ] && return 0

  log "Checking player data for $mode mode"
  java -jar /opt/minecraft/mctool.jar migrate-players /data "$mode" || rc=$?
  if [ "$rc" = 3 ]; then
    log "Backing up before migrating player data"
    backup-now
    java -jar /opt/minecraft/mctool.jar migrate-players /data "$mode" --apply || rc=$?
    [ "$rc" = 3 ] && rc=0
  fi
  if [ "$rc" != 0 ]; then
    log "Player data migration failed; not starting so no data is split across two ids. Fix the error above or set MIGRATE_PLAYER_DATA=false."
    exit 1
  fi
  echo "$mode" > "$state"
}
migrate_players

mkdir -p plugins
if [ "${ENFORCE_USERNAME_RULES:-true}" = "true" ]; then
  cp /opt/minecraft/plugins/UsernameRules.jar plugins/
else
  rm -f plugins/UsernameRules.jar
fi

# Install/update plugins from plugins.txt; never block startup on a plugin download problem.
plugins sync || log "WARNING: plugin sync failed; starting with the plugins already installed"

# shellcheck source=/dev/null
. /opt/minecraft/paper-version.env
log "Starting Paper $PAPER_VERSION build $PAPER_BUILD with ${MEMORY} heap"

java "${jvm[@]}" -jar /opt/minecraft/paper.jar --nogui <&0 &
pid=$!

stopping=0
shutdown() {
  [ "$stopping" = 1 ] && return
  stopping=1
  log "Stop requested; saving worlds and shutting down"
  if ! rcon-cli stop >/dev/null 2>&1; then
    log "RCON unavailable, sending SIGTERM to server"
    kill -TERM "$pid" 2>/dev/null || true
  fi
}
trap shutdown TERM INT

# Hang watchdog: once the server has answered a ping, kill it after repeated failures so Docker restarts it.
watchdog() {
  local interval="${WATCHDOG_INTERVAL:-60}" max="${WATCHDOG_FAILURES:-5}" fails=0 up=0
  while kill -0 "$pid" 2>/dev/null; do
    sleep "$interval"
    [ "$stopping" = 1 ] && return
    if mc-health >/dev/null 2>&1; then up=1; fails=0; continue; fi
    [ "$up" = 1 ] || continue
    fails=$((fails + 1))
    log "Watchdog: server not responding ($fails/$max)"
    if [ "$fails" -ge "$max" ]; then
      log "Watchdog: server is hung, killing it"
      kill -TERM "$pid" 2>/dev/null || true
      sleep 60
      kill -KILL "$pid" 2>/dev/null || true
      return
    fi
  done
}
if [ "${WATCHDOG_INTERVAL:-60}" != "0" ]; then watchdog & fi

say() { rcon-cli "say $*" >/dev/null 2>&1 || true; }

# Daily at PLUGIN_UPDATE_TIME (container TZ): stage plugin updates; if any, warn players, back up, and restart.
plugin_auto_update() {
  local at="${PLUGIN_UPDATE_TIME:-04:00}" now target rc
  while kill -0 "$pid" 2>/dev/null; do
    now=$(date +%s)
    target=$(date -d "today $at" +%s)
    [ "$target" -le "$now" ] && target=$(date -d "tomorrow $at" +%s)
    sleep $((target - now))
    kill -0 "$pid" 2>/dev/null || return
    rc=0
    plugins update || rc=$?
    [ "$rc" = 0 ] || continue
    log "Plugin updates staged; restarting in 5 minutes"
    say "Server restarting in 5 minutes to update plugins"
    sleep 240; say "Server restarting in 1 minute to update plugins"
    sleep 50;  say "Server restarting in 10 seconds"
    sleep 10
    backup-now || log "WARNING: pre-update backup failed"
    rcon-cli stop >/dev/null 2>&1 || kill -TERM "$pid"
    return
  done
}
if [ "${PLUGIN_AUTO_UPDATE:-false}" = "true" ]; then plugin_auto_update & fi

status=0
while kill -0 "$pid" 2>/dev/null; do
  wait "$pid" && status=0 || status=$?
done
log "Server exited with status $status"
exit "$status"
