<!-- language-switcher -->
**English** · [हिन्दी](hi/edge-cases.md) · [Español](es/edge-cases.md) · [简体中文](zh-CN/edge-cases.md) · [Português (BR)](pt-BR/edge-cases.md) · [日本語](ja/edge-cases.md) · [한국어](ko/edge-cases.md)

# <img src="assets/icons/edge-cases.svg" width="28" height="28" alt=""> Edge cases

How the server behaves in unusual situations. Entries marked **tested** were reproduced on a real server and the behavior shown was observed.

## Configuration mistakes

| Situation | Behavior | |
|---|---|---|
| `EULA` not `TRUE` | Refuses to start and explains how to accept | tested |
| `RCON_PASSWORD` left as the placeholder | Refuses to start | tested |
| `RCON_PASSWORD` contains a backslash or edge spaces | Refuses to start. `server.properties` would silently change it, breaking safe saves. | tested |
| `MEMORY=2GB` (wrong format) | Refuses to start: "must look like 2G or 1536M" | tested |
| `MEMORY` larger than `CONTAINER_MEMORY` allows | Refuses to start and says which `CONTAINER_MEMORY` to set, instead of an out-of-memory kill loop | tested |
| `WATCHDOG_INTERVAL=1m` | Refuses to start: must be a whole number of seconds | tested |
| `PLUGIN_UPDATE_TIME=25:00` with auto-update on | Refuses to start, instead of auto-update silently not running | tested |
| `BACKUP_INTERVAL=abc` | Backup container stops with an error, instead of backing up non-stop | tested |
| `BACKUP_KEEP=0` | Backup refused, instead of deleting every backup including the new one | tested |
| `PAPER_BUILD` set with `MC_VERSION=latest` | Build stops: build numbers only make sense for an exact version | tested |
| `.env` saved with Windows line endings | `make` strips them when reading versions; save `.env` with LF endings to be safe | |
| Changed `.env` but ran `make restart` | Old settings stay; `make restart` doesn't re-read `.env`. Use `make up`. | |
| `MOTD` with accents, other scripts, or emoji | Written as `\uXXXX` escapes, so every Minecraft version shows it correctly | tested |
| `enable-status=false` (hidden from server lists) | Health check falls back to RCON; the server stays healthy | tested |
| `server-ip` set in `server.properties` | Reset to empty on start; inside Docker the server must listen on all interfaces | |

## Minecraft versions

| Situation | Behavior | |
|---|---|---|
| `MC_VERSION` doesn't exist (`9.9`) | `make` stops: not available, run `make versions` | tested |
| `MC_VERSION` too old (`1.16.5`) | `make` stops: 1.17 or newer is supported | tested |
| A version with only beta Paper builds | `make` stops: no stable build yet | |
| `latest` moves to a new Minecraft release | Happens only when the image is rebuilt. Pin `MC_VERSION` to avoid surprises. | |
| `MC_VERSION` raised | World backed up, then upgraded; plugins re-resolved | tested |
| `MC_VERSION` lowered | Refuses to start with instructions (downgrade guard) | tested |
| A backup from a newer version is restored | Refused by the downgrade guard on the next start | |
| `JAVA_VERSION` too old for the Minecraft version | `make` stops with the minimum Java needed | |
| Plain `docker compose up --build` with an old `MC_VERSION` | Build stops with "Build with 'make up'"; plain Compose defaults to Java 25 | |

## Backups and restore

| Situation | Behavior | |
|---|---|---|
| Manual and scheduled backup at the same moment | Run one after the other (shared lock), so autosave is never re-enabled mid-backup | tested |
| Not enough disk space | Backup refused with sizes in the message; the server keeps running safely | tested |
| Backup interrupted (container killed) | Only a hidden `.partial` file is left, and the next backup removes it. Finished backups are never partial. | |
| Archive corrupt after writing | Detected by `gzip -t`, discarded, and older backups are kept | |
| Two backups in the same second | The second gets a unique suffix instead of overwriting | |
| Server stopped during a scheduled backup | Backup proceeds without pausing saves (nothing is writing) | |
| `make restore` with a mistyped file name | Stops before touching anything; the server keeps running | tested |
| `make restore` with a corrupt file | Stops before touching anything (`gzip -t`) | |
| Extraction fails during restore (disk full) | Current data is untouched, because extraction happens in a staging folder first | |
| Restore while the server is somehow still running | Refused | |

## Plugins

| Situation | Behavior | |
|---|---|---|
| The plugin site says a version supports your Minecraft, but it needs newer Java | Detected from the jar; the newest version that works is used | tested |
| No version works on this server | Skipped with an explanation and a `JAVA_VERSION` hint; an installed copy that can't load is removed | |
| A dependency isn't declared on the site but is in `plugin.yml` | Found by name on Hangar or Modrinth, and accepted only if the jar's plugin name matches | |
| A dependency can't be found anywhere | Warning naming it; the server still starts | |
| Same plugin listed from Modrinth and Hangar | The first is kept, the second skipped, so Paper never sees duplicates | |
| Plugin also installed by hand | The managed copy is skipped | |
| Typo in a project name, or a deleted project | Warning; other plugins are unaffected | tested |
| Modrinth or Hangar down during startup | Warning; installed versions stay; retried at the next start | |
| Two plugin updates at once | The second is refused with "already running" | tested |
| Container stopped while applying staged plugins | The apply step is safe to repeat and finishes on the next start | |
| `plugins.txt` saved with a byte-order mark or Windows line endings | Handled | |
| `plugins.txt` missing | Plugins are left as they are (nothing is uninstalled) | |
| All lines removed from `plugins.txt` | Every managed plugin is uninstalled; hand-installed jars stay | |

## Players and modes

| Situation | Behavior | |
|---|---|---|
| Offline player joins as `steve` when `Steve` exists | Rejected: "already taken, log in as exactly Steve" | tested |
| Offline name with spaces, dashes, accents, or dots | Rejected with the rule explained | tested |
| Switch online → offline with existing players | Data moved to offline IDs, after a backup | tested |
| Switch offline → online, player has no account | Their data is kept under the offline ID for a later switch back | tested |
| Switch offline → online while Mojang is unreachable | Server doesn't start (prevents split data); Docker retries | |
| Player has data under both IDs | Newer file kept; older moved to `player-migration-conflicts/` | tested |
| Bedrock players via Geyser/Floodgate (`.Name`) | Rejected by username rules; set `ENFORCE_USERNAME_RULES=false` | |

## Runtime

| Situation | Behavior | |
|---|---|---|
| Server process crashes (`kill -9`) | Docker restarts it; healthy again within seconds | tested |
| Server freezes but the process stays alive | Watchdog restarts it after about 5 minutes of no response | |
| Very slow first start (world generation) | Watchdog waits for the first response; health check allows 5 minutes | |
| Java out of memory | Exits cleanly and Docker restarts it | |
| `/stop` typed in game | Saves and stops; Docker starts it again (use `make down` to keep it stopped) | |
| Very large world doesn't finish saving within 2 minutes | Raise `STOP_TIMEOUT` | |
| Host port 25565 already in use | `make up` fails with "port is already allocated"; set `SERVER_PORT` | |
| Host reboot | Containers return when Docker starts | |

## Known limits

- Only Minecraft: Java Edition 1.17 and newer, on Paper.
- Only Modrinth and Hangar are searched for plugins automatically.
- Plugin data stored under player UUIDs isn't migrated between online and offline mode.
- `make update-plugins`, `make backup`, and `make console` need the server container to be running.
- `make` needs Docker to be able to pull `eclipse-temurin:25-jdk` (once) to resolve versions.
