<!-- language-switcher -->
[English](../operations.md) · [हिन्दी](../hi/operations.md) · [Español](../es/operations.md) · [简体中文](../zh-CN/operations.md) · [Português (BR)](../pt-BR/operations.md) · **日本語** · [한국어](../ko/operations.md)

# <img src="../assets/icons/operations.svg" width="28" height="28" alt=""> 運用

日々の作業です。コマンドはすべてプロジェクトフォルダーで実行してください。

## コマンド一覧

| コマンド | 内容 |
|---|---|
| `make up` | 必要ならビルドし、`.env` を反映して、すべてを起動 |
| `make down` | すべてを安全に停止（ワールドは保存されます） |
| `make restart` | ゲームサーバーを安全に再起動（現在の設定のまま） |
| `make status` | コンテナの状態とヘルス |
| `make logs` | ログを追跡表示（<kbd>Ctrl</kbd>+<kbd>C</kbd> は表示を止めるだけでサーバーは止まりません） |
| `make console` | 対話式のサーバーコンソール（`exit` で終了） |
| `make cmd C="…"` | サーバーコマンドを 1 つ実行 |
| `make backup` | 今すぐバックアップ |
| `make backups` | バックアップの一覧 |
| `make restore FILE=…` | バックアップを復元。[バックアップと復元](backups.md) を参照 |
| `make update` | 今の Minecraft バージョンの最新 Paper ビルドで再ビルド |
| `make versions` | Minecraft のバージョンと、それぞれに必要な Java の一覧 |
| `make plugins` | インストール済みプラグインと、それぞれが入っている理由を表示 |
| `make update-plugins` | `plugins/plugins.txt` を反映。[プラグイン](plugins.md) を参照 |

## 正常に動いているか

```bash
make status
```

`(healthy)` はサーバーが応答していることを意味します。初回起動では新しいワールドの生成に時間がかかることがあり、ヘルスチェックは 5 分間待ちます。"healthy" の意味は [信頼性](reliability.md#ヘルスチェック) を参照してください。

## サーバーコンソール

```bash
make console            # interactive
make cmd C="list"       # one command
make cmd C="op Steve"
make cmd C="whitelist add Steve"
make cmd C="say Restarting soon"
```

`docker compose attach mc` で生のコンソールに接続することもできます。切り離すには <kbd>Ctrl</kbd>+<kbd>P</kbd> のあと <kbd>Ctrl</kbd>+<kbd>Q</kbd> を押します。そこで <kbd>Ctrl</kbd>+<kbd>C</kbd> を押すと、サーバーは安全に停止します。

## ログ

```bash
make logs                                  # all services, follow
docker compose logs mc --since 1h          # game server, last hour
docker compose logs backup                 # backup history
docker compose exec mc ls logs             # Paper's own log files
```

Docker はコンテナごとに最大 5 × 10 MB のログしか保持しないため、ログでディスクがいっぱいになることはありません。

## サーバーファイルの編集

サーバーのファイルは `mc-data` ボリュームにあります。ファイルを編集するには:

```bash
docker compose cp mc:/data/config/paper-global.yml .
# edit it
docker compose cp paper-global.yml mc:/data/config/paper-global.yml
make restart
```

`.env` で管理している設定（[設定リファレンス](configuration.md#ゲームプレイ) を参照）は起動のたびに上書きされます。それらは `.env` で変更してください。

## 更新

| 対象 | 方法 |
|---|---|
| 同じ Minecraft バージョンの新しい Paper ビルド | `make update` |
| 新しい Minecraft バージョン | `MC_VERSION` を変えて `make up`。ワールドのアップグレード前にバックアップを取ります。[Minecraft のバージョン](minecraft-versions.md) を参照。 |
| プラグイン | `make update-plugins`。[プラグイン](plugins.md) を参照。 |
| このプロジェクト自体 | プロジェクトのファイルを差し替えて（`.env`、`plugins/plugins.txt`、`backups/` は残す）`make up` |

## ディスク使用量

```bash
docker system df -v | grep mc-data     # world volume size
du -sh backups                         # backups
```

バックアップが使う容量は `BACKUP_KEEP` で決まります。ディスクがいっぱいになりそうな場合、バックアップは実行を拒否します。[バックアップと復元](backups.md#ディスク容量の安全) を参照してください。

## 最初からやり直す

ワールドとサーバーのデータをすべて消すには（`./backups` のバックアップは残ります）:

```bash
make backup                 # optional: keep a final copy
docker compose down -v      # -v deletes the mc-data volume
make up
```
