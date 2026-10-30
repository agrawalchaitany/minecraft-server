<!-- language-switcher -->
**English** · [हिन्दी](hi/deployment.md) · [Español](es/deployment.md) · [简体中文](zh-CN/deployment.md) · [Português (BR)](pt-BR/deployment.md) · [日本語](ja/deployment.md) · [한국어](ko/deployment.md)

# <img src="assets/icons/deployment.svg" width="28" height="28" alt=""> Deployment

The same project runs on macOS (for testing or playing with friends) and on Linux servers (for something always-on). Images build natively for both Intel/AMD (`amd64`) and ARM (`arm64`, including Apple Silicon).

## Linux server (VPS, home server)

### 1. Install Docker

Install Docker Engine and the Compose plugin following Docker's instructions for your distribution, then:

```bash
sudo systemctl enable --now docker      # start Docker on boot
sudo usermod -aG docker "$USER"          # run docker without sudo (log out and in again)
```

### 2. Copy the project

```bash
git clone <your-repository> minecraft && cd minecraft
# or: scp -r Minecraft-Server/ user@server:~/minecraft
```

### 3. Configure

```bash
cp .env.example .env
id -u && id -g          # put these into PUID / PGID
```

In `.env`, set at least `EULA`, `RCON_PASSWORD`, `MC_VERSION` (pin an exact version), `MEMORY`, and `CONTAINER_MEMORY`.

| Server RAM | `MEMORY` | `CONTAINER_MEMORY` |
|---|---|---|
| 4 GB | `2G` | `3G` |
| 8 GB | `5G` | `6G` |
| 16 GB | `10G` | `12G` |

Leave about 1 GB for the operating system.

### 4. Open the game port

```bash
sudo ufw allow 25565/tcp
```

Also allow TCP 25565 in your cloud provider's firewall or security group, if it has one. **Never open 25575** (RCON); it's internal only.

### 5. Start

```bash
make up
```

Containers come back automatically after reboots.

## macOS

Install [Docker Desktop](https://www.docker.com/products/docker-desktop/), then follow [Getting started](getting-started.md). Things to know:

- Docker Desktop has its own memory limit (**Settings → Resources**). It must be larger than `CONTAINER_MEMORY`.
- Friends on your home network connect to your Mac's local IP. For friends outside it, forward TCP 25565 on your router to your Mac.
- Docker Desktop must be running for the server to run. Enable "Start Docker Desktop when you sign in" for it to come back after a restart.

## Off-site backups

`./backups` sits on the same disk as the server. Copy it elsewhere on a schedule, for example with `rsync` from cron on the server:

```bash
# crontab -e   (every day at 05:00)
0 5 * * * rsync -a --delete /home/me/minecraft/backups/ backup-host:/srv/minecraft-backups/
```

Any tool works (`rclone` to cloud storage, `restic`, and so on). Copy only finished `mc-*.tar.gz` files; the hidden `.partial` files are in-progress backups.

## Moving to another machine

1. `make backup` on the old machine.
2. Set up the project on the new machine with the same `.env` and `plugins/plugins.txt`.
3. Copy the backup into the new `./backups/`.
4. `make up`, then `make restore FILE=…`. See [Backups and restore](backups.md#restoring-on-a-new-machine).
5. Point players (or your DNS name) at the new address.

## Updating the project itself

Replace the project files with the new version, but keep `.env`, `plugins/plugins.txt`, and `backups/`. Then:

```bash
make up
```

Your world lives in the Docker volume and isn't affected by replacing the files.

## Security checklist

- Keep `ONLINE_MODE=true` unless you need offline mode, and read [the risks](offline-mode.md#the-risk-usernames-arent-verified) if you do.
- Use a long random `RCON_PASSWORD`. Never publish port 25575.
- Keep `.env` private; it contains the RCON password. It's excluded from git and from the Docker build.
- Keep the host updated, and use SSH keys instead of passwords.
- Make operators only of people who need it; `make cmd C="deop Name"` removes it.
- Pin `MC_VERSION` and review plugin updates before applying them.
