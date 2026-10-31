<!-- language-switcher -->
[English](../troubleshooting.md) · [हिन्दी](../hi/troubleshooting.md) · [Español](../es/troubleshooting.md) · [简体中文](../zh-CN/troubleshooting.md) · [Português (BR)](../pt-BR/troubleshooting.md) · **日本語** · [한국어](../ko/troubleshooting.md)

# <img src="../assets/icons/troubleshooting.svg" width="28" height="28" alt=""> トラブルシューティング

まずはこちら:

```bash
make status          # is it running and healthy?
make logs            # what did it say?
```

ほとんどの問題では、ログに設定名や原因を示すわかりやすい `ERROR:` 行が出ます。以下のメッセージはログに表示されるとおり英語のまま載せています。

## サーバーが起動しない

**"You must accept the Minecraft EULA"**
`.env` で `EULA=TRUE` にしてから `make up`。

**"Set RCON_PASSWORD in .env to your own random value"** または **"RCON_PASSWORD must not contain backslashes…"**
`.env` の `RCON_PASSWORD` を空にして自動生成させるか、`openssl rand -hex 24` の結果を入れてください。

**"MEMORY … does not fit in the container limit"**
`CONTAINER_MEMORY` をメッセージが提案する値まで上げるか、`MEMORY` を下げてください。macOS では Docker Desktop のメモリ（**Settings → Resources**）も確認してください。

**"This world was last run on Minecraft X, but the server is now Y"**
`MC_VERSION` を下げています。元に戻すか、古いバージョンで作ったバックアップを復元してください。[Minecraft のバージョン](minecraft-versions.md#ダウングレード) を参照してください。

**"Player data migration failed; not starting…"**
多くの場合、オンラインモードへの切り替え時に Mojang の API に接続できませんでした。サーバーのインターネット接続を確認してください。Docker が自動で再試行します。[オンラインモードとオフラインモード](offline-mode.md#モードをまたいだプレイヤーデータ) を参照してください。

**終了コード 137 で再起動を繰り返す**
コンテナがメモリ上限に達しています。`CONTAINER_MEMORY` を上げる（最低でも `MEMORY` + 1G）か、`MEMORY` を下げてください。

**"port is already allocated"**
ホストでほかのプログラムがポート 25565 を使っています。それを止めるか、`SERVER_PORT=25566` にしてそのポートに接続してください。

## ビルドの問題

**"Minecraft X needs Java N but the image is being built with Java M"**
`docker compose` で直接ビルドしています。Java を自動で選ぶ `make up` を使ってください。`JAVA_VERSION` を自分で設定している場合は削除してください。

**"Minecraft X is not available on Paper" / "is too old"**
`make versions` からバージョンを選んでください（1.17 以降）。

**"Patching Paper failed (attempt N), retrying"**
ビルド中に Mojang からのダウンロードが失敗しました。自動で再試行します。すべて失敗した場合はインターネット接続を確認して、もう一度 `make up` を実行してください。

## プレイヤーが接続できない

1. `make status` が `(healthy)` を示しているはずです。
2. サーバーが同じマシンにある: `localhost` に接続。
3. クラウドサーバー: ホストのファイアウォールで TCP 25565 を開け（`sudo ufw allow 25565/tcp`）、**さらに**事業者のファイアウォールやセキュリティグループでも開けてください。
4. 自宅のネットワーク: ルーターで TCP 25565 をホストに転送。
5. 公式アカウントのないプレイヤー: `ONLINE_MODE=false` に設定。[オンラインモードとオフラインモード](offline-mode.md) を参照。
6. オフラインモードで "Invalid username" や "already taken" が出る: [ユーザー名ルール](offline-mode.md#ユーザー名ルール) を参照。

## Unhealthy

初回起動ではワールド生成に数分かかることがあり、ヘルスチェックは 5 分待ちます。unhealthy のままなら `make logs` でエラーを探してください。フリーズしたサーバーは [ウォッチドッグ](reliability.md#フリーズ検知ウォッチドッグ) が自動で再起動します。

## プラグイン

**プラグインが読み込まれなかった**
`make logs` の `[plugins] WARNING` 行を確認してください。互換バージョンがない、依存プラグインがない、新しい Java が必要、などの理由が書かれています。`make plugins` でインストール済みのものを確認できます。

**"needs Java N (this server runs Java M)"**
そのプラグインには、あなたの Minecraft バージョンとその Java の両方に対応するバージョンがありません。新しい `MC_VERSION` を使うか、Minecraft バージョンがその Java で動くなら `JAVA_VERSION=N` を設定してください。

**"requires the plugin X, which could not be found automatically"**
Modrinth か Hangar で X を探して `plugins.txt` に追加するか、[手動で](plugins.md#ほかのサイトのプラグイン) インストールしてください。

**"another plugin install/update is already running"**
定期更新が実行中です。1 分後にもう一度試してください。

## バックアップ

**"not enough disk space"**
ホストの空き容量を増やすか、`BACKUP_KEEP` を下げてください。ディスクがいっぱいになるとサーバーがワールドを保存できなくなるため、バックアップはディスクを埋めることを拒否します。

**"another backup has been running for 15 minutes"**
前のバックアップが止まっています。たいていはディスクが非常に遅いことが原因です。`docker compose logs backup` を確認してください。

**`./backups` permission denied (Linux)**
`.env` の `PUID` と `PGID` を `id -u` / `id -g` の値にして `make up`。

## 設定が反映されない

`make restart` は以前の設定を保持します。`.env` を編集したら `make up` を実行してください。`.env` のゲーム設定は起動のたびに `server.properties` を上書きするので、`server.properties` で管理したい設定は `.env` で空にしておいてください。
