#!/usr/bin/env bash
set -euo pipefail

log() { echo "[backup $(date -u +%FT%TZ)] $*"; }
fail() { log "ERROR: $*"; exit 1; }

BACKUP_DIR="${BACKUP_DIR:-/backups}"
KEEP="${BACKUP_KEEP:-24}"
[[ "$KEEP" =~ ^[1-9][0-9]*$ ]] || fail "BACKUP_KEEP must be 1 or more (got '$KEEP')"
mkdir -p "$BACKUP_DIR"

# One backup at a time: a second one would re-enable autosave while the first is still reading the world.
exec 9>"$BACKUP_DIR/.backup.lock"
flock -w 900 9 || fail "another backup has been running for 15 minutes; giving up"
rm -f "$BACKUP_DIR"/.mc-*.partial

name="mc-$(date -u +%Y%m%d-%H%M%S).tar.gz"
[ -e "$BACKUP_DIR/$name" ] && name="${name%.tar.gz}-$$.tar.gz"
tmp="$BACKUP_DIR/.$name.partial"
excludes=(--exclude=./cache --exclude=./libraries --exclude=./versions --exclude=./logs --exclude=./plugins/.managed/pending --exclude=./plugins/spark/tmp)

# A backup that fills the disk can stop the server from saving the world, so check for room first.
latest="$(ls -1t "$BACKUP_DIR"/mc-*.tar.gz 2>/dev/null | head -n1 || true)"
if [ -n "$latest" ]; then
  need_kb=$(( $(du -k "$latest" | cut -f1) * 12 / 10 ))
else
  need_kb=$(( $(du -sk /data 2>/dev/null | cut -f1) * 6 / 10 ))
fi
free_kb="$(df -Pk "$BACKUP_DIR" | awk 'NR == 2 {print $4}')"
if [ "$free_kb" -lt $((need_kb + 512 * 1024)) ]; then
  fail "not enough disk space: about $((need_kb / 1024))M needed plus 512M headroom, $((free_kb / 1024))M free. Lower BACKUP_KEEP or free space."
fi

# Pause autosave so region files are not rewritten while we read them.
if rcon-cli save-off >/dev/null 2>&1; then
  trap 'rcon-cli save-on >/dev/null 2>&1 || log "WARNING: could not re-enable autosave"; rm -f "$tmp"' EXIT
  rcon-cli save-all flush >/dev/null
  sleep 2
else
  trap 'rm -f "$tmp"' EXIT
  log "WARNING: RCON unreachable; backing up without pausing saves (fine if the server is stopped)"
fi

log "Creating $name"
rc=0
tar -czf "$tmp" -C /data "${excludes[@]}" . || rc=$?
# tar exit 1 means "a file changed while reading" (logs, usercache); the archive is still valid.
if [ "$rc" -gt 1 ]; then fail "tar failed with status $rc"; fi
gzip -t "$tmp" || fail "the new archive is corrupt; previous backups were kept"

mv "$tmp" "$BACKUP_DIR/$name"
log "Saved $BACKUP_DIR/$name ($(du -h "$BACKUP_DIR/$name" | cut -f1))"

ls -1t "$BACKUP_DIR"/mc-*.tar.gz 2>/dev/null | tail -n +"$((KEEP + 1))" | while read -r old; do
  log "Pruning $(basename "$old")"
  rm -f "$old"
done
