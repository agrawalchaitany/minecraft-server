<!-- language-switcher -->
[English](../configuration.md) · [हिन्दी](../hi/configuration.md) · [Español](../es/configuration.md) · **简体中文** · [Português (BR)](../pt-BR/configuration.md) · [日本語](../ja/configuration.md) · [한국어](../ko/configuration.md)

# <img src="../assets/icons/configuration.svg" width="28" height="28" alt=""> 配置参考

所有设置都在项目目录的 `.env` 文件中。从带注释的模板开始：

```bash
cp .env.example .env
```

## 应用修改

| 你修改了…… | 运行 | 原因 |
|---|---|---|
| `.env` 中的任意设置 | `make up` | 用新值重新创建容器。你的世界会保留。 |
| `MC_VERSION`、`PAPER_BUILD`、`JAVA_VERSION` | `make up` | 这些是构建设置；`make up` 会重新构建镜像。 |
| `plugins/plugins.txt` | `make update-plugins` | 参见 [插件](plugins.md)。 |

`make restart` 会用*旧的*设置重启。`.env` 只在创建容器时读取。

无效的值会在启动时被拒绝，并给出包含设置名称的提示，所以拼写错误永远不会变成崩溃循环。如果服务器没有启动，请查看 `make logs`。

## 必填

| 变量 | 默认值 | 说明 |
|---|---|---|
| `EULA` | `false` | 必须为 `TRUE`，表示接受 [Minecraft EULA](https://aka.ms/MinecraftEULA)。 |
| `RCON_PASSWORD` | 占位值 | 用于安全保存、备份和控制台的内部密码。必须修改占位值。不能包含反斜杠，开头和结尾不能有空格。用 `openssl rand -hex 24` 生成就很好。 |

## 版本

| 变量 | 默认值 | 说明 |
|---|---|---|
| `MC_VERSION` | `latest` | Minecraft 版本：`latest`（最新的稳定 Paper 版本）或确切版本，如 `1.21.4`。需要 Minecraft 1.17 或更新。用 `make versions` 列出。 |
| `PAPER_BUILD` | `latest` | 指定的 Paper 构建编号。只能与确切的 `MC_VERSION` 一起使用，因为构建编号按版本分别计算。 |
| `JAVA_VERSION` | *（不设置）* | 通常不设置；会根据 Minecraft 版本选择 Java 17、21 或 25。只有在需要强制使用更新的 Java 时才设置，例如某个插件需要它。 |
| `ALLOW_DOWNGRADE` | `false` | 允许用比世界上次运行的版本更旧的 Minecraft 打开世界。这可能损坏世界。参见 [Minecraft 版本](minecraft-versions.md#降级)。 |

## 资源

| 变量 | 默认值 | 说明 |
|---|---|---|
| `MEMORY` | `2G` | Java 堆大小，如 `4G` 或 `1536M`。启动时就会预留（`-Xms` = `-Xmx`）。 |
| `CONTAINER_MEMORY` | `3G` | 容器的硬性内存上限。至少为 `MEMORY` + 512M；建议 `MEMORY` + 1G。如果堆放不下，服务器会拒绝启动。 |
| `JVM_OPTS` | *（空）* | 额外的 JVM 参数，加在内置参数之后。 |

大致参考：几个朋友用 `2G`，约 10 名玩家用 `4G`，20 名以上玩家或重型插件用 `6G`–`8G`。

## 网络

| 变量 | 默认值 | 说明 |
|---|---|---|
| `SERVER_PORT` | `25565` | 玩家连接的主机端口。容器内服务器始终使用 25565。 |

## 游戏设置

这些设置每次启动都会写入 `server.properties`。如果想直接在 `server.properties` 中管理某个键，就把对应的值**留空**。

| 变量 | 默认值 | `server.properties` 键 |
|---|---|---|
| `MOTD` | `A reliable Paper server` | `motd`。任何语言和表情符号都可以；`§` 颜色代码有效。 |
| `MAX_PLAYERS` | `20` | `max-players` |
| `DIFFICULTY` | `normal` | `difficulty`：`peaceful`、`easy`、`normal`、`hard` |
| `GAMEMODE` | `survival` | `gamemode`：`survival`、`creative`、`adventure`、`spectator` |
| `ONLINE_MODE` | `true` | `online-mode`。`false` 允许没有正版账号的玩家加入。参见 [在线与离线模式](offline-mode.md)。 |
| `ENFORCE_USERNAME_RULES` | `true` | 不是属性。离线模式下要求 Mojang 风格的用户名。 |
| `VIEW_DISTANCE` | `10` | `view-distance` |
| `SIMULATION_DISTANCE` | `10` | `simulation-distance` |
| `LEVEL_NAME` | `world` | `level-name`。修改它会切换到（或创建）另一个世界。 |
| `SEED` | *（空）* | `level-seed`。只影响新生成的世界。 |
| `PVP` | `true` | `pvp` |
| `WHITELIST` | `false` | `white-list` 和 `enforce-whitelist` |
| `MIGRATE_PLAYER_DATA` | `true` | 不是属性。`ONLINE_MODE` 改变时保留玩家物品栏。 |

无论 `server.properties` 中写的是什么，服务器始终会设置这些：`server-port=25565`、`server-ip=`（监听所有网络接口）、`enable-rcon=true`、`rcon.port` 和 `rcon.password`。

## 插件

| 变量 | 默认值 | 说明 |
|---|---|---|
| `PLUGIN_AUTO_UPDATE` | `false` | 每天检查插件更新，并重启以应用。参见 [插件](plugins.md#自动更新)。 |
| `PLUGIN_UPDATE_TIME` | `04:00` | 检查时间，`TZ` 时区下的 24 小时制 `HH:MM`。 |
| `TZ` | `UTC` | 时区，例如 `Asia/Shanghai` 或 `Europe/London`。 |

## 备份

| 变量 | 默认值 | 说明 |
|---|---|---|
| `BACKUP_INTERVAL` | `1h` | 定时备份的间隔：数字加 `s`、`m`、`h` 或 `d`，如 `30m` 或 `6h`。 |
| `BACKUP_KEEP` | `24` | 保留多少个备份（至少 1 个）。更旧的会被删除。 |

## 可靠性

| 变量 | 默认值 | 说明 |
|---|---|---|
| `STOP_TIMEOUT` | `2m` | 关闭时 Docker 在强制终止前等待保存世界的时间。世界非常大时请调高。 |
| `WATCHDOG_INTERVAL` | `60` | 卡死检查的间隔秒数。`0` 表示关闭看门狗。 |
| `WATCHDOG_FAILURES` | `5` | 连续失败多少次后重启卡死的服务器。 |

## 文件所有权

| 变量 | 默认值 | 说明 |
|---|---|---|
| `PUID` / `PGID` | `1000` | 拥有服务器文件和 `./backups` 的用户和组。在 Linux 上请设置为 `id -u` 和 `id -g` 的输出。 |

## 高级

| 变量 | 默认值 | 说明 |
|---|---|---|
| `RCON_PORT` | `25575` | 内部 RCON 端口。从不对外发布；只有当某个插件需要 25575 时才修改。 |
