<!-- language-switcher -->
**English** · [हिन्दी](hi/operations.md) · [Español](es/operations.md) · [简体中文](zh-CN/operations.md) · [Português (BR)](pt-BR/operations.md) · [日本語](ja/operations.md) · [한국어](ko/operations.md)

# <img src="assets/icons/operations.svg" width="28" height="28" alt=""> Operations

Day-to-day tasks. Run all commands from the project folder.

## Command reference

| Command | What it does |
|---|---|
| `make up` | Build if needed, apply `.env`, and start everything |
| `make down` | Stop everything gracefully (worlds are saved) |
| `make restart` | Gracefully restart the game server (keeps current settings) |
| `make status` | Container state and health |
| `make logs` | Follow the logs (<kbd>Ctrl</kbd>+<kbd>C</kbd> stops following, not the server) |
| `make console` | Interactive server console (type `exit` to leave) |
| `make cmd C="…"` | Run one server command |
| `make backup` | Take a backup now |
| `make backups` | List backups |
| `make restore FILE=…` | Restore a backup; see [Backups and restore](backups.md) |
| `make update` | Rebuild with the newest Paper build of your Minecraft version |
| `make versions` | List Minecraft versions and the Java each needs |
| `make plugins` | Show installed plugins and why each one is there |
| `make update-plugins` | Apply `plugins/plugins.txt`; see [Plugins](plugins.md) |

## Is it healthy?

```bash
make status
```

`(healthy)` means the server is answering. During the first start, generating a new world can take a while; the health check allows five minutes. See [Reliability](reliability.md#health-check) for what "healthy" means.

## Server console

```bash
make console            # interactive
make cmd C="list"       # one command
make cmd C="op Steve"
make cmd C="whitelist add Steve"
make cmd C="say Restarting soon"
```

You can also attach to the raw console with `docker compose attach mc`. Detach with <kbd>Ctrl</kbd>+<kbd>P</kbd> then <kbd>Ctrl</kbd>+<kbd>Q</kbd>. Pressing <kbd>Ctrl</kbd>+<kbd>C</kbd> there stops the server, gracefully.

## Logs

```bash
make logs                                  # all services, follow
docker compose logs mc --since 1h          # game server, last hour
docker compose logs backup                 # backup history
docker compose exec mc ls logs             # Paper's own log files
```

Docker keeps up to 5 × 10 MB of logs per container, so logs never fill the disk.

## Editing server files

The server's files live in the `mc-data` volume. To edit one:

```bash
docker compose cp mc:/data/config/paper-global.yml .
# edit it
docker compose cp paper-global.yml mc:/data/config/paper-global.yml
make restart
```

Settings that `.env` controls (see the [configuration reference](configuration.md#gameplay)) are overwritten on each start. Change those in `.env` instead.

## Updating

| What | How |
|---|---|
| Newer Paper build, same Minecraft version | `make update` |
| Newer Minecraft version | Change `MC_VERSION`, then `make up`. A backup is taken before the world is upgraded. See [Minecraft versions](minecraft-versions.md). |
| Plugins | `make update-plugins`. See [Plugins](plugins.md). |
| This project | Replace the project files (keep `.env`, `plugins/plugins.txt`, and `backups/`), then `make up` |

## Disk usage

```bash
docker system df -v | grep mc-data     # world volume size
du -sh backups                         # backups
```

`BACKUP_KEEP` controls how much space backups use. Backups refuse to run if they could fill the disk; see [Backups and restore](backups.md#disk-space-safety).

## Starting over

To delete the world and all server data (backups in `./backups` are kept):

```bash
make backup                 # optional: keep a final copy
docker compose down -v      # -v deletes the mc-data volume
make up
```
