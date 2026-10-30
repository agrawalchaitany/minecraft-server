<!-- language-switcher -->
[English](../edge-cases.md) · [हिन्दी](../hi/edge-cases.md) · [Español](../es/edge-cases.md) · **简体中文** · [Português (BR)](../pt-BR/edge-cases.md) · [日本語](../ja/edge-cases.md) · [한국어](../ko/edge-cases.md)

# <img src="../assets/icons/edge-cases.svg" width="28" height="28" alt=""> 边界情况

服务器在特殊情况下的行为。标记为**已测试**的条目已在真实服务器上复现，并观察到了所述行为。

## 配置错误

| 情况 | 行为 | |
|---|---|---|
| `EULA` 不是 `TRUE` | 拒绝启动，并说明如何接受 | 已测试 |
| `RCON_PASSWORD` 仍是占位值 | 拒绝启动 | 已测试 |
| `RCON_PASSWORD` 包含反斜杠或首尾空格 | 拒绝启动。`server.properties` 会悄悄改变它，导致安全保存失效。 | 已测试 |
| `MEMORY=2GB`（格式错误） | 拒绝启动："must look like 2G or 1536M" | 已测试 |
| `MEMORY` 超出 `CONTAINER_MEMORY` 允许的范围 | 拒绝启动并说明该设置多少 `CONTAINER_MEMORY`，而不是陷入内存不足被杀的循环 | 已测试 |
| `WATCHDOG_INTERVAL=1m` | 拒绝启动：必须是整数秒 | 已测试 |
| 开启自动更新时设置 `PLUGIN_UPDATE_TIME=25:00` | 拒绝启动，而不是让自动更新悄悄不运行 | 已测试 |
| `BACKUP_INTERVAL=abc` | 备份容器报错停止，而不是不停地备份 | 已测试 |
| `BACKUP_KEEP=0` | 拒绝备份，而不是删除包括新备份在内的所有备份 | 已测试 |
| 在 `MC_VERSION=latest` 时设置了 `PAPER_BUILD` | 构建停止：构建编号只对确切版本有意义 | 已测试 |
| `.env` 以 Windows 换行符保存 | `make` 读取版本时会去掉它们；为保险起见，请用 LF 换行保存 `.env` | |
| 修改了 `.env` 却运行了 `make restart` | 旧设置保持不变；`make restart` 不会重新读取 `.env`。请使用 `make up`。 | |
| `MOTD` 包含重音字符、其他文字或表情符号 | 以 `\uXXXX` 转义写入，因此每个 Minecraft 版本都能正确显示 | 已测试 |
| `enable-status=false`（在服务器列表中隐藏） | 健康检查改用 RCON；服务器保持健康 | 已测试 |
| 在 `server.properties` 中设置了 `server-ip` | 启动时会被清空；在 Docker 中服务器必须监听所有网络接口 | |

## Minecraft 版本

| 情况 | 行为 | |
|---|---|---|
| `MC_VERSION` 不存在（`9.9`） | `make` 停止：不可用，请运行 `make versions` | 已测试 |
| `MC_VERSION` 过旧（`1.16.5`） | `make` 停止：支持 1.17 或更新版本 | 已测试 |
| 某个版本只有 beta 版 Paper 构建 | `make` 停止：还没有稳定构建 | |
| `latest` 跳到了新的 Minecraft 版本 | 只在重建镜像时发生。固定 `MC_VERSION` 可避免意外。 | |
| 提高 `MC_VERSION` | 先备份世界再升级；插件重新解析 | 已测试 |
| 降低 `MC_VERSION` | 拒绝启动并给出说明（降级保护） | 已测试 |
| 恢复了来自更新版本的备份 | 下一次启动时被降级保护拒绝 | |
| `JAVA_VERSION` 对该 Minecraft 版本来说过旧 | `make` 停止，并给出所需的最低 Java | |
| 用旧的 `MC_VERSION` 直接运行 `docker compose up --build` | 构建停止并提示 "Build with 'make up'"；直接使用 Compose 时默认 Java 25 | |

## 备份与恢复

| 情况 | 行为 | |
|---|---|---|
| 手动备份和定时备份同时发生 | 依次运行（共享锁），所以自动保存不会在备份中途被重新打开 | 已测试 |
| 磁盘空间不足 | 拒绝备份，并在消息中给出大小；服务器继续安全运行 | 已测试 |
| 备份被中断（容器被杀） | 只留下一个隐藏的 `.partial` 文件，下一次备份会删除它。完成的备份永远不会是半成品。 | |
| 写入后归档损坏 | 被 `gzip -t` 发现并丢弃，旧的备份保留 | |
| 同一秒内两次备份 | 第二个会加上唯一后缀，而不是覆盖 | |
| 定时备份期间服务器停止 | 备份在不暂停保存的情况下继续（没有任何写入） | |
| `make restore` 时文件名拼错 | 在动任何东西之前就停止；服务器继续运行 | 已测试 |
| `make restore` 的文件已损坏 | 在动任何东西之前就停止（`gzip -t`） | |
| 恢复时解压失败（磁盘已满） | 当前数据不受影响，因为会先解压到暂存目录 | |
| 服务器由于某种原因仍在运行时进行恢复 | 被拒绝 | |

