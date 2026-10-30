<div align="center">

<img src="../assets/banner.svg" alt="Minecraft Server：ゼロから構築した、本番運用向けの Docker 用 Paper サーバー" width="100%">

[![Minecraft](https://img.shields.io/badge/Minecraft-1.17%20to%20latest-62B47A)](../ja/minecraft-versions.md)
[![Paper](https://img.shields.io/badge/Paper-stable%20builds-1f6feb)](https://papermc.io)
[![Java](https://img.shields.io/badge/Java-17%20%7C%2021%20%7C%2025%20(auto)-orange?logo=openjdk&logoColor=white)](../ja/minecraft-versions.md#java-の選び方)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)](https://docs.docker.com/compose/)
[![Platforms](https://img.shields.io/badge/platform-linux%2Famd64%20%7C%20linux%2Farm64%20%7C%20macOS-lightgrey)](../ja/deployment.md)

[クイックスタート](#クイックスタート) ·
[機能](#機能) ·
[ドキュメント](../ja/README.md) ·
[トラブルシューティング](../ja/troubleshooting.md)

<!-- language-switcher -->
[English](../../README.md) · [हिन्दी](README.hi.md) · [Español](README.es.md) · [简体中文](README.zh-CN.md) · [Português (BR)](README.pt-BR.md) · **日本語** · [한국어](README.ko.md)

</div>

---

1.17 から最新まで、Minecraft: Java Edition の任意のバージョンを [Paper](https://papermc.io) 上でコマンド 1 つで動かせます。サーバーは自動で再起動し、ワールドを正しく保存し、定期的にバックアップを取り、プラグインの互換性を保ち、サーバーを壊すような設定は受け付けません。

すべてゼロから構築しています。サードパーティ製のサーバーイメージやスクリプトは使いません。使うのは公式の `eclipse-temurin` Java イメージと、PaperMC・Mojang・Modrinth・Hangar の公式ダウンロードサービスだけです。サーバーとプラグインの jar はすべてチェックサムを検証します。

## クイックスタート

```bash
cp .env.example .env
```

`.env` を編集します:

```env
EULA=TRUE                         # accept https://aka.ms/MinecraftEULA
RCON_PASSWORD=<random>            # e.g. output of: openssl rand -hex 24
MC_VERSION=latest                 # or an exact version, e.g. 1.21.4 (see: make versions)
```

起動します:

```bash
make up
make status        # wait for (healthy), then connect to localhost:25565
```

詳しい手順: **[はじめに](../ja/getting-started.md)**。

## 機能

| | 分野 | 内容 |
|:-:|---|---|
| <img src="../assets/icons/versions.svg" width="20" height="20" alt=""> | **どのバージョンでも** | Minecraft 1.17 から最新まで。対応する Java（17・21・25）を自動で選びます。アップグレード前にワールドをバックアップし、ワールドを壊すダウングレードは拒否します。[詳細](../ja/minecraft-versions.md) |
| <img src="../assets/icons/reliability.svg" width="20" height="20" alt=""> | **自己回復** | クラッシュやメモリ不足のあと自動で再起動します。ヘルスチェックに加え、フリーズしたサーバーを再起動するウォッチドッグがあります。[詳細](../ja/reliability.md) |
| <img src="../assets/icons/shutdown.svg" width="20" height="20" alt=""> | **安全な停止** | 停止のたびに、まずサーバーコンソール経由ですべてのワールドを保存します。[詳細](../ja/reliability.md#安全な停止) |
| <img src="../assets/icons/backups.svg" width="20" height="20" alt=""> | **バックアップ** | 定期実行で、プレイヤーがオンラインでも一貫性を保ちます。ロックで重複実行を防ぎ、検証・ディスク容量チェック・世代管理を行います。アップグレード、移行、プラグイン更新、復元の前にも自動で取得します。[詳細](../ja/backups.md) |
| <img src="../assets/icons/restore.svg" width="20" height="20" alt=""> | **安全な復元** | コマンド 1 つ。まずファイルを確認し、安全のためのバックアップを取り、一時フォルダーに展開するので、失敗しても今のワールドは失われません。[詳細](../ja/backups.md#復元) |
| <img src="../assets/icons/plugins.svg" width="20" height="20" alt=""> | **プラグインマネージャー** | `plugins.txt` にプラグインを書くだけ。jar 自体を調べて、あなたの Minecraft と Java で本当に動く最新版をそれぞれ選びます。必須の依存プラグインも入れ、競合や重複も見つけます。更新は手動でも毎日自動でも可能です。[詳細](../ja/plugins.md) |
| <img src="../assets/icons/players.svg" width="20" height="20" alt=""> | **オフラインモード** | 公式アカウントのないプレイヤーも参加できます。Mojang のユーザー名ルールを適用し、モードを切り替えてもインベントリを保持します。[詳細](../ja/offline-mode.md) |
| <img src="../assets/icons/guard.svg" width="20" height="20" alt=""> | **ガードレール** | 起動時にすべての設定をわかりやすいメッセージ付きで検証します。入力ミスでクラッシュループにはなりません。[詳細](../ja/reliability.md#設定の検証) |
| <img src="../assets/icons/security.svg" width="20" height="20" alt=""> | **最小権限** | root 以外のユーザーで動作します。リモートコンソールは Docker の外に公開されません。ログはローテーションされます。 |

## よく使うコマンド

| コマンド | |
|---|---|
| `make up` | 起動、または `.env` の変更を反映 |
| `make down` | 停止（ワールドは保存されます） |
| `make status` / `make logs` | 状態とログ |
| `make console` / `make cmd C="op Steve"` | サーバーコンソール |
| `make backup` / `make restore FILE=…` | バックアップと復元 |
| `make update-plugins` / `make plugins` | `plugins.txt` を反映、インストール済みプラグインを表示 |
| `make versions` / `make update` | Minecraft バージョン一覧、Paper の更新 |

全コマンド: [運用](../ja/operations.md)。

## ドキュメント

| | ガイド | |
|:-:|---|---|
| <img src="../assets/icons/start.svg" width="20" height="20" alt=""> | [はじめに](../ja/getting-started.md) | 5 分で起動 |
| <img src="../assets/icons/architecture.svg" width="20" height="20" alt=""> | [アーキテクチャ](../ja/architecture.md) | `make up` からサーバー起動までの仕組み |
| <img src="../assets/icons/configuration.svg" width="20" height="20" alt=""> | [設定](../ja/configuration.md) | `.env` のすべての設定 |
| <img src="../assets/icons/operations.svg" width="20" height="20" alt=""> | [運用](../ja/operations.md) | 日々の作業 |
| <img src="../assets/icons/versions.svg" width="20" height="20" alt=""> | [Minecraft のバージョン](../ja/minecraft-versions.md) | バージョン、Java、アップグレードとダウングレード |
| <img src="../assets/icons/plugins.svg" width="20" height="20" alt=""> | [プラグイン](../ja/plugins.md) | `plugins.txt`、互換性の解決、自動更新、Mod |
| <img src="../assets/icons/backups.svg" width="20" height="20" alt=""> | [バックアップと復元](../ja/backups.md) | バックアップの一貫性と、安全な復元の仕組み |
| <img src="../assets/icons/reliability.svg" width="20" height="20" alt=""> | [信頼性](../ja/reliability.md) | ヘルスチェック、ウォッチドッグ、停止、メモリ、設定の検証 |
| <img src="../assets/icons/players.svg" width="20" height="20" alt=""> | [オンラインモードとオフラインモード](../ja/offline-mode.md) | アカウントのないプレイヤー、ユーザー名ルール、プレイヤーデータ |
| <img src="../assets/icons/deployment.svg" width="20" height="20" alt=""> | [デプロイ](../ja/deployment.md) | Linux サーバー、macOS、ファイアウォール、外部バックアップ |
| <img src="../assets/icons/edge-cases.svg" width="20" height="20" alt=""> | [エッジケース](../ja/edge-cases.md) | 珍しい状況とその扱い |
| <img src="../assets/icons/troubleshooting.svg" width="20" height="20" alt=""> | [トラブルシューティング](../ja/troubleshooting.md) | 症状と対処法 |

## 必要なもの

- Docker Desktop（macOS/Windows）、または Compose プラグイン付きの Docker Engine 24 以降（Linux）
- `make`
- デフォルト設定で約 3 GB の空きメモリ

## プロジェクト構成

```
Dockerfile, compose.yaml, Makefile, .env.example
scripts/     startup, backup, restore, and helper scripts
tools/       dependency-free Java tools (RCON, versions, plugins, player migration)
plugins/     plugins.txt and the built-in username-rules plugin
docs/        documentation
```

## 免責事項

Mojang Studios、Microsoft、PaperMC とは関係ありません。Minecraft は Mojang Studios の商標です。サーバーを動かすには [Minecraft EULA](https://aka.ms/MinecraftEULA) への同意が必要です。
