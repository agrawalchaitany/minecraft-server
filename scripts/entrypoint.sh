#!/usr/bin/env bash
set -euo pipefail

PUID="${PUID:-1000}"
PGID="${PGID:-1000}"

# Start as root only long enough to fix volume ownership, then drop privileges.
if [ "$(id -u)" = "0" ]; then
  for dir in /data /backups; do
    if [ -d "$dir" ] && [ -w "$dir" ] && [ "$(stat -c %u:%g "$dir")" != "$PUID:$PGID" ]; then
      echo "[entrypoint] Fixing ownership of $dir to $PUID:$PGID"
      chown -R "$PUID:$PGID" "$dir"
    fi
  done
  exec setpriv --reuid="$PUID" --regid="$PGID" --clear-groups -- "$0" "$@"
fi

case "${1:-server}" in
  server)     exec start-server ;;
  backup)     exec backup-loop ;;
  backup-now) exec backup-now ;;
  restore)    shift; exec restore-backup "$@" ;;
  *)          exec "$@" ;;
esac
