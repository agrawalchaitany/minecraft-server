<div align="center">

<img src="../assets/banner.svg" alt="Minecraft Server：从零构建、可用于生产环境的 Docker 版 Paper 服务器" width="100%">

[![Minecraft](https://img.shields.io/badge/Minecraft-1.17%20to%20latest-62B47A)](../zh-CN/minecraft-versions.md)
[![Paper](https://img.shields.io/badge/Paper-stable%20builds-1f6feb)](https://papermc.io)
[![Java](https://img.shields.io/badge/Java-17%20%7C%2021%20%7C%2025%20(auto)-orange?logo=openjdk&logoColor=white)](../zh-CN/minecraft-versions.md#如何选择-java)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)](https://docs.docker.com/compose/)
[![Platforms](https://img.shields.io/badge/platform-linux%2Famd64%20%7C%20linux%2Farm64%20%7C%20macOS-lightgrey)](../zh-CN/deployment.md)

[快速开始](#快速开始) ·
[功能](#功能) ·
[文档](../zh-CN/README.md) ·
[故障排除](../zh-CN/troubleshooting.md)

<!-- language-switcher -->
[English](../../README.md) · [हिन्दी](README.hi.md) · [Español](README.es.md) · **简体中文** · [Português (BR)](README.pt-BR.md) · [日本語](README.ja.md) · [한국어](README.ko.md)

</div>

---

只需一条命令，即可在 [Paper](https://papermc.io) 上运行 Minecraft: Java Edition 1.17 至最新的任意版本。服务器会自动重启、正确保存世界、定时备份、保持插件兼容，并拒绝会导致故障的设置。

一切均从零构建：不使用任何第三方服务器镜像或脚本。唯一的外部来源是官方的 `eclipse-temurin` Java 镜像，以及 PaperMC、Mojang、Modrinth 和 Hangar 的官方下载服务。每个服务器和插件 jar 都会校验 checksum。

## 快速开始

```bash
cp .env.example .env
```

编辑 `.env`：

```env
EULA=TRUE                         # accept https://aka.ms/MinecraftEULA
RCON_PASSWORD=<random>            # e.g. output of: openssl rand -hex 24
MC_VERSION=latest                 # or an exact version, e.g. 1.21.4 (see: make versions)
```

启动：

```bash
make up
make status        # wait for (healthy), then connect to localhost:25565
```

完整教程：**[入门指南](../zh-CN/getting-started.md)**。

## 功能

| | 方面 | 你能得到什么 |
|:-:|---|---|
| <img src="../assets/icons/versions.svg" width="20" height="20" alt=""> | **任意版本** | 支持 Minecraft 1.17 至最新版本。自动选择匹配的 Java（17、21 或 25）。升级前先备份世界；会损坏世界的降级会被拒绝。[详情](../zh-CN/minecraft-versions.md) |
| <img src="../assets/icons/reliability.svg" width="20" height="20" alt=""> | **自我修复** | 崩溃和内存不足后自动重启。提供健康检查，以及会重启卡死服务器的看门狗（watchdog）。[详情](../zh-CN/reliability.md) |
| <img src="../assets/icons/shutdown.svg" width="20" height="20" alt=""> | **安全关闭** | 每次停止前都会先通过服务器控制台保存所有世界。[详情](../zh-CN/reliability.md#安全关闭) |
| <img src="../assets/icons/backups.svg" width="20" height="20" alt=""> | **备份** | 定时执行，即使玩家在线也保持一致；加锁保证不会重叠；经过校验、检查磁盘空间并自动轮换。在升级、迁移、插件更新和恢复之前自动备份。[详情](../zh-CN/backups.md) |
| <img src="../assets/icons/restore.svg" width="20" height="20" alt=""> | **安全恢复** | 一条命令。先检查文件，再做一次安全备份，并先解压到暂存目录，因此失败也不会丢失当前世界。[详情](../zh-CN/backups.md#恢复) |
| <img src="../assets/icons/plugins.svg" width="20" height="20" alt=""> | **插件管理器** | 在 `plugins.txt` 中列出插件。通过检查 jar 本身，为每个插件选择在你的 Minecraft 和 Java 上真正能运行的最新版本。自动安装必需的依赖，并发现冲突和重复。可按需或每天更新。[详情](../zh-CN/plugins.md) |
| <img src="../assets/icons/players.svg" width="20" height="20" alt=""> | **离线模式** | 允许没有正版账号的玩家加入，同时执行 Mojang 的用户名规则，并在切换模式时保留玩家物品栏。[详情](../zh-CN/offline-mode.md) |
| <img src="../assets/icons/guard.svg" width="20" height="20" alt=""> | **防护措施** | 启动时校验每一项设置并给出清晰提示。不会因为拼写错误而陷入崩溃循环。[详情](../zh-CN/reliability.md#配置校验) |
| <img src="../assets/icons/security.svg" width="20" height="20" alt=""> | **最小权限** | 以非 root 用户运行。远程控制台从不暴露到 Docker 之外。日志自动轮换。 |

## 常用命令

| 命令 | |
|---|---|
| `make up` | 启动，或应用 `.env` 的修改 |
| `make down` | 停止（会保存世界） |
| `make status` / `make logs` | 运行状态和日志 |
| `make console` / `make cmd C="op Steve"` | 服务器控制台 |
| `make backup` / `make restore FILE=…` | 备份和恢复 |
| `make update-plugins` / `make plugins` | 应用 `plugins.txt` 并查看已安装插件 |
| `make versions` / `make update` | 列出 Minecraft 版本；更新 Paper |

全部命令：[日常运维](../zh-CN/operations.md)。

## 文档

| | 指南 | |
|:-:|---|---|
| <img src="../assets/icons/start.svg" width="20" height="20" alt=""> | [入门指南](../zh-CN/getting-started.md) | 五分钟运行起来 |
| <img src="../assets/icons/architecture.svg" width="20" height="20" alt=""> | [架构](../zh-CN/architecture.md) | 从 `make up` 到服务器运行的完整过程 |
| <img src="../assets/icons/configuration.svg" width="20" height="20" alt=""> | [配置](../zh-CN/configuration.md) | `.env` 中的每一项设置 |
| <img src="../assets/icons/operations.svg" width="20" height="20" alt=""> | [日常运维](../zh-CN/operations.md) | 日常任务 |
| <img src="../assets/icons/versions.svg" width="20" height="20" alt=""> | [Minecraft 版本](../zh-CN/minecraft-versions.md) | 版本、Java、升级和降级 |
| <img src="../assets/icons/plugins.svg" width="20" height="20" alt=""> | [插件](../zh-CN/plugins.md) | `plugins.txt`、兼容性解析、自动更新、模组 |
| <img src="../assets/icons/backups.svg" width="20" height="20" alt=""> | [备份与恢复](../zh-CN/backups.md) | 备份如何保持一致、恢复如何保持安全 |
| <img src="../assets/icons/reliability.svg" width="20" height="20" alt=""> | [可靠性](../zh-CN/reliability.md) | 健康检查、看门狗、关闭、内存、配置校验 |
| <img src="../assets/icons/players.svg" width="20" height="20" alt=""> | [在线与离线模式](../zh-CN/offline-mode.md) | 无账号玩家、用户名规则、玩家数据 |
| <img src="../assets/icons/deployment.svg" width="20" height="20" alt=""> | [部署](../zh-CN/deployment.md) | Linux 服务器、macOS、防火墙、异地备份 |
| <img src="../assets/icons/edge-cases.svg" width="20" height="20" alt=""> | [边界情况](../zh-CN/edge-cases.md) | 特殊情况及其处理方式 |
| <img src="../assets/icons/troubleshooting.svg" width="20" height="20" alt=""> | [故障排除](../zh-CN/troubleshooting.md) | 症状与解决方法 |

## 环境要求

- Docker Desktop（macOS/Windows），或带 Compose 插件的 Docker Engine 24+（Linux）
- `make`
- 默认设置下约 3 GB 可用内存

## 项目结构

```
Dockerfile, compose.yaml, Makefile, .env.example
scripts/     startup, backup, restore, and helper scripts
tools/       dependency-free Java tools (RCON, versions, plugins, player migration)
plugins/     plugins.txt and the built-in username-rules plugin
docs/        documentation
```

## 免责声明

本项目与 Mojang Studios、Microsoft 或 PaperMC 无关。Minecraft 是 Mojang Studios 的商标。运行服务器需要接受 [Minecraft EULA](https://aka.ms/MinecraftEULA)。
