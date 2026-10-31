<!-- language-switcher -->
[English](../troubleshooting.md) · [हिन्दी](../hi/troubleshooting.md) · [Español](../es/troubleshooting.md) · **简体中文** · [Português (BR)](../pt-BR/troubleshooting.md) · [日本語](../ja/troubleshooting.md) · [한국어](../ko/troubleshooting.md)

# <img src="../assets/icons/troubleshooting.svg" width="28" height="28" alt=""> 故障排除

先运行：

```bash
make status          # is it running and healthy?
make logs            # what did it say?
```

大多数问题都会在日志中产生一条清楚的 `ERROR:` 行，指出相关设置或原因。下面的消息保留英文原文，与日志中显示的一致。

## 服务器无法启动

**"You must accept the Minecraft EULA"**
在 `.env` 中设置 `EULA=TRUE`，然后 `make up`。

**"Set RCON_PASSWORD in .env to your own random value"** 或 **"RCON_PASSWORD must not contain backslashes…"**
在 `.env` 中将 `RCON_PASSWORD` 留空以自动生成，或使用 `openssl rand -hex 24`。

**"MEMORY … does not fit in the container limit"**
把 `CONTAINER_MEMORY` 提高到消息建议的值，或降低 `MEMORY`。在 macOS 上，还要检查 Docker Desktop 的内存设置（**Settings → Resources**）。

**"This world was last run on Minecraft X, but the server is now Y"**
你降低了 `MC_VERSION`。把它改回去，或者恢复一个在旧版本上制作的备份。参见 [Minecraft 版本](minecraft-versions.md#降级)。

**"Player data migration failed; not starting…"**
通常是在切换到在线模式时无法访问 Mojang 的 API。检查服务器的网络连接；Docker 会自动重试。参见 [在线与离线模式](offline-mode.md#跨模式的玩家数据)。

**不断重启，退出码为 137**
容器达到了内存上限。提高 `CONTAINER_MEMORY`（至少 `MEMORY` + 1G），或降低 `MEMORY`。

**"port is already allocated"**
主机上有其他程序占用了 25565 端口。停止它，或设置 `SERVER_PORT=25566` 并连接该端口。

## 构建问题

**"Minecraft X needs Java N but the image is being built with Java M"**
你直接用 `docker compose` 构建了。请使用 `make up`，它会自动选择 Java。如果你自己设置了 `JAVA_VERSION`，请删除它。

**"Minecraft X is not available on Paper" / "is too old"**
从 `make versions` 中选择一个版本（1.17 或更新）。

**"Patching Paper failed (attempt N), retrying"**
构建时从 Mojang 下载失败，会自动重试。如果所有尝试都失败，请检查网络连接后再次运行 `make up`。

## 玩家无法连接

1. `make status` 应显示 `(healthy)`。
2. 服务器在同一台机器上：连接 `localhost`。
3. 云服务器：在主机防火墙中开放 TCP 25565（`sudo ufw allow 25565/tcp`），**并且**在云服务商的防火墙或安全组中也开放。
4. 家庭网络：在路由器上把 TCP 25565 转发到主机。
5. 没有正版账号的玩家：设置 `ONLINE_MODE=false`。参见 [在线与离线模式](offline-mode.md)。
6. 离线模式下出现 "Invalid username" 或 "already taken"：参见 [用户名规则](offline-mode.md#用户名规则)。

## Unhealthy

首次启动时生成世界可能需要几分钟；健康检查会等待五分钟。如果一直是 unhealthy，请在 `make logs` 中查找错误。卡死的服务器会被 [看门狗](reliability.md#卡死看门狗) 自动重启。

## 插件

**某个插件没有加载**
在 `make logs` 中查找 `[plugins] WARNING` 行，它们会说明原因：没有兼容版本、缺少依赖、需要更新的 Java 等。`make plugins` 会显示已安装的内容。

**"needs Java N (this server runs Java M)"**
该插件没有任何版本能同时支持你的 Minecraft 版本及其 Java。使用更新的 `MC_VERSION`；或者如果你的 Minecraft 版本可以在该 Java 上运行，设置 `JAVA_VERSION=N`。

**"requires the plugin X, which could not be found automatically"**
在 Modrinth 或 Hangar 上找到 X 并加入 `plugins.txt`，或者[手动](plugins.md#来自其他网站的插件)安装。

**"another plugin install/update is already running"**
定时更新正在进行。一分钟后再试。

## 备份

**"not enough disk space"**
释放主机磁盘空间，或降低 `BACKUP_KEEP`。备份拒绝占满磁盘，因为磁盘满了会导致服务器无法保存世界。

**"another backup has been running for 15 minutes"**
上一次备份卡住了，通常是因为磁盘非常慢。查看 `docker compose logs backup`。

**`./backups` permission denied (Linux)**
把 `.env` 中的 `PUID` 和 `PGID` 设置为 `id -u` / `id -g` 的结果，然后 `make up`。

## 设置似乎没有生效

`make restart` 会保留旧设置；编辑 `.env` 后请运行 `make up`。`.env` 中的游戏设置每次启动都会覆盖 `server.properties`，所以如果你想在 `server.properties` 中管理某项设置，就在 `.env` 中把它留空。
