<div align="center">

<img src="../assets/banner.svg" alt="Minecraft Server: 처음부터 직접 만든, 운영 환경용 Docker Paper 서버" width="100%">

[![Minecraft](https://img.shields.io/badge/Minecraft-1.17%20to%20latest-62B47A)](../ko/minecraft-versions.md)
[![Paper](https://img.shields.io/badge/Paper-stable%20builds-1f6feb)](https://papermc.io)
[![Java](https://img.shields.io/badge/Java-17%20%7C%2021%20%7C%2025%20(auto)-orange?logo=openjdk&logoColor=white)](../ko/minecraft-versions.md#java를-고르는-방식)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)](https://docs.docker.com/compose/)
[![Platforms](https://img.shields.io/badge/platform-linux%2Famd64%20%7C%20linux%2Farm64%20%7C%20macOS-lightgrey)](../ko/deployment.md)

[빠른 시작](#빠른-시작) ·
[기능](#기능) ·
[문서](../ko/README.md) ·
[문제 해결](../ko/troubleshooting.md)

<!-- language-switcher -->
[English](../../README.md) · [हिन्दी](README.hi.md) · [Español](README.es.md) · [简体中文](README.zh-CN.md) · [Português (BR)](README.pt-BR.md) · [日本語](README.ja.md) · **한국어**

</div>

---

1.17부터 최신 버전까지 Minecraft: Java Edition의 어떤 버전이든 [Paper](https://papermc.io) 위에서 명령어 하나로 실행하세요. 서버는 스스로 재시작하고, 월드를 올바르게 저장하고, 정해진 주기로 백업하고, 플러그인 호환성을 유지하며, 서버를 망가뜨릴 수 있는 설정은 거부합니다.

모든 것을 처음부터 직접 만들었습니다. 서드파티 서버 이미지나 스크립트는 사용하지 않습니다. 외부에서 가져오는 것은 공식 `eclipse-temurin` Java 이미지와 PaperMC, Mojang, Modrinth, Hangar의 공식 다운로드 서비스뿐입니다. 모든 서버 및 플러그인 jar는 체크섬을 검증합니다.

## 빠른 시작

### 미리 만든 이미지

빌드할 필요 없음: Java 25 위의 Minecraft 26.3(최신 안정 버전), 플러그인 없음, Intel/AMD와 ARM 지원.

```bash
docker run -d --name minecraft --restart unless-stopped \
  -p 25565:25565 -e EULA=TRUE -e MEMORY=2G \
  -v minecraft-data:/data \
  agrawalchaitany/minecraft-server:26.3
```

`EULA=TRUE`로 설정하면 [Minecraft EULA](https://aka.ms/MinecraftEULA)에 동의하는 것입니다. `localhost:25565`로 접속하고, 콘솔은 `docker exec -it minecraft rcon-cli`로 엽니다. 같은 이미지가 GitHub Packages에도 `ghcr.io/agrawalchaitany/minecraft-server`로 올라가 있습니다.

정기 백업(`./backups`에 저장)과 함께 실행하려면 미리 준비된 [compose 파일](../../quickstart/compose.yaml)을 사용하세요:

```bash
mkdir minecraft && cd minecraft
curl -fsSLO https://raw.githubusercontent.com/agrawalchaitany/minecraft-server/main/quickstart/compose.yaml
echo EULA=TRUE > .env
docker compose up -d
```

### 소스에서 빌드

Minecraft 버전을 고르거나, 플러그인을 추가하거나, 그 밖의 것을 바꾸려면 직접 빌드하세요:

```bash
cp .env.example .env
```

`.env`를 수정합니다:

```env
EULA=TRUE                         # accept https://aka.ms/MinecraftEULA
RCON_PASSWORD=                    # optional; generated automatically if empty
MC_VERSION=latest                 # or an exact version, e.g. 1.21.4 (see: make versions)
```

실행합니다:

```bash
make up
make status        # wait for (healthy), then connect to localhost:25565
```

전체 안내: **[시작하기](../ko/getting-started.md)**.

## 기능

<img src="../assets/icons/versions.svg" width="36" height="36" align="left" hspace="10" alt="">

**모든 버전**<br>
Minecraft 1.17부터 최신 버전까지. 맞는 Java(17, 21, 25)를 자동으로 선택합니다. 업그레이드 전에 월드를 백업하고, 월드를 손상시킬 수 있는 다운그레이드는 거부합니다. [자세히](../ko/minecraft-versions.md)

<img src="../assets/icons/reliability.svg" width="36" height="36" align="left" hspace="10" alt="">

**자가 복구**<br>
크래시나 메모리 부족 후 자동으로 재시작합니다. 헬스 체크와, 멈춘 서버를 재시작하는 워치독이 있습니다. [자세히](../ko/reliability.md)

<img src="../assets/icons/shutdown.svg" width="36" height="36" align="left" hspace="10" alt="">

**안전한 종료**<br>
멈출 때마다 먼저 서버 콘솔을 통해 모든 월드를 저장합니다. [자세히](../ko/reliability.md#안전한-종료)

<img src="../assets/icons/backups.svg" width="36" height="36" align="left" hspace="10" alt="">

**백업**<br>
정해진 주기로, 플레이어가 접속 중이어도 일관성 있게 백업합니다. 잠금으로 두 백업이 겹치지 않게 하고, 검증, 디스크 공간 확인, 오래된 백업 정리를 합니다. 업그레이드, 마이그레이션, 플러그인 업데이트, 복원 전에도 자동으로 백업합니다. [자세히](../ko/backups.md)

<img src="../assets/icons/restore.svg" width="36" height="36" align="left" hspace="10" alt="">

**안전한 복원**<br>
명령어 하나. 먼저 파일을 확인하고, 안전 백업을 만들고, 임시 폴더에 압축을 풀기 때문에 실패해도 현재 월드를 잃지 않습니다. [자세히](../ko/backups.md#복원하기)

<img src="../assets/icons/plugins.svg" width="36" height="36" align="left" hspace="10" alt="">

**플러그인 관리자**<br>
`plugins.txt`에 플러그인을 적기만 하면 됩니다. jar 자체를 검사해서, 내 Minecraft와 Java에서 실제로 동작하는 최신 버전을 플러그인마다 고릅니다. 필요한 의존 플러그인을 설치하고, 충돌과 중복을 찾아냅니다. 필요할 때 또는 매일 업데이트합니다. [자세히](../ko/plugins.md)

<img src="../assets/icons/players.svg" width="36" height="36" align="left" hspace="10" alt="">

**오프라인 모드**<br>
정품 계정이 없는 플레이어도 접속할 수 있게 하면서, Mojang의 사용자 이름 규칙을 적용하고 모드를 바꿔도 인벤토리를 유지합니다. [자세히](../ko/offline-mode.md)

<img src="../assets/icons/guard.svg" width="36" height="36" align="left" hspace="10" alt="">

**안전장치**<br>
시작할 때 모든 설정을 명확한 메시지와 함께 검증합니다. 오타 때문에 크래시 루프에 빠지지 않습니다. [자세히](../ko/reliability.md#설정-검증)

<img src="../assets/icons/security.svg" width="36" height="36" align="left" hspace="10" alt="">

**최소 권한**<br>
root가 아닌 사용자로 실행됩니다. 원격 콘솔은 Docker 밖으로 절대 노출되지 않습니다. 로그는 순환 관리됩니다.

## 자주 쓰는 명령어

| 명령어 | |
|---|---|
| `make up` | 시작, 또는 `.env` 변경 사항 적용 |
| `make down` | 중지 (월드는 저장됨) |
| `make status` / `make logs` | 상태와 로그 |
| `make console` / `make cmd C="op Steve"` | 서버 콘솔 |
| `make backup` / `make restore FILE=…` | 백업과 복원 |
| `make update-plugins` / `make plugins` | `plugins.txt` 적용, 설치된 플러그인 보기 |
| `make versions` / `make update` | Minecraft 버전 목록, Paper 업데이트 |

모든 명령어: [운영](../ko/operations.md).

## 문서

| | 가이드 | |
|:-:|---|---|
| <img src="../assets/icons/start.svg" width="20" height="20" alt=""> | [시작하기](../ko/getting-started.md) | 5분 만에 실행 |
| <img src="../assets/icons/architecture.svg" width="20" height="20" alt=""> | [아키텍처](../ko/architecture.md) | `make up`부터 서버 실행까지의 동작 방식 |
| <img src="../assets/icons/configuration.svg" width="20" height="20" alt=""> | [설정](../ko/configuration.md) | `.env`의 모든 설정 |
| <img src="../assets/icons/operations.svg" width="20" height="20" alt=""> | [운영](../ko/operations.md) | 일상 작업 |
| <img src="../assets/icons/versions.svg" width="20" height="20" alt=""> | [Minecraft 버전](../ko/minecraft-versions.md) | 버전, Java, 업그레이드와 다운그레이드 |
| <img src="../assets/icons/plugins.svg" width="20" height="20" alt=""> | [플러그인](../ko/plugins.md) | `plugins.txt`, 호환성 해결, 자동 업데이트, 모드 |
| <img src="../assets/icons/backups.svg" width="20" height="20" alt=""> | [백업과 복원](../ko/backups.md) | 백업의 일관성과 복원의 안전성을 지키는 방법 |
| <img src="../assets/icons/reliability.svg" width="20" height="20" alt=""> | [안정성](../ko/reliability.md) | 헬스 체크, 워치독, 종료, 메모리, 설정 검증 |
| <img src="../assets/icons/players.svg" width="20" height="20" alt=""> | [온라인 모드와 오프라인 모드](../ko/offline-mode.md) | 계정 없는 플레이어, 사용자 이름 규칙, 플레이어 데이터 |
| <img src="../assets/icons/deployment.svg" width="20" height="20" alt=""> | [배포](../ko/deployment.md) | Linux 서버, macOS, 방화벽, 외부 백업 |
| <img src="../assets/icons/edge-cases.svg" width="20" height="20" alt=""> | [예외 상황](../ko/edge-cases.md) | 특이한 상황과 그 처리 방식 |
| <img src="../assets/icons/troubleshooting.svg" width="20" height="20" alt=""> | [문제 해결](../ko/troubleshooting.md) | 증상과 해결 방법 |

## 요구 사항

- Docker Desktop(macOS/Windows) 또는 Compose 플러그인이 있는 Docker Engine 24 이상(Linux)
- `make`
- 기본 설정 기준 약 3 GB의 여유 메모리

## 프로젝트 구조

```
Dockerfile, compose.yaml, Makefile, .env.example
scripts/     startup, backup, restore, and helper scripts
tools/       dependency-free Java tools (RCON, versions, plugins, player migration)
plugins/     plugins.txt and the built-in username-rules plugin
docs/        documentation
```

## 면책 조항

Mojang Studios, Microsoft, PaperMC와 관련이 없습니다. Minecraft는 Mojang Studios의 상표입니다. 서버를 실행하려면 [Minecraft EULA](https://aka.ms/MinecraftEULA)에 동의해야 합니다.
