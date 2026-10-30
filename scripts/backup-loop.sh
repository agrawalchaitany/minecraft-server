#!/usr/bin/env bash
set -uo pipefail

interval="${BACKUP_INTERVAL:-1h}"
# Reject bad values up front; otherwise `sleep` fails instantly and the loop would back up non-stop.
if ! [[ "$interval" =~ ^[0-9]+[smhd]?$ ]] || [[ "$interval" =~ ^0+[smhd]?$ ]]; then
  echo "[backup-loop] ERROR: BACKUP_INTERVAL must look like 30m, 6h or 1d (got '$interval')"
  exit 1
fi
echo "[backup-loop] Backing up every $interval, keeping ${BACKUP_KEEP:-24}"

trap 'exit 0' TERM INT
while true; do
  sleep "$interval" & wait $!
  backup-now || echo "[backup-loop] Backup failed; will retry next interval"
done
