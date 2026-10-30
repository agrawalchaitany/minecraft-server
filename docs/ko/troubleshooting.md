<!-- language-switcher -->
[English](../troubleshooting.md) · [हिन्दी](../hi/troubleshooting.md) · [Español](../es/troubleshooting.md) · [简体中文](../zh-CN/troubleshooting.md) · [Português (BR)](../pt-BR/troubleshooting.md) · [日本語](../ja/troubleshooting.md) · **한국어**

# <img src="../assets/icons/troubleshooting.svg" width="28" height="28" alt=""> 문제 해결

먼저 실행하세요:

```bash
make status          # is it running and healthy?
make logs            # what did it say?
```

대부분의 문제는 로그에 설정 이름이나 원인을 알려 주는 명확한 `ERROR:` 줄을 남깁니다. 아래 메시지는 로그에 나오는 그대로 영어로 적었습니다.

## 서버가 시작되지 않음

**"You must accept the Minecraft EULA"**
`.env`에서 `EULA=TRUE`로 설정한 다음 `make up`.

**"Set RCON_PASSWORD in .env to your own random value"** 또는 **"RCON_PASSWORD must not contain backslashes…"**
`openssl rand -hex 24`의 결과를 `.env`에 넣으세요.

**"MEMORY … does not fit in the container limit"**
`CONTAINER_MEMORY`를 메시지가 제안하는 값으로 올리거나 `MEMORY`를 낮추세요. macOS에서는 Docker Desktop의 메모리(**Settings → Resources**)도 확인하세요.

**"This world was last run on Minecraft X, but the server is now Y"**
`MC_VERSION`을 낮췄습니다. 원래대로 되돌리거나, 이전 버전에서 만든 백업을 복원하세요. [Minecraft 버전](minecraft-versions.md#다운그레이드)을 참고하세요.

**"Player data migration failed; not starting…"**
대개 온라인 모드로 바꾸는 동안 Mojang API에 연결하지 못한 경우입니다. 서버의 인터넷 연결을 확인하세요. Docker가 자동으로 다시 시도합니다. [온라인 모드와 오프라인 모드](offline-mode.md#모드를-넘나드는-플레이어-데이터)를 참고하세요.

**종료 코드 137로 계속 재시작됨**
컨테이너가 메모리 한도에 도달했습니다. `CONTAINER_MEMORY`를 올리거나(최소 `MEMORY` + 1G) `MEMORY`를 낮추세요.

**"port is already allocated"**
호스트에서 다른 프로그램이 포트 25565를 쓰고 있습니다. 그것을 멈추거나, `SERVER_PORT=25566`으로 설정하고 그 포트로 접속하세요.

## 빌드 문제

**"Minecraft X needs Java N but the image is being built with Java M"**
`docker compose`로 직접 빌드했습니다. Java를 자동으로 고르는 `make up`을 사용하세요. `JAVA_VERSION`을 직접 설정했다면 지우세요.

**"Minecraft X is not available on Paper" / "is too old"**
`make versions`에서 버전을 고르세요(1.17 이상).

**"Patching Paper failed (attempt N), retrying"**
빌드 중 Mojang에서 내려받기가 실패했으며, 자동으로 다시 시도합니다. 모든 시도가 실패하면 인터넷 연결을 확인하고 `make up`을 다시 실행하세요.

## 플레이어가 접속하지 못함

1. `make status`에 `(healthy)`가 보여야 합니다.
2. 서버가 같은 머신에 있음: `localhost`로 접속.
3. 클라우드 서버: 호스트 방화벽에서 TCP 25565를 열고(`sudo ufw allow 25565/tcp`), **또한** 제공업체의 방화벽이나 보안 그룹에서도 여세요.
4. 집 네트워크: 공유기에서 TCP 25565를 호스트로 포트 포워딩.
5. 정품 계정이 없는 플레이어: `ONLINE_MODE=false`로 설정. [온라인 모드와 오프라인 모드](offline-mode.md) 참고.
6. 오프라인 모드에서 "Invalid username" 또는 "already taken": [사용자 이름 규칙](offline-mode.md#사용자-이름-규칙) 참고.

## Unhealthy

처음 시작할 때 월드 생성에 몇 분이 걸릴 수 있으며, 헬스 체크는 5분을 기다립니다. 계속 unhealthy라면 `make logs`에서 오류를 찾아보세요. 멈춘 서버는 [워치독](reliability.md#멈춤-감지-워치독)이 자동으로 재시작합니다.

## 플러그인

**플러그인이 로드되지 않음**
`make logs`에서 `[plugins] WARNING` 줄을 확인하세요. 호환 버전 없음, 의존성 누락, 더 새로운 Java 필요 등 이유가 적혀 있습니다. `make plugins`로 설치된 것을 볼 수 있습니다.

**"needs Java N (this server runs Java M)"**
그 플러그인에는 내 Minecraft 버전과 그 Java를 동시에 지원하는 버전이 없습니다. 더 새로운 `MC_VERSION`을 쓰거나, 내 Minecraft 버전이 그 Java에서 동작한다면 `JAVA_VERSION=N`을 설정하세요.

**"requires the plugin X, which could not be found automatically"**
Modrinth나 Hangar에서 X를 찾아 `plugins.txt`에 추가하거나, [직접](plugins.md#다른-사이트의-플러그인) 설치하세요.

**"another plugin install/update is already running"**
예약된 업데이트가 진행 중입니다. 1분 뒤에 다시 시도하세요.

## 백업

**"not enough disk space"**
호스트의 공간을 확보하거나 `BACKUP_KEEP`을 낮추세요. 디스크가 가득 차면 서버가 월드를 저장하지 못할 수 있으므로, 백업은 디스크를 채우는 것을 거부합니다.

**"another backup has been running for 15 minutes"**
이전 백업이 멈춰 있습니다. 대개 디스크가 매우 느린 경우입니다. `docker compose logs backup`을 확인하세요.

**`./backups` permission denied (Linux)**
`.env`의 `PUID`와 `PGID`를 `id -u` / `id -g` 값으로 설정한 다음 `make up`.

## 설정이 적용되지 않는 것 같음

`make restart`는 이전 설정을 유지합니다. `.env`를 수정한 뒤에는 `make up`을 실행하세요. `.env`의 게임 설정은 시작할 때마다 `server.properties`를 덮어쓰므로, `server.properties`에서 관리하고 싶은 설정은 `.env`에서 비워 두세요.
