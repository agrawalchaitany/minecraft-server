<!-- language-switcher -->
**English** · [हिन्दी](hi/getting-started.md) · [Español](es/getting-started.md) · [简体中文](zh-CN/getting-started.md) · [Português (BR)](pt-BR/getting-started.md) · [日本語](ja/getting-started.md) · [한국어](ko/getting-started.md)

# <img src="assets/icons/start.svg" width="28" height="28" alt=""> Getting started

This guide takes you from nothing to a running Minecraft server you can join, in about five minutes.

## What you get

Running `make up` starts two containers:

- **`mc`**: a [Paper](https://papermc.io) Minecraft: Java Edition server. Paper is a faster, plugin-capable drop-in replacement for the official server.
- **`backup`**: a small companion container that backs up your world every hour.

The server restarts itself after crashes, saves the world properly when stopped, and refuses settings that would break it. You don't need to know Docker to run it day to day; everything is a `make` command.

## Requirements

| | |
|---|---|
| Docker | Docker Desktop (macOS, Windows), or Docker Engine 24+ with the Compose plugin (Linux) |
| `make` | Preinstalled on macOS and most Linux distributions |
| Memory | About 3 GB free for the default settings |
| Network | Internet access during setup to download Paper, Java, and plugins |

## Set up

**1. Create your settings file.**

```bash
cp .env.example .env
```

**2. Edit two values in `.env`.**

```env
EULA=TRUE
MC_VERSION=latest
```

- `EULA=TRUE` means you accept the [Minecraft EULA](https://aka.ms/MinecraftEULA). The server won't start without it.
- `RCON_PASSWORD` is optional. It is used internally to save the world safely during backups and shutdowns. Leave it empty and a random one is generated and kept in the data volume. It is never exposed outside Docker.
- `MC_VERSION` is the Minecraft version to run. Keep `latest`, or pick one from `make versions`, such as `1.21.4`. **Choose now:** worlds can be upgraded to a newer version later, but never moved back to an older one. See [Minecraft versions](minecraft-versions.md).

**3. Start it.**

```bash
make up
```

The first run downloads Java and Paper and builds the image, which takes a few minutes. You'll see which version was selected:

```
==> Minecraft 26.3 (Paper) on Java 25
```

**4. Wait until it's healthy.**

```bash
make status
```

When the `mc` container shows `(healthy)`, the server is ready. To watch it start, run `make logs` and press <kbd>Ctrl</kbd>+<kbd>C</kbd> to stop following. This doesn't stop the server.

## Connect

In Minecraft, choose **Multiplayer → Add Server** and enter:

- `localhost` if the server runs on the same computer, or
- the server's IP address if it runs elsewhere. On a cloud server, also open port 25565; see [Deployment](deployment.md).

## Make yourself an operator

```bash
make cmd C="op YourMinecraftName"
```

## Common next steps

| I want to… | Go to |
|---|---|
| Let friends without an official Minecraft account join | [Online and offline mode](offline-mode.md) |
| Add plugins | [Plugins](plugins.md) |
| Give the server more memory | `MEMORY` and `CONTAINER_MEMORY` in the [configuration reference](configuration.md#resources) |
| Run it on a cloud server | [Deployment](deployment.md) |
| See every command | [Operations](operations.md) |

## Changing settings later

Edit `.env`, then run:

```bash
make up
```

`make up` applies the new settings and keeps your world. (`make restart` only restarts with the *old* settings.)