## 插件

| 情况 | 行为 | |
|---|---|---|
| 插件网站说某个版本支持你的 Minecraft，但它需要更新的 Java | 从 jar 中检测出来；使用能运行的最新版本 | 已测试 |
| 没有任何版本能在这台服务器上运行 | 跳过并说明原因，附带 `JAVA_VERSION` 提示；无法加载的已安装副本会被移除 | |
| 某个依赖没有在网站上声明，但写在 `plugin.yml` 里 | 在 Hangar 或 Modrinth 上按名字查找，只有 jar 的插件名匹配时才接受 | |
| 某个依赖在任何地方都找不到 | 给出包含其名称的警告；服务器仍然启动 | |
| 同一个插件同时从 Modrinth 和 Hangar 列出 | 保留第一个，跳过第二个，这样 Paper 永远不会看到重复 | |
| 插件也被手动安装了 | 跳过受管理的副本 | |
| 项目名拼错，或项目已被删除 | 警告；其他插件不受影响 | 已测试 |
| 启动时 Modrinth 或 Hangar 无法访问 | 警告；保留已安装的版本；下次启动时重试 | |
| 两个插件更新同时进行 | 第二个以 "already running" 被拒绝 | 已测试 |
| 应用暂存插件时容器被停止 | 应用步骤可以安全地重复执行，会在下次启动时完成 | |
| `plugins.txt` 带有字节顺序标记（BOM）或 Windows 换行符 | 能正确处理 | |
| `plugins.txt` 不存在 | 插件保持原样（不会卸载任何东西） | |
| 删除了 `plugins.txt` 的所有行 | 卸载所有受管理的插件；手动安装的 jar 保留 | |

## 玩家与模式

| 情况 | 行为 | |
|---|---|---|
| `Steve` 已存在时，离线玩家以 `steve` 加入 | 被拒绝："already taken, log in as exactly Steve" | 已测试 |
| 离线名字包含空格、连字符、重音字符或点 | 被拒绝，并说明规则 | 已测试 |
| 有现有玩家时从在线切换到离线 | 备份后把数据迁移到离线 ID | 已测试 |
| 从离线切换到在线，玩家没有账号 | 其数据保留在离线 ID 下，供以后切回使用 | 已测试 |
| 无法访问 Mojang 时从离线切换到在线 | 服务器不启动（防止数据被拆分）；Docker 会重试 | |
| 玩家在两个 ID 下都有数据 | 保留较新的文件；较旧的移到 `player-migration-conflicts/` | 已测试 |
| 通过 Geyser/Floodgate 加入的基岩版玩家（`.Name`） | 被用户名规则拒绝；请设置 `ENFORCE_USERNAME_RULES=false` | |

## 运行时

| 情况 | 行为 | |
|---|---|---|
| 服务器进程崩溃（`kill -9`） | Docker 重启它；几秒内恢复健康 | 已测试 |
| 服务器卡死但进程仍存活 | 约 5 分钟无响应后，看门狗会重启它 | |
| 首次启动非常慢（生成世界） | 看门狗会等待第一次响应；健康检查给出 5 分钟 | |
| Java 内存不足 | 干净地退出，由 Docker 重启 | |
| 在游戏中输入 `/stop` | 保存并停止；Docker 会再次启动它（用 `make down` 才能保持停止） | |
| 非常大的世界在 2 分钟内保存不完 | 调高 `STOP_TIMEOUT` | |
| 主机的 25565 端口已被占用 | `make up` 失败并提示 "port is already allocated"；请设置 `SERVER_PORT` | |
| 主机重启 | Docker 启动后容器自动恢复 | |

## 已知限制

- 仅支持运行在 Paper 上的 Minecraft: Java Edition 1.17 及更新版本。
- 只会自动在 Modrinth 和 Hangar 上搜索插件。
- 插件按玩家 UUID 保存的数据不会在在线和离线模式之间迁移。
- `make update-plugins`、`make backup` 和 `make console` 需要服务器容器正在运行。
- `make` 需要 Docker 能够拉取 `eclipse-temurin:25-jdk`（一次）来解析版本。
