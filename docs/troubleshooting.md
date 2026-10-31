<!-- language-switcher -->
**English** · [हिन्दी](hi/troubleshooting.md) · [Español](es/troubleshooting.md) · [简体中文](zh-CN/troubleshooting.md) · [Português (BR)](pt-BR/troubleshooting.md) · [日本語](ja/troubleshooting.md) · [한국어](ko/troubleshooting.md)

# <img src="assets/icons/troubleshooting.svg" width="28" height="28" alt=""> Troubleshooting

Start with:

```bash
make status          # is it running and healthy?
make logs            # what did it say?
```

Most problems produce a clear `ERROR:` line in the logs naming the setting or the cause.

## The server won't start

**"You must accept the Minecraft EULA"**
Set `EULA=TRUE` in `.env`, then `make up`.

**"Set RCON_PASSWORD in .env to your own random value"** or **"RCON_PASSWORD must not contain backslashes…"**
Leave `RCON_PASSWORD` empty in `.env` to have one generated, or use `openssl rand -hex 24`.

**"MEMORY … does not fit in the container limit"**
Raise `CONTAINER_MEMORY` to the value the message suggests, or lower `MEMORY`. On macOS, also check Docker Desktop's memory under **Settings → Resources**.

**"This world was last run on Minecraft X, but the server is now Y"**
You lowered `MC_VERSION`. Set it back, or restore a backup made on the older version. See [Minecraft versions](minecraft-versions.md#downgrading).

**"Player data migration failed; not starting…"**
Usually Mojang's API couldn't be reached while switching to online mode. Check the server's internet connection; Docker retries automatically. See [Online and offline mode](offline-mode.md#player-data-across-modes).

**Keeps restarting with exit code 137**
The container hit its memory limit. Raise `CONTAINER_MEMORY` (at least `MEMORY` + 1G), or lower `MEMORY`.

**"port is already allocated"**
Something else uses port 25565 on the host. Stop it, or set `SERVER_PORT=25566` and connect to that port.

## Build problems

**"Minecraft X needs Java N but the image is being built with Java M"**
You built with plain `docker compose`. Use `make up`, which picks Java automatically. If you set `JAVA_VERSION` yourself, remove it.

**"Minecraft X is not available on Paper" / "is too old"**
Pick a version from `make versions` (1.17 or newer).

**"Patching Paper failed (attempt N), retrying"**
A download from Mojang failed during the build; it retries automatically. If all attempts fail, check your internet connection and run `make up` again.

## Players can't connect

1. `make status` should show `(healthy)`.
2. Server on the same machine: connect to `localhost`.
3. Cloud server: open TCP 25565 in the host firewall (`sudo ufw allow 25565/tcp`) **and** in the provider's firewall or security group.
4. Home network: forward TCP 25565 on the router to the host.
5. Players without an official account: set `ONLINE_MODE=false`. See [Online and offline mode](offline-mode.md).
6. "Invalid username" or "already taken" in offline mode: see [username rules](offline-mode.md#username-rules).

## Unhealthy

During the first start, world generation can take a few minutes; health checks allow five. If it stays unhealthy, look in `make logs` for errors. A hung server is restarted automatically by the [watchdog](reliability.md#hang-watchdog).

## Plugins

**A plugin didn't load**
Check `make logs` for `[plugins] WARNING` lines. They explain why: no compatible version, missing dependency, needs newer Java, and so on. `make plugins` shows what's installed.

**"needs Java N (this server runs Java M)"**
No version of that plugin supports both your Minecraft version and its Java. Use a newer `MC_VERSION`, or set `JAVA_VERSION=N` if your Minecraft version runs on it.

**"requires the plugin X, which could not be found automatically"**
Find X on Modrinth or Hangar and add it to `plugins.txt`, or install it [by hand](plugins.md#plugins-from-other-sites).

**"another plugin install/update is already running"**
A scheduled update is in progress. Try again in a minute.

## Backups

**"not enough disk space"**
Free space on the host, or lower `BACKUP_KEEP`. Backups refuse to fill the disk, because a full disk can stop the server from saving the world.

**"another backup has been running for 15 minutes"**
A previous backup is stuck, usually on a very slow disk. Check `docker compose logs backup`.

**`./backups` permission denied (Linux)**
Set `PUID` and `PGID` in `.env` to `id -u` / `id -g`, then `make up`.

## Settings don't seem to apply

`make restart` keeps the old settings; run `make up` after editing `.env`. Gameplay settings in `.env` overwrite `server.properties` on every start, so leave a setting empty in `.env` if you want to manage it in `server.properties`.
