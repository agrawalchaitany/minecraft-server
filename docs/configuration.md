<!-- language-switcher -->
**English** · [हिन्दी](hi/configuration.md) · [Español](es/configuration.md) · [简体中文](zh-CN/configuration.md) · [Português (BR)](pt-BR/configuration.md) · [日本語](ja/configuration.md) · [한국어](ko/configuration.md)

# <img src="assets/icons/configuration.svg" width="28" height="28" alt=""> Configuration reference

All settings live in `.env` in the project folder. Start from the annotated template:

```bash
cp .env.example .env
```

## Applying changes

| You changed… | Run | Why |
|---|---|---|
| Any setting in `.env` | `make up` | Recreates the container with the new values. Your world is kept. |
| `MC_VERSION`, `PAPER_BUILD`, `JAVA_VERSION` | `make up` | These are build settings; `make up` rebuilds the image. |
| `plugins/plugins.txt` | `make update-plugins` | See [Plugins](plugins.md). |

`make restart` restarts with the *old* settings. `.env` is only read when a container is created.

Invalid values are rejected at startup with a message naming the setting, so a typo never turns into a crash loop. Check `make logs` if the server doesn't come up.

## Required

| Variable | Default | Description |
|---|---|---|
| `EULA` | `false` | Must be `TRUE` to accept the [Minecraft EULA](https://aka.ms/MinecraftEULA). |
| `RCON_PASSWORD` | empty | Optional internal password for safe saves, backups, and the console. Leave it empty and a random one is generated on first start and kept in the data volume as `.rcon-password`. If you set your own: no backslashes, and no leading or trailing spaces. `openssl rand -hex 24` makes a good one. |

## Version

| Variable | Default | Description |
|---|---|---|
| `MC_VERSION` | `latest` | Minecraft version: `latest` (newest stable Paper release) or an exact version such as `1.21.4`. Minecraft 1.17 or newer. List them with `make versions`. |
| `PAPER_BUILD` | `latest` | A specific Paper build number. Only valid with an exact `MC_VERSION`, because build numbers are per version. |
| `JAVA_VERSION` | *(unset)* | Normally left unset; Java 17, 21, or 25 is picked to match the Minecraft version. Set it only to force a newer Java, for example when a plugin needs it. |
| `ALLOW_DOWNGRADE` | `false` | Allow opening a world in an older Minecraft version than it was last run on. This can corrupt the world. See [Minecraft versions](minecraft-versions.md#downgrading). |

## Resources

| Variable | Default | Description |
|---|---|---|
| `MEMORY` | `2G` | Java heap size, such as `4G` or `1536M`. It is reserved at startup (`-Xms` = `-Xmx`). `auto` sizes it to the container's memory limit, leaving at least 640M for the JVM itself. |
| `CONTAINER_MEMORY` | `3G` | Hard memory limit for the container. Must be at least `MEMORY` + 512M; `MEMORY` + 1G is recommended. The server refuses to start if the heap doesn't fit. |
| `JVM_OPTS` | *(empty)* | Extra JVM flags, added after the built-in ones. |

Rough sizing: `2G` for a few friends, `4G` for about 10 players, `6G`–`8G` for 20+ players or heavy plugins.

## Network

| Variable | Default | Description |
|---|---|---|
| `SERVER_PORT` | `25565` | Port on the host that players connect to. Inside the container the server always uses 25565. |

## Gameplay

These are written to `server.properties` on every start. Leave a value **empty** to manage that key directly in `server.properties` instead.

| Variable | Default | `server.properties` key |
|---|---|---|
| `MOTD` | `A reliable Paper server` | `motd`. Any language and emoji are fine; `§` color codes work. |
| `MAX_PLAYERS` | `20` | `max-players` |
| `DIFFICULTY` | `normal` | `difficulty`: `peaceful`, `easy`, `normal`, `hard` |
| `GAMEMODE` | `survival` | `gamemode`: `survival`, `creative`, `adventure`, `spectator` |
| `ONLINE_MODE` | `true` | `online-mode`. `false` lets players without an official account join. See [Online and offline mode](offline-mode.md). |
| `ENFORCE_USERNAME_RULES` | `true` | Not a property. In offline mode, require Mojang-style usernames. |
| `VIEW_DISTANCE` | `10` | `view-distance` |
| `SIMULATION_DISTANCE` | `10` | `simulation-distance` |
| `LEVEL_NAME` | `world` | `level-name`. Changing it switches to (or creates) a different world. |
| `SEED` | *(empty)* | `level-seed`. Only affects newly generated worlds. |
| `PVP` | `true` | `pvp` |
| `WHITELIST` | `false` | `white-list` and `enforce-whitelist` |
| `MIGRATE_PLAYER_DATA` | `true` | Not a property. Keep player inventories when `ONLINE_MODE` changes. |
| `LEVEL_TYPE` | *(empty)* | `level-type`, e.g. `minecraft:flat`. Only affects newly generated worlds. |
| `GENERATOR_SETTINGS` | *(empty)* | `generator-settings`: JSON for the world type, e.g. the layers of a flat world. Only affects new worlds. |
| `GENERATE_STRUCTURES` | *(empty)* | `generate-structures` |
| `ALLOW_NETHER` | *(empty)* | `allow-nether` |
| `SPAWN_PROTECTION` | *(empty)* | `spawn-protection` (radius in blocks; `0` turns it off) |

### Behind a proxy

Used by the [lobby network](network.md); leave them unset for a single server.

| Variable | Description |
|---|---|
| `PROXY_SECRET` | Turns on Velocity modern forwarding with this secret. The server then runs in offline mode, accepts only logins forwarded by the proxy, and skips `ENFORCE_USERNAME_RULES` and `MIGRATE_PLAYER_DATA` (the proxy's login plugin handles both). |
| `NETWORK_ROLE` | `lobby`, `game`, or `limbo`. Installs the built-in `Network` plugin in that role. |

The server always sets these, whatever is in `server.properties`: `server-port=25565`, `server-ip=` (listen on all interfaces), `enable-rcon=true`, `rcon.port`, and `rcon.password`.

## Plugins

| Variable | Default | Description |
|---|---|---|
| `PLUGIN_AUTO_UPDATE` | `false` | Check for plugin updates daily and restart to apply them. See [Plugins](plugins.md#automatic-updates). |
| `PLUGIN_UPDATE_TIME` | `04:00` | When to check, as `HH:MM` 24-hour time in `TZ`. |
| `TZ` | `UTC` | Timezone, such as `Asia/Kolkata` or `Europe/London`. |

## Backups

| Variable | Default | Description |
|---|---|---|
| `BACKUP_INTERVAL` | `1h` | Time between scheduled backups: a number with `s`, `m`, `h`, or `d`, such as `30m` or `6h`. |
| `BACKUP_KEEP` | `24` | How many backups to keep (at least 1). Older ones are deleted. |

## Reliability

| Variable | Default | Description |
|---|---|---|
| `STOP_TIMEOUT` | `2m` | How long Docker waits for the world to save on shutdown before force-killing. Raise it for very large worlds. |
| `WATCHDOG_INTERVAL` | `60` | Seconds between hang checks. `0` disables the watchdog. |
| `WATCHDOG_FAILURES` | `5` | Failed checks in a row before a hung server is restarted. |

## File ownership

| Variable | Default | Description |
|---|---|---|
| `PUID` / `PGID` | `1000` | User and group that own server files and `./backups`. On Linux, set them to the output of `id -u` and `id -g`. |

## Advanced

| Variable | Default | Description |
|---|---|---|
| `RCON_PORT` | `25575` | Internal RCON port. Never published; change only if a plugin needs 25575. |
