<!-- language-switcher -->
[English](../deployment.md) · [हिन्दी](../hi/deployment.md) · [Español](../es/deployment.md) · [简体中文](../zh-CN/deployment.md) · [Português (BR)](../pt-BR/deployment.md) · **日本語** · [한국어](../ko/deployment.md)

# <img src="../assets/icons/deployment.svg" width="28" height="28" alt=""> デプロイ

同じプロジェクトが macOS（テストや友だちと遊ぶ用）でも Linux サーバー（常時稼働用）でも動きます。イメージは Intel/AMD（`amd64`）と ARM（`arm64`、Apple Silicon を含む）の両方向けにネイティブでビルドされます。

## Linux サーバー（VPS、自宅サーバー）

### 1. Docker をインストールする

お使いのディストリビューション向けの Docker の手順に従って Docker Engine と Compose プラグインをインストールし、次を実行します:

```bash
sudo systemctl enable --now docker      # start Docker on boot
sudo usermod -aG docker "$USER"          # run docker without sudo (log out and in again)
```

### 2. プロジェクトをコピーする

```bash
git clone <your-repository> minecraft && cd minecraft
# or: scp -r Minecraft-Server/ user@server:~/minecraft
```

### 3. 設定する

```bash
cp .env.example .env
id -u && id -g          # put these into PUID / PGID
```

`.env` で少なくとも `EULA`、`MC_VERSION`（具体的なバージョンに固定）、`MEMORY`、`CONTAINER_MEMORY` を設定します。

| サーバーの RAM | `MEMORY` | `CONTAINER_MEMORY` |
|---|---|---|
| 4 GB | `2G` | `3G` |
| 8 GB | `5G` | `6G` |
| 16 GB | `10G` | `12G` |

OS 用に約 1 GB 残しておきます。

### 4. ゲームポートを開ける

```bash
sudo ufw allow 25565/tcp
```

クラウド事業者のファイアウォールやセキュリティグループがある場合は、そこでも TCP 25565 を許可してください。**25575（RCON）は絶対に開けないでください**。内部専用です。

### 5. 起動する

```bash
make up
```

再起動後もコンテナは自動で復帰します。

## macOS

[Docker Desktop](https://www.docker.com/products/docker-desktop/) をインストールし、[はじめに](getting-started.md) の手順に従います。知っておくこと:

- Docker Desktop には独自のメモリ上限があります（**Settings → Resources**）。`CONTAINER_MEMORY` より大きくしてください。
- 同じ家のネットワークの友だちは、Mac のローカル IP に接続します。外部の友だちのためには、ルーターで TCP 25565 を Mac に転送してください。
- サーバーを動かすには Docker Desktop が起動している必要があります。再起動後に戻るよう "Start Docker Desktop when you sign in" を有効にしてください。

## 外部バックアップ

`./backups` はサーバーと同じディスクにあります。定期的に別の場所へコピーしてください。たとえばサーバー上の cron で `rsync` を使います:

```bash
# crontab -e   (every day at 05:00)
0 5 * * * rsync -a --delete /home/me/minecraft/backups/ backup-host:/srv/minecraft-backups/
```

ツールは何でも構いません（クラウドストレージへの `rclone`、`restic` など）。コピーするのは完成した `mc-*.tar.gz` ファイルだけにしてください。隠しファイルの `.partial` は作成途中のバックアップです。

## 別のマシンへ移す

1. 古いマシンで `make backup`。
2. 新しいマシンに、同じ `.env` と `plugins/plugins.txt` でプロジェクトをセットアップ。
3. バックアップを新しい `./backups/` にコピー。
4. `make up` のあと `make restore FILE=…`。[バックアップと復元](backups.md#新しいマシンで復元する) を参照。
5. プレイヤー（または DNS 名）を新しいアドレスに向ける。

## プロジェクト自体の更新

プロジェクトのファイルを新しいバージョンに差し替えます。ただし `.env`、`plugins/plugins.txt`、`backups/` は残してください。そのあと:

```bash
make up
```

ワールドは Docker ボリュームにあるので、ファイルを差し替えても影響はありません。

## セキュリティチェックリスト

- オフラインモードが必要でなければ `ONLINE_MODE=true` のままにし、必要な場合は [リスク](offline-mode.md#リスク-ユーザー名は確認されない) を読んでください。
- `RCON_PASSWORD` を空にして自動生成させるか、長くランダムな値を使ってください。ポート 25575 は絶対に公開しないでください。
- `.env` は非公開にしてください。RCON パスワードが入っています。git と Docker のビルドからは除外されています。
- ホストを最新に保ち、パスワードではなく SSH 鍵を使ってください。
- オペレーターは必要な人だけにしてください。`make cmd C="deop Name"` で外せます。
- `MC_VERSION` を固定し、プラグインの更新は反映する前に確認してください。
