<!-- language-switcher -->
[English](../configuration.md) · [हिन्दी](../hi/configuration.md) · [Español](../es/configuration.md) · [简体中文](../zh-CN/configuration.md) · [Português (BR)](../pt-BR/configuration.md) · **日本語** · [한국어](../ko/configuration.md)

# <img src="../assets/icons/configuration.svg" width="28" height="28" alt=""> 設定リファレンス

設定はすべてプロジェクトフォルダーの `.env` にあります。説明付きのテンプレートから始めてください:

```bash
cp .env.example .env
```

## 変更を反映する

| 変更したもの | 実行するコマンド | 理由 |
|---|---|---|
| `.env` のどの設定でも | `make up` | 新しい値でコンテナを作り直します。ワールドはそのままです。 |
| `MC_VERSION`、`PAPER_BUILD`、`JAVA_VERSION` | `make up` | ビルド時の設定なので、`make up` がイメージを再ビルドします。 |
| `plugins/plugins.txt` | `make update-plugins` | [プラグイン](plugins.md) を参照。 |

`make restart` は*以前の*設定のまま再起動します。`.env` が読まれるのはコンテナを作るときだけです。

不正な値は、起動時に設定名を示すメッセージとともに拒否されるため、入力ミスがクラッシュループになることはありません。サーバーが起動しないときは `make logs` を確認してください。

## 必須

