#!/usr/bin/env bash
# Starts the Velocity proxy: players connect here, log in (VeloAuth), then move between the servers.
set -euo pipefail

log() { echo "[start-proxy] $*"; }
fail() { log "ERROR: $*"; exit 1; }

cd /data
# shellcheck source=/dev/null
. /opt/velocity/velocity-version.env

[[ "${PROXY_SECRET:-}" =~ ^[A-Za-z0-9_-]{16,}$ ]] \
  || fail "PROXY_SECRET must be set in .env to at least 16 letters, numbers, - or _ ('make up' generates one)"
(umask 077; printf '%s' "$PROXY_SECRET" > forwarding.secret)

games="$(java -XX:TieredStopAtLevel=1 -Xmx64m -jar /opt/velocity/mctool.jar games "${NETWORK_GAMES_FILE:-/config/network/games.yml}")" \
  || fail "could not read games.yml"

# Velocity's own defaults for a new install; the settings this network depends on are rewritten on every start,
# anything else you change in velocity.toml is kept.
[ -s velocity.toml ] || cp /opt/velocity/default-velocity.toml velocity.toml

toml_string() { local v="${1//\\/\\\\}"; printf '"%s"' "${v//\"/\\\"}"; }
servers="limbo = \"limbo:25565\"
lobby = \"lobby:25565\"
$(while read -r id address; do [ -n "$id" ] && printf '%s = "%s"\n' "$id" "$address"; done <<<"$games")
try = [\"lobby\"]"
top="bind=\"0.0.0.0:25565\"
online-mode=${PROXY_ONLINE_MODE:-false}
force-key-authentication=false
player-info-forwarding-mode=\"modern\"
forwarding-secret-file=\"forwarding.secret\"
show-max-players=${MAX_PLAYERS:-100}"
[ -n "${PROXY_MOTD:-}" ] && top="$top
motd=$(toml_string "$PROXY_MOTD")"

TOP="$top" SERVERS="$servers" awk '
  BEGIN {
    n = split(ENVIRON["TOP"], kv, "\n")
    for (i = 1; i <= n; i++) { eq = index(kv[i], "="); k = substr(kv[i], 1, eq - 1); want[k] = substr(kv[i], eq + 1); order[i] = k }
  }
  function flush_top(   i) { if (topdone) return; for (i = 1; i <= n; i++) if (!(order[i] in seen)) print order[i] " = " want[order[i]]; topdone = 1 }
  /^[ \t]*\[/ {
    flush_top()
    section = $0; gsub(/[ \t]/, "", section)
    print
    if (section == "[servers]") { print ENVIRON["SERVERS"]; print "" }
    skip = (section == "[servers]" || section == "[forced-hosts]")
    next
  }
  skip { next }
  section == "" && match($0, /^[A-Za-z0-9_-]+[ \t]*=/) {
    k = $0; sub(/[ \t]*=.*/, "", k)
    if (k in want) { print k " = " want[k]; seen[k] = 1; next }
  }
  { print }
  END { flush_top() }
' velocity.toml > velocity.toml.tmp && mv velocity.toml.tmp velocity.toml
log "Servers: limbo lobby $(awk '{printf "%s ", $1}' <<<"$games")"

mkdir -p plugins
tool=(java -XX:TieredStopAtLevel=1 -Xmx128m -jar /opt/velocity/mctool.jar)
"${tool[@]}" plugins sync "${PLUGINS_MANIFEST:-/config/proxy/plugins.txt}" /data/plugins "$VELOCITY_VERSION" velocity \
  || log "WARNING: plugin sync failed; starting with the plugins already installed"

# Heap: the container limit minus room for the JVM itself, unless PROXY_MEMORY is set.
heap="${PROXY_MEMORY:-}"
if [ -z "$heap" ]; then
  limit="$(cat /sys/fs/cgroup/memory.max 2>/dev/null || echo max)"
  if [[ "$limit" =~ ^[0-9]+$ ]]; then heap="$(( limit / 1048576 - 256 ))M"; else heap=512M; fi
fi
[[ "$heap" =~ ^[0-9]+[MmGg]$ ]] || fail "PROXY_MEMORY must look like 512M or 1G (got '$heap')"

log "Starting Velocity $VELOCITY_VERSION build $VELOCITY_BUILD with $heap heap"
# Velocity's recommended flags (docs.papermc.io/velocity/tuning). exec: Velocity itself handles SIGTERM.
exec java -Xms"$heap" -Xmx"$heap" -XX:+UseG1GC -XX:G1HeapRegionSize=4M -XX:+UnlockExperimentalVMOptions \
  -XX:+ParallelRefProcEnabled -XX:+AlwaysPreTouch -XX:MaxInlineLevel=15 -XX:+ExitOnOutOfMemoryError \
  ${JVM_OPTS:-} -jar /opt/velocity/velocity.jar
