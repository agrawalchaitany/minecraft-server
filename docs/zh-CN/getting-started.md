<!-- language-switcher -->
[English](../getting-started.md) · [हिन्दी](../hi/getting-started.md) · [Español](../es/getting-started.md) · **简体中文** · [Português (BR)](../pt-BR/getting-started.md) · [日本語](../ja/getting-started.md) · [한국어](../ko/getting-started.md)

# <img src="../assets/icons/start.svg" width="28" height="28" alt=""> 入门指南

本指南带你从零开始，在大约五分钟内运行一个可以加入的 Minecraft 服务器。

## 你会得到什么

运行 `make up` 会启动两个容器：

- **`mc`**：一个 [Paper](https://papermc.io) Minecraft: Java Edition 服务器。Paper 是官方服务器的直接替代品，速度更快并且支持插件。
- **`backup`**：一个小型辅助容器，每小时备份一次你的世界。

服务器崩溃后会自动重启，停止时会正确保存世界，并拒绝会导致故障的设置。日常使用无需了解 Docker，一切都是 `make` 命令。

## 环境要求

| | |
|---|---|
| Docker | Docker Desktop（macOS、Windows），或带 Compose 插件的 Docker Engine 24+（Linux） |
| `make` | macOS 和大多数 Linux 发行版已预装 |
| 内存 | 默认设置下约 3 GB 可用内存 |
| 网络 | 安装期间需要联网，以下载 Paper、Java 和插件 |

## 安装

**1. 创建配置文件。**

```bash
cp .env.example .env
```

**2. 修改 `.env` 中的两个值。**

```env
EULA=TRUE
MC_VERSION=latest
```

- `EULA=TRUE` 表示你接受 [Minecraft EULA](https://aka.ms/MinecraftEULA)。没有它服务器不会启动。
- `RCON_PASSWORD` 是可选的。它在内部使用，用于在备份和关闭时安全地保存世界。留空时会生成一个随机密码并保存在数据卷中。它永远不会暴露到 Docker 之外。
- `MC_VERSION` 是要运行的 Minecraft 版本。保留 `latest`，或从 `make versions` 中选择一个，例如 `1.21.4`。**现在就决定：**世界以后可以升级到更新的版本，但永远无法退回到更旧的版本。参见 [Minecraft 版本](minecraft-versions.md)。

**3. 启动。**

```bash
make up
```

首次运行会下载 Java 和 Paper 并构建镜像，需要几分钟。你会看到选中的版本：

```
==> Minecraft 26.3 (Paper) on Java 25
```

**4. 等待状态变为 healthy。**

```bash
make status
```

当 `mc` 容器显示 `(healthy)` 时，服务器就准备好了。想看启动过程，运行 `make logs`，按 <kbd>Ctrl</kbd>+<kbd>C</kbd> 停止跟踪日志。这不会停止服务器。

## 连接

在 Minecraft 中选择 **多人游戏 → 添加服务器**，输入：

- `localhost`，如果服务器运行在同一台电脑上；或者
- 服务器的 IP 地址，如果它运行在其他地方。在云服务器上还需要开放 25565 端口，参见 [部署](deployment.md)。

## 让自己成为管理员

```bash
make cmd C="op YourMinecraftName"
```

## 常见的下一步

| 我想要…… | 前往 |
|---|---|
| 让没有正版 Minecraft 账号的朋友加入 | [在线与离线模式](offline-mode.md) |
| 添加插件 | [插件](plugins.md) |
| 给服务器更多内存 | [配置参考](configuration.md#资源) 中的 `MEMORY` 和 `CONTAINER_MEMORY` |
| 在云服务器上运行 | [部署](deployment.md) |
| 查看所有命令 | [日常运维](operations.md) |

## 之后修改设置

编辑 `.env`，然后运行：

```bash
make up
```

`make up` 会应用新设置并保留你的世界。（`make restart` 只会用*旧的*设置重启。）
