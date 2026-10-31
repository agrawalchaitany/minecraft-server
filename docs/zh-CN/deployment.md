<!-- language-switcher -->
[English](../deployment.md) · [हिन्दी](../hi/deployment.md) · [Español](../es/deployment.md) · **简体中文** · [Português (BR)](../pt-BR/deployment.md) · [日本語](../ja/deployment.md) · [한국어](../ko/deployment.md)

# <img src="../assets/icons/deployment.svg" width="28" height="28" alt=""> 部署

同一个项目既可以在 macOS 上运行（用于测试或和朋友一起玩），也可以在 Linux 服务器上运行（用于长期在线）。镜像会原生构建，支持 Intel/AMD（`amd64`）和 ARM（`arm64`，包括 Apple Silicon）。

## Linux 服务器（VPS、家用服务器）

### 1. 安装 Docker

按照 Docker 针对你所用发行版的说明安装 Docker Engine 和 Compose 插件，然后：

```bash
sudo systemctl enable --now docker      # start Docker on boot
sudo usermod -aG docker "$USER"          # run docker without sudo (log out and in again)
```

### 2. 复制项目

```bash
git clone <your-repository> minecraft && cd minecraft
# or: scp -r Minecraft-Server/ user@server:~/minecraft
```

### 3. 配置

```bash
cp .env.example .env
id -u && id -g          # put these into PUID / PGID
```

在 `.env` 中至少设置 `EULA`、`MC_VERSION`（固定为确切版本）、`MEMORY` 和 `CONTAINER_MEMORY`。

| 服务器内存 | `MEMORY` | `CONTAINER_MEMORY` |
|---|---|---|
| 4 GB | `2G` | `3G` |
| 8 GB | `5G` | `6G` |
| 16 GB | `10G` | `12G` |

为操作系统预留约 1 GB。

### 4. 开放游戏端口

```bash
sudo ufw allow 25565/tcp
```

如果你的云服务商有防火墙或安全组，也要在那里放行 TCP 25565。**永远不要开放 25575**（RCON），它只供内部使用。

### 5. 启动

```bash
make up
```

重启机器后，容器会自动恢复运行。

## macOS

安装 [Docker Desktop](https://www.docker.com/products/docker-desktop/)，然后按照 [入门指南](getting-started.md) 操作。需要注意：

- Docker Desktop 有自己的内存上限（**Settings → Resources**），必须大于 `CONTAINER_MEMORY`。
- 同一家庭网络中的朋友连接你 Mac 的局域网 IP。外网的朋友需要你在路由器上把 TCP 25565 转发到你的 Mac。
- Docker Desktop 必须在运行，服务器才能运行。开启 "Start Docker Desktop when you sign in"，重启后它就会自动恢复。

## 异地备份

`./backups` 和服务器在同一块磁盘上。请定期把它复制到别处，例如在服务器上用 cron 运行 `rsync`：

```bash
# crontab -e   (every day at 05:00)
0 5 * * * rsync -a --delete /home/me/minecraft/backups/ backup-host:/srv/minecraft-backups/
```

任何工具都可以（同步到云存储的 `rclone`、`restic` 等）。只复制已完成的 `mc-*.tar.gz` 文件；隐藏的 `.partial` 文件是正在进行中的备份。

## 迁移到另一台机器

1. 在旧机器上执行 `make backup`。
2. 在新机器上用相同的 `.env` 和 `plugins/plugins.txt` 设置项目。
3. 把备份复制到新的 `./backups/` 中。
4. 执行 `make up`，然后 `make restore FILE=…`。参见 [备份与恢复](backups.md#在新机器上恢复)。
5. 让玩家（或你的 DNS 域名）指向新地址。

## 更新项目本身

用新版本替换项目文件，但保留 `.env`、`plugins/plugins.txt` 和 `backups/`。然后：

```bash
make up
```

你的世界保存在 Docker 卷中，替换文件不会影响它。

## 安全检查清单

- 除非需要离线模式，否则保持 `ONLINE_MODE=true`；如果需要，请阅读 [风险说明](offline-mode.md#风险用户名不会被验证)。
- 将 `RCON_PASSWORD` 留空以自动生成，或使用较长的随机值。永远不要发布 25575 端口。
- 保持 `.env` 私密，它包含 RCON 密码。它已被排除在 git 和 Docker 构建之外。
- 保持主机系统更新，并用 SSH 密钥代替密码。
- 只给需要的人管理员权限；`make cmd C="deop Name"` 可以收回。
- 固定 `MC_VERSION`，并在应用插件更新前先检查。