| 変数 | デフォルト | 説明 |
|---|---|---|
| `EULA` | `false` | [Minecraft EULA](https://aka.ms/MinecraftEULA) に同意するには `TRUE` にする必要があります。 |
| `RCON_PASSWORD` | 空 | 安全な保存、バックアップ、コンソールに使う任意の内部パスワード。空のままにすると、初回起動時にランダムなものが生成され、データボリュームに `.rcon-password` として保存されます。自分で設定する場合: バックスラッシュは不可、先頭と末尾のスペースも不可。`openssl rand -hex 24` で良いものが作れます。 |

## バージョン

| 変数 | デフォルト | 説明 |
|---|---|---|
| `MC_VERSION` | `latest` | Minecraft のバージョン: `latest`（Paper の最新安定版）または `1.21.4` のような具体的なバージョン。Minecraft 1.17 以降。`make versions` で一覧を表示します。 |
| `PAPER_BUILD` | `latest` | 特定の Paper ビルド番号。ビルド番号はバージョンごとなので、具体的な `MC_VERSION` と組み合わせたときだけ有効です。 |
| `JAVA_VERSION` | *（未設定）* | 通常は未設定のままで、Minecraft のバージョンに合わせて Java 17・21・25 が選ばれます。プラグインが必要とするときなど、新しい Java を強制したい場合だけ設定します。 |
| `ALLOW_DOWNGRADE` | `false` | ワールドが最後に動いたバージョンより古い Minecraft で開くことを許可します。ワールドが壊れる可能性があります。[Minecraft のバージョン](minecraft-versions.md#ダウングレード) を参照。 |

## リソース

| 変数 | デフォルト | 説明 |
|---|---|---|
| `MEMORY` | `2G` | Java のヒープサイズ。`4G` や `1536M` など。起動時に確保されます（`-Xms` = `-Xmx`）。 |
| `CONTAINER_MEMORY` | `3G` | コンテナのメモリ上限。最低でも `MEMORY` + 512M、推奨は `MEMORY` + 1G です。ヒープが収まらない場合、サーバーは起動を拒否します。 |
| `JVM_OPTS` | *（空）* | 追加の JVM フラグ。組み込みのフラグのあとに付きます。 |

目安: 友だち数人なら `2G`、10 人程度なら `4G`、20 人以上や重いプラグインなら `6G`〜`8G`。

## ネットワーク

| 変数 | デフォルト | 説明 |
|---|---|---|
| `SERVER_PORT` | `25565` | プレイヤーが接続するホストのポート。コンテナ内のサーバーは常に 25565 を使います。 |

## ゲームプレイ

これらは起動のたびに `server.properties` に書き込まれます。そのキーを `server.properties` で直接管理したいときは、値を**空**にしてください。

| 変数 | デフォルト | `server.properties` のキー |
|---|---|---|
| `MOTD` | `A reliable Paper server` | `motd`。どの言語や絵文字でも大丈夫で、`§` のカラーコードも使えます。 |
| `MAX_PLAYERS` | `20` | `max-players` |
| `DIFFICULTY` | `normal` | `difficulty`: `peaceful`、`easy`、`normal`、`hard` |
| `GAMEMODE` | `survival` | `gamemode`: `survival`、`creative`、`adventure`、`spectator` |
| `ONLINE_MODE` | `true` | `online-mode`。`false` にすると公式アカウントのないプレイヤーも参加できます。[オンラインモードとオフラインモード](offline-mode.md) を参照。 |
| `ENFORCE_USERNAME_RULES` | `true` | プロパティではありません。オフラインモードで Mojang 形式のユーザー名を必須にします。 |
| `VIEW_DISTANCE` | `10` | `view-distance` |
| `SIMULATION_DISTANCE` | `10` | `simulation-distance` |
| `LEVEL_NAME` | `world` | `level-name`。変えると別のワールドに切り替わります（なければ作られます）。 |
| `SEED` | *（空）* | `level-seed`。新しく生成するワールドにだけ影響します。 |
| `PVP` | `true` | `pvp` |
| `WHITELIST` | `false` | `white-list` と `enforce-whitelist` |
| `MIGRATE_PLAYER_DATA` | `true` | プロパティではありません。`ONLINE_MODE` を変えたときにプレイヤーのインベントリを保持します。 |

`server.properties` の内容にかかわらず、サーバーは常に次の値を設定します: `server-port=25565`、`server-ip=`（すべてのインターフェースで待ち受け）、`enable-rcon=true`、`rcon.port`、`rcon.password`。

## プラグイン

| 変数 | デフォルト | 説明 |
|---|---|---|
| `PLUGIN_AUTO_UPDATE` | `false` | 毎日プラグインの更新を確認し、再起動して反映します。[プラグイン](plugins.md#自動更新) を参照。 |
| `PLUGIN_UPDATE_TIME` | `04:00` | 確認する時刻。`TZ` における 24 時間表記の `HH:MM`。 |
| `TZ` | `UTC` | タイムゾーン。`Asia/Tokyo` や `Europe/London` など。 |

## バックアップ

| 変数 | デフォルト | 説明 |
|---|---|---|
| `BACKUP_INTERVAL` | `1h` | 定期バックアップの間隔。`30m` や `6h` のように、数字に `s`・`m`・`h`・`d` を付けます。 |
| `BACKUP_KEEP` | `24` | 保持するバックアップの数（最低 1）。古いものは削除されます。 |

## 信頼性

| 変数 | デフォルト | 説明 |
|---|---|---|
| `STOP_TIMEOUT` | `2m` | 停止時に、Docker が強制終了する前にワールドの保存を待つ時間。非常に大きなワールドでは増やしてください。 |
| `WATCHDOG_INTERVAL` | `60` | フリーズ確認の間隔（秒）。`0` でウォッチドッグを無効にします。 |
| `WATCHDOG_FAILURES` | `5` | フリーズしたサーバーを再起動するまでの連続失敗回数。 |

## ファイルの所有者

| 変数 | デフォルト | 説明 |
|---|---|---|
| `PUID` / `PGID` | `1000` | サーバーファイルと `./backups` を所有するユーザーとグループ。Linux では `id -u` と `id -g` の出力を設定してください。 |

## 高度な設定

| 変数 | デフォルト | 説明 |
|---|---|---|
| `RCON_PORT` | `25575` | 内部の RCON ポート。公開されることはありません。プラグインが 25575 を必要とする場合だけ変更してください。 |
