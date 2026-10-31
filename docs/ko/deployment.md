<!-- language-switcher -->
[English](../deployment.md) · [हिन्दी](../hi/deployment.md) · [Español](../es/deployment.md) · [简体中文](../zh-CN/deployment.md) · [Português (BR)](../pt-BR/deployment.md) · [日本語](../ja/deployment.md) · **한국어**

# <img src="../assets/icons/deployment.svg" width="28" height="28" alt=""> 배포

같은 프로젝트가 macOS(테스트나 친구들과 놀기용)와 Linux 서버(항상 켜 두는 용도) 모두에서 실행됩니다. 이미지는 Intel/AMD(`amd64`)와 ARM(`arm64`, Apple Silicon 포함) 모두를 위해 네이티브로 빌드됩니다.

## Linux 서버(VPS, 가정용 서버)

### 1. Docker 설치

사용하는 배포판에 맞는 Docker 안내에 따라 Docker Engine과 Compose 플러그인을 설치한 다음:

```bash
sudo systemctl enable --now docker      # start Docker on boot
sudo usermod -aG docker "$USER"          # run docker without sudo (log out and in again)
```

### 2. 프로젝트 복사

```bash
git clone <your-repository> minecraft && cd minecraft
# or: scp -r Minecraft-Server/ user@server:~/minecraft
```

### 3. 설정

```bash
cp .env.example .env
id -u && id -g          # put these into PUID / PGID
```

`.env`에서 최소한 `EULA`, `MC_VERSION`(정확한 버전으로 고정), `MEMORY`, `CONTAINER_MEMORY`를 설정하세요.

| 서버 RAM | `MEMORY` | `CONTAINER_MEMORY` |
|---|---|---|
| 4 GB | `2G` | `3G` |
| 8 GB | `5G` | `6G` |
| 16 GB | `10G` | `12G` |

운영체제를 위해 약 1 GB를 남겨 두세요.

### 4. 게임 포트 열기

```bash
sudo ufw allow 25565/tcp
```

클라우드 제공업체에 방화벽이나 보안 그룹이 있다면 거기서도 TCP 25565를 허용하세요. **25575(RCON)는 절대 열지 마세요.** 내부 전용입니다.

### 5. 시작

```bash
make up
```

재부팅 후에도 컨테이너가 자동으로 다시 실행됩니다.

## macOS

[Docker Desktop](https://www.docker.com/products/docker-desktop/)을 설치한 다음 [시작하기](getting-started.md)를 따라 하세요. 알아 둘 점:

- Docker Desktop에는 자체 메모리 한도가 있습니다(**Settings → Resources**). `CONTAINER_MEMORY`보다 커야 합니다.
- 같은 집 네트워크의 친구는 내 Mac의 로컬 IP로 접속합니다. 외부의 친구를 위해서는 공유기에서 TCP 25565를 Mac으로 포트 포워딩하세요.
- 서버가 돌아가려면 Docker Desktop이 실행 중이어야 합니다. 재시작 후에도 다시 켜지도록 "Start Docker Desktop when you sign in"을 켜 두세요.

## 외부 백업

`./backups`는 서버와 같은 디스크에 있습니다. 정기적으로 다른 곳에 복사하세요. 예를 들어 서버의 cron에서 `rsync`를 사용합니다:

```bash
# crontab -e   (every day at 05:00)
0 5 * * * rsync -a --delete /home/me/minecraft/backups/ backup-host:/srv/minecraft-backups/
```

어떤 도구든 괜찮습니다(클라우드 스토리지로 보내는 `rclone`, `restic` 등). 완료된 `mc-*.tar.gz` 파일만 복사하세요. 숨김 파일인 `.partial`은 진행 중인 백업입니다.

## 다른 머신으로 옮기기

1. 기존 머신에서 `make backup`.
2. 새 머신에 같은 `.env`와 `plugins/plugins.txt`로 프로젝트를 설정.
3. 백업을 새 `./backups/`에 복사.
4. `make up` 후 `make restore FILE=…`. [백업과 복원](backups.md#새-머신에서-복원하기) 참고.
5. 플레이어(또는 DNS 이름)가 새 주소를 가리키게 하기.

## 프로젝트 자체 업데이트

프로젝트 파일을 새 버전으로 교체하되 `.env`, `plugins/plugins.txt`, `backups/`는 유지하세요. 그다음:

```bash
make up
```

월드는 Docker 볼륨에 있으므로 파일을 교체해도 영향을 받지 않습니다.

## 보안 체크리스트

- 오프라인 모드가 필요하지 않다면 `ONLINE_MODE=true`를 유지하고, 필요하다면 [위험성](offline-mode.md#위험성-사용자-이름이-확인되지-않음)을 읽어 보세요.
- `RCON_PASSWORD`를 비워 자동으로 만들게 하거나, 길고 무작위인 값을 쓰세요. 포트 25575는 절대 공개하지 마세요.
- `.env`는 비공개로 유지하세요. RCON 비밀번호가 들어 있습니다. git과 Docker 빌드에서는 제외되어 있습니다.
- 호스트를 최신 상태로 유지하고, 비밀번호 대신 SSH 키를 사용하세요.
- 꼭 필요한 사람만 운영자로 만드세요. `make cmd C="deop Name"`으로 해제할 수 있습니다.
- `MC_VERSION`을 고정하고, 플러그인 업데이트는 적용하기 전에 검토하세요.
