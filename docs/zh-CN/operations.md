<!-- language-switcher -->
[English](../operations.md) · [हिन्दी](../hi/operations.md) · [Español](../es/operations.md) · **简体中文** · [Português (BR)](../pt-BR/operations.md) · [日本語](../ja/operations.md) · [한국어](../ko/operations.md)

# <img src="../assets/icons/operations.svg" width="28" height="28" alt=""> 日常运维

日常任务。所有命令都在项目目录中运行。

## 命令参考

| 命令 | 作用 |
|---|---|
| `make up` | 按需构建，应用 `.env`，启动所有服务 |
| `make down` | 安全地停止所有服务（世界会被保存） |
| `make restart` | 安全地重启游戏服务器（保留当前设置） |
| `make status` | 容器状态和健康状况 |
| `make logs` | 跟踪日志（<kbd>Ctrl</kbd>+<kbd>C</kbd> 只停止跟踪，不会停止服务器） |
| `make console` | 交互式服务器控制台（输入 `exit` 退出） |
| `make cmd C="…"` | 执行一条服务器命令 |
| `make backup` | 立即备份 |
| `make backups` | 列出备份 |
| `make restore FILE=…` | 恢复备份；参见 [备份与恢复](backups.md) |
| `make update` | 用你的 Minecraft 版本的最新 Paper 构建重新构建 |
| `make versions` | 列出 Minecraft 版本及各自需要的 Java |
| `make plugins` | 显示已安装插件以及每个插件存在的原因 |
| `make update-plugins` | 应用 `plugins/plugins.txt`；参见 [插件](plugins.md) |

## 运行是否健康？

```bash
make status
```

`(healthy)` 表示服务器正在响应。首次启动时生成新世界可能需要一些时间；健康检查会等待五分钟。"healthy" 的具体含义参见 [可靠性](reliability.md#健康检查)。

## 服务器控制台

```bash
make console            # interactive
make cmd C="list"       # one command
make cmd C="op Steve"
make cmd C="whitelist add Steve"
make cmd C="say Restarting soon"
```

也可以用 `docker compose attach mc` 直接连接原始控制台。按 <kbd>Ctrl</kbd>+<kbd>P</kbd> 再按 <kbd>Ctrl</kbd>+<kbd>Q</kbd> 断开。在那里按 <kbd>Ctrl</kbd>+<kbd>C</kbd> 会安全地停止服务器。

## 日志

```bash
make logs                                  # all services, follow
docker compose logs mc --since 1h          # game server, last hour
docker compose logs backup                 # backup history
docker compose exec mc ls logs             # Paper's own log files
```

Docker 为每个容器最多保留 5 × 10 MB 日志，因此日志永远不会占满磁盘。

## 编辑服务器文件

服务器文件存放在 `mc-data` 卷中。编辑某个文件：

```bash
docker compose cp mc:/data/config/paper-global.yml .
# edit it
docker compose cp paper-global.yml mc:/data/config/paper-global.yml
make restart
```

由 `.env` 控制的设置（参见 [配置参考](configuration.md#游戏设置)）每次启动都会被覆盖，请改在 `.env` 中修改。

## 更新

| 更新什么 | 怎么做 |
|---|---|
| 同一 Minecraft 版本的更新 Paper 构建 | `make update` |
| 更新的 Minecraft 版本 | 修改 `MC_VERSION`，然后 `make up`。升级世界前会先备份。参见 [Minecraft 版本](minecraft-versions.md)。 |
| 插件 | `make update-plugins`。参见 [插件](plugins.md)。 |
| 本项目 | 替换项目文件（保留 `.env`、`plugins/plugins.txt` 和 `backups/`），然后 `make up` |

## 磁盘占用

```bash
docker system df -v | grep mc-data     # world volume size
du -sh backups                         # backups
```

`BACKUP_KEEP` 决定备份占用多少空间。如果备份可能占满磁盘，它会拒绝执行；参见 [备份与恢复](backups.md#磁盘空间安全)。

## 从头开始

删除世界和所有服务器数据（`./backups` 中的备份会保留）：

```bash
make backup                 # optional: keep a final copy
docker compose down -v      # -v deletes the mc-data volume
make up
```
