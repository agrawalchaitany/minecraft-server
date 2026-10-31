<div align="center">

<img src="docs/assets/banner.svg" alt="Minecraft Server: a self-built, production-ready Paper server for Docker" width="100%">

[![Minecraft](https://img.shields.io/badge/Minecraft-1.17%20to%20latest-62B47A)](docs/minecraft-versions.md)
[![Paper](https://img.shields.io/badge/Paper-stable%20builds-1f6feb)](https://papermc.io)
[![Java](https://img.shields.io/badge/Java-17%20%7C%2021%20%7C%2025%20(auto)-orange?logo=openjdk&logoColor=white)](docs/minecraft-versions.md#how-java-is-chosen)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)](https://docs.docker.com/compose/)
[![Platforms](https://img.shields.io/badge/platform-linux%2Famd64%20%7C%20linux%2Farm64%20%7C%20macOS-lightgrey)](docs/deployment.md)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

[Quick start](#quick-start) ·
[Features](#features) ·
[Documentation](docs/README.md) ·
[Troubleshooting](docs/troubleshooting.md)

<!-- language-switcher -->
**English** · [हिन्दी](docs/readme/README.hi.md) · [Español](docs/readme/README.es.md) · [简体中文](docs/readme/README.zh-CN.md) · [Português (BR)](docs/readme/README.pt-BR.md) · [日本語](docs/readme/README.ja.md) · [한국어](docs/readme/README.ko.md)

</div>

---

A Minecraft server in Docker: run any Minecraft: Java Edition version from 1.17 to the latest, on [Paper](https://papermc.io), with one command. The server restarts itself, saves the world properly, backs up on a schedule, keeps plugins compatible, and refuses settings that would break it.

Everything is built from scratch: no third-party server images or scripts. The only inputs are the official `eclipse-temurin` Java images and the official PaperMC, Mojang, Modrinth, and Hangar download services. Every server and plugin jar is checksum-verified.

## Quick start

### Ready-made image

No build needed: Minecraft 26.3 (latest stable) on Java 25, no plugins, for Intel/AMD and ARM.

```bash
docker run -d --name minecraft --restart unless-stopped \
  -p 25565:25565 -e EULA=TRUE -e MEMORY=2G \
  -v minecraft-data:/data \
  agrawalchaitany/minecraft-server:26.3
```

Setting `EULA=TRUE` accepts the [Minecraft EULA](https://aka.ms/MinecraftEULA). Connect to `localhost:25565`; open the console with `docker exec -it minecraft rcon-cli`. The same image is on GitHub Packages as `ghcr.io/agrawalchaitany/minecraft-server`.

With scheduled backups (into `./backups`), use the ready-made [compose file](quickstart/compose.yaml):

```bash
mkdir minecraft && cd minecraft
curl -fsSLO https://raw.githubusercontent.com/agrawalchaitany/minecraft-server/main/quickstart/compose.yaml
echo EULA=TRUE > .env
docker compose up -d
```

### Build from source

To choose the Minecraft version, add plugins, or change anything else, build it yourself:

```bash
cp .env.example .env
```

Edit `.env`:

```env
EULA=TRUE                         # accept https://aka.ms/MinecraftEULA
RCON_PASSWORD=                    # optional; generated automatically if empty
MC_VERSION=latest                 # or an exact version, e.g. 1.21.4 (see: make versions)
```

Start it:

```bash
make up
make status        # wait for (healthy), then connect to localhost:25565
```

Full walkthrough: **[Getting started](docs/getting-started.md)**.

## Features

<table>
<tr>
<td width="33%" valign="top" align="center">

<br><img src="docs/assets/icons/versions.svg" width="32" height="32" alt=""><br>**Any version**

Minecraft 1.17 to the latest. The matching Java (17, 21, or 25) is chosen automatically. Upgrades back up the world first; downgrades that would corrupt it are refused. [More](docs/minecraft-versions.md)
<br>&nbsp;

</td>
<td width="33%" valign="top" align="center">

<br><img src="docs/assets/icons/reliability.svg" width="32" height="32" alt=""><br>**Self-healing**

Restarts after crashes and out-of-memory errors. A health check, plus a watchdog that restarts a frozen server. [More](docs/reliability.md)
<br>&nbsp;

</td>
<td width="33%" valign="top" align="center">

<br><img src="docs/assets/icons/shutdown.svg" width="32" height="32" alt=""><br>**Safe shutdown**

Every stop saves all worlds through the server console first. [More](docs/reliability.md#graceful-shutdown)
<br>&nbsp;

</td>
</tr>
<tr>
<td width="33%" valign="top" align="center">

<br><img src="docs/assets/icons/backups.svg" width="32" height="32" alt=""><br>**Backups**

Scheduled and consistent while players are online, locked so two never overlap, verified, checked for disk space, and rotated. Taken automatically before upgrades, migrations, plugin updates, and restores. [More](docs/backups.md)
<br>&nbsp;

</td>
<td width="33%" valign="top" align="center">

<br><img src="docs/assets/icons/restore.svg" width="32" height="32" alt=""><br>**Safe restore**

One command. Checks the file first, takes a safety backup, and extracts to a staging folder so a failure can't lose the current world. [More](docs/backups.md#restoring)
<br>&nbsp;

</td>
<td width="33%" valign="top" align="center">

<br><img src="docs/assets/icons/plugins.svg" width="32" height="32" alt=""><br>**Plugin manager**

List plugins in `plugins.txt`. For each one, the newest version that really works on your Minecraft and Java is chosen, by inspecting the jar itself. Required dependencies are installed. Conflicts and duplicates are caught. Updates on demand or daily. [More](docs/plugins.md)
<br>&nbsp;

</td>
</tr>
<tr>
<td width="33%" valign="top" align="center">

<br><img src="docs/assets/icons/players.svg" width="32" height="32" alt=""><br>**Offline mode**

Let players without an official account join, with Mojang's username rules enforced and player inventories kept when switching modes. [More](docs/offline-mode.md)
<br>&nbsp;

</td>
<td width="33%" valign="top" align="center">

<br><img src="docs/assets/icons/guard.svg" width="32" height="32" alt=""><br>**Guard rails**

Every setting is validated at startup with a clear message. No crash loops from typos. [More](docs/reliability.md#configuration-validation)
<br>&nbsp;

</td>
<td width="33%" valign="top" align="center">

<br><img src="docs/assets/icons/security.svg" width="32" height="32" alt=""><br>**Least privilege**

Runs as a non-root user. The remote console is never exposed outside Docker. Logs are rotated.
<br>&nbsp;

</td>
</tr>
</table>

## Everyday commands

| Command | |
|---|---|
| `make up` | Start, or apply changes to `.env` |
| `make down` | Stop (worlds are saved) |
| `make status` / `make logs` | Health and logs |
| `make console` / `make cmd C="op Steve"` | Server console |
| `make backup` / `make restore FILE=…` | Back up and restore |
| `make update-plugins` / `make plugins` | Apply `plugins.txt` and show installed plugins |
| `make versions` / `make update` | List Minecraft versions; update Paper |

All commands: [Operations](docs/operations.md).

## Documentation

| | Guide | |
|:-:|---|---|
| <img src="docs/assets/icons/start.svg" width="20" height="20" alt=""> | [Getting started](docs/getting-started.md) | Running in five minutes |
| <img src="docs/assets/icons/architecture.svg" width="20" height="20" alt=""> | [Architecture](docs/architecture.md) | How it works, from `make up` to a running server |
| <img src="docs/assets/icons/configuration.svg" width="20" height="20" alt=""> | [Configuration](docs/configuration.md) | Every setting in `.env` |
| <img src="docs/assets/icons/operations.svg" width="20" height="20" alt=""> | [Operations](docs/operations.md) | Day-to-day tasks |
| <img src="docs/assets/icons/versions.svg" width="20" height="20" alt=""> | [Minecraft versions](docs/minecraft-versions.md) | Versions, Java, upgrades, and downgrades |
| <img src="docs/assets/icons/plugins.svg" width="20" height="20" alt=""> | [Plugins](docs/plugins.md) | `plugins.txt`, compatibility resolution, auto-updates, mods |
| <img src="docs/assets/icons/backups.svg" width="20" height="20" alt=""> | [Backups and restore](docs/backups.md) | How backups stay consistent and restores stay safe |
| <img src="docs/assets/icons/reliability.svg" width="20" height="20" alt=""> | [Reliability](docs/reliability.md) | Health checks, watchdog, shutdown, memory, validation |
| <img src="docs/assets/icons/players.svg" width="20" height="20" alt=""> | [Online and offline mode](docs/offline-mode.md) | Accountless players, username rules, player data |
| <img src="docs/assets/icons/architecture.svg" width="20" height="20" alt=""> | [Lobby network](docs/network.md) | A lobby with a game menu and game servers you switch on and off |
| <img src="docs/assets/icons/deployment.svg" width="20" height="20" alt=""> | [Deployment](docs/deployment.md) | Linux servers, macOS, firewalls, off-site backups |
| <img src="docs/assets/icons/edge-cases.svg" width="20" height="20" alt=""> | [Edge cases](docs/edge-cases.md) | Unusual situations and how they're handled |
| <img src="docs/assets/icons/troubleshooting.svg" width="20" height="20" alt=""> | [Troubleshooting](docs/troubleshooting.md) | Symptoms and fixes |

## Requirements

- Docker Desktop (macOS/Windows) or Docker Engine 24+ with the Compose plugin (Linux)
- `make`
- About 3 GB of free memory for the default settings

## Project layout

```
Dockerfile, compose.yaml, Makefile, .env.example
scripts/     startup, backup, restore, and helper scripts
tools/       dependency-free Java tools (RCON, versions, plugins, player migration)
plugins/     plugins.txt and the built-in plugins (username rules, network menu)
network/     optional lobby network: Velocity proxy, lobby, and switchable game servers
docs/        documentation
```

## FAQ

**How do I host a Minecraft server with Docker?**
Run the one-line `docker run` command in [Quick start](#quick-start), or use the [compose file](quickstart/compose.yaml) to add scheduled backups. Players connect to your machine's address on port 25565.

**Does it work on a Raspberry Pi, Apple Silicon, or Windows?**
Yes. The image is built for `linux/amd64` and `linux/arm64`, so it runs on Raspberry Pi 4/5, ARM cloud servers, Apple Silicon Macs, and Windows through Docker Desktop. See [Deployment](docs/deployment.md).

**Can I run a lobby with several game modes, like big public servers?**
Yes. [`network/`](network) runs a Velocity proxy, a lobby with a game menu, and separate Survival, BedWars, SkyWars, KitPvP, racing, and Anarchy servers that you switch on and off. Cracked and premium players are both supported. See [Lobby network](docs/network.md).

**Can I use plugins like EssentialsX, LuckPerms, or WorldEdit?**
Yes, any Paper, Spigot, or Bukkit plugin. List them in `plugins.txt` and the plugin manager installs the newest version that works with your Minecraft and Java version, plus required dependencies. See [Plugins](docs/plugins.md).

**Does it support Fabric, Forge, or Bedrock?**
No. It runs Paper, which loads plugins but not mods, and it is Java Edition only. See [Mods](docs/plugins.md#mods).

**Can players without a Minecraft account join?**
Yes, with offline mode (`ONLINE_MODE=false`). Username rules are still enforced, and inventories are kept when you switch modes. See [Online and offline mode](docs/offline-mode.md).

**How do I upgrade to a new Minecraft version?**
Change `MC_VERSION` in `.env` and run `make up`. The world is backed up first, and downgrades that would corrupt it are refused. See [Minecraft versions](docs/minecraft-versions.md).

## License

[MIT](LICENSE). The license covers this project's code, not Minecraft or Paper.

## Disclaimer

Not affiliated with Mojang Studios, Microsoft, or PaperMC. Minecraft is a trademark of Mojang Studios. Running the server requires accepting the [Minecraft EULA](https://aka.ms/MinecraftEULA).
