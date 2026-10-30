#!/usr/bin/env bash
# Run only while the game server is stopped (see `make restore`).
set -euo pipefail

file="${1:?usage: restore <backup file name in /backups>}"
src="/backups/$(basename "$file")"
[ -f "$src" ] || { echo "Backup not found: $src" >&2; ls -1 /backups >&2; exit 1; }

if mc-health >/dev/null 2>&1 || (RCON_HOST="${RCON_HOST:-mc}" rcon-cli list >/dev/null 2>&1); then
  echo "The server appears to be running; stop it before restoring." >&2
  exit 1
fi

echo "Checking $src"
gzip -t "$src"

echo "Taking a safety backup of current data first"
BACKUP_KEEP=100000 backup-now

# Extract next to the live data first, so a failed extraction (e.g. disk full) leaves the current world untouched.
staging=/data/.restoring
rm -rf "$staging"
mkdir -p "$staging"
echo "Restoring $src"
if ! tar -xzf "$src" -C "$staging"; then
  rm -rf "$staging"
  echo "Extraction failed; current data was left unchanged." >&2
  exit 1
fi

find /data -mindepth 1 -maxdepth 1 \
  ! -name cache ! -name libraries ! -name versions ! -name .restoring -exec rm -rf {} +
find "$staging" -mindepth 1 -maxdepth 1 -exec mv -t /data {} +
rmdir "$staging"
echo "Restore complete. Start the server again."
