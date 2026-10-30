<!-- language-switcher -->
[English](../getting-started.md) · [हिन्दी](../hi/getting-started.md) · [Español](../es/getting-started.md) · [简体中文](../zh-CN/getting-started.md) · [Português (BR)](../pt-BR/getting-started.md) · **日本語** · [한국어](../ko/getting-started.md)

# <img src="../assets/icons/start.svg" width="28" height="28" alt=""> はじめに

このガイドでは、何もない状態から、参加できる Minecraft サーバーが動くところまでを約 5 分で進めます。

## 何が手に入るか

`make up` を実行すると 2 つのコンテナが起動します:

- **`mc`**: [Paper](https://papermc.io) の Minecraft: Java Edition サーバー。Paper は公式サーバーの代わりにそのまま使える、高速でプラグイン対応のサーバーです。
- **`backup`**: 1 時間ごとにワールドをバックアップする小さな補助コンテナ。

サーバーはクラッシュ後に自動で再起動し、停止時にはワールドを正しく保存し、サーバーを壊すような設定は受け付けません。日々の運用に Docker の知識は不要で、すべて `make` コマンドで操作できます。

## 必要なもの

| | |
|---|---|
| Docker | Docker Desktop（macOS、Windows）、または Compose プラグイン付きの Docker Engine 24 以降（Linux） |
| `make` | macOS とほとんどの Linux ディストリビューションに最初から入っています |
| メモリ | デフォルト設定で約 3 GB の空き |
| ネットワーク | セットアップ中に Paper、Java、プラグインをダウンロードするためのインターネット接続 |

## セットアップ

**1. 設定ファイルを作ります。**

```bash
cp .env.example .env
```

**2. `.env` の 3 つの値を変更します。**

```env
EULA=TRUE
RCON_PASSWORD=paste-a-long-random-value-here
MC_VERSION=latest
```

- `EULA=TRUE` は [Minecraft EULA](https://aka.ms/MinecraftEULA) に同意するという意味です。これがないとサーバーは起動しません。
- `RCON_PASSWORD` は内部で使われ、バックアップ時や停止時にワールドを安全に保存するためのものです。`openssl rand -hex 24` で生成してください。Docker の外には公開されません。
- `MC_VERSION` は動かす Minecraft のバージョンです。`latest` のままにするか、`make versions` から `1.21.4` などを選びます。**今決めてください:** ワールドは後で新しいバージョンにアップグレードできますが、古いバージョンには戻せません。[Minecraft のバージョン](minecraft-versions.md) を参照してください。

**3. 起動します。**

```bash
make up
```

初回は Java と Paper をダウンロードしてイメージをビルドするため、数分かかります。選ばれたバージョンが表示されます:

```
==> Minecraft 26.3 (Paper) on Java 25
```

**4. healthy になるまで待ちます。**

```bash
make status
```

`mc` コンテナに `(healthy)` と表示されれば準備完了です。起動の様子を見るには `make logs` を実行し、<kbd>Ctrl</kbd>+<kbd>C</kbd> で表示を止めます。サーバーは止まりません。

## 接続する

Minecraft で **マルチプレイ → サーバーを追加** を選び、次を入力します:

- サーバーが同じコンピューターで動いているなら `localhost`
- 別の場所で動いているならサーバーの IP アドレス。クラウドサーバーではポート 25565 も開けてください。[デプロイ](deployment.md) を参照してください。

## 自分をオペレーターにする

```bash
make cmd C="op YourMinecraftName"
```

## よくある次のステップ

| やりたいこと | 参照先 |
|---|---|
| 公式 Minecraft アカウントのない友だちを参加させたい | [オンラインモードとオフラインモード](offline-mode.md) |
| プラグインを追加したい | [プラグイン](plugins.md) |
| サーバーのメモリを増やしたい | [設定リファレンス](configuration.md#リソース) の `MEMORY` と `CONTAINER_MEMORY` |
| クラウドサーバーで動かしたい | [デプロイ](deployment.md) |
| すべてのコマンドを見たい | [運用](operations.md) |

## あとで設定を変える

`.env` を編集してから実行します:

```bash
make up
```

`make up` は新しい設定を反映し、ワールドはそのまま残ります。（`make restart` は*以前の*設定のまま再起動するだけです。）
