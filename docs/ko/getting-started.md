<!-- language-switcher -->
[English](../getting-started.md) · [हिन्दी](../hi/getting-started.md) · [Español](../es/getting-started.md) · [简体中文](../zh-CN/getting-started.md) · [Português (BR)](../pt-BR/getting-started.md) · [日本語](../ja/getting-started.md) · **한국어**

# <img src="../assets/icons/start.svg" width="28" height="28" alt=""> 시작하기

이 가이드는 아무것도 없는 상태에서 접속 가능한 Minecraft 서버를 실행하기까지 약 5분 동안 안내합니다.

## 무엇을 얻게 되나요

`make up`을 실행하면 컨테이너 두 개가 시작됩니다:

- **`mc`**: [Paper](https://papermc.io) Minecraft: Java Edition 서버. Paper는 공식 서버를 그대로 대체할 수 있는, 더 빠르고 플러그인을 지원하는 서버입니다.
- **`backup`**: 한 시간마다 월드를 백업하는 작은 보조 컨테이너.

서버는 크래시 후 스스로 재시작하고, 멈출 때 월드를 올바르게 저장하며, 서버를 망가뜨릴 수 있는 설정은 거부합니다. 일상적인 운영에 Docker 지식은 필요 없습니다. 모든 것이 `make` 명령어입니다.

## 요구 사항

| | |
|---|---|
| Docker | Docker Desktop(macOS, Windows) 또는 Compose 플러그인이 있는 Docker Engine 24 이상(Linux) |
| `make` | macOS와 대부분의 Linux 배포판에 기본 설치됨 |
| 메모리 | 기본 설정 기준 약 3 GB 여유 |
| 네트워크 | 설치 중 Paper, Java, 플러그인을 내려받기 위한 인터넷 연결 |

## 설치

**1. 설정 파일을 만듭니다.**

```bash
cp .env.example .env
```

**2. `.env`에서 두 가지 값을 바꿉니다.**

```env
EULA=TRUE
MC_VERSION=latest
```

- `EULA=TRUE`는 [Minecraft EULA](https://aka.ms/MinecraftEULA)에 동의한다는 뜻입니다. 이것이 없으면 서버가 시작되지 않습니다.
- `RCON_PASSWORD`는 선택 사항입니다. 내부적으로, 백업과 종료 중에 월드를 안전하게 저장하는 데 쓰입니다. 비워 두면 무작위 비밀번호가 만들어져 데이터 볼륨에 저장됩니다. Docker 밖으로는 절대 노출되지 않습니다.
- `MC_VERSION`은 실행할 Minecraft 버전입니다. `latest`로 두거나 `make versions`에서 `1.21.4` 같은 버전을 고르세요. **지금 정하세요:** 월드는 나중에 새 버전으로 업그레이드할 수 있지만 이전 버전으로는 절대 되돌릴 수 없습니다. [Minecraft 버전](minecraft-versions.md)을 참고하세요.

**3. 시작합니다.**

```bash
make up
```

처음 실행할 때는 Java와 Paper를 내려받고 이미지를 빌드하므로 몇 분 걸립니다. 어떤 버전이 선택되었는지 표시됩니다:

```
==> Minecraft 26.3 (Paper) on Java 25
```

**4. healthy가 될 때까지 기다립니다.**

```bash
make status
```

`mc` 컨테이너에 `(healthy)`가 표시되면 서버가 준비된 것입니다. 시작 과정을 보려면 `make logs`를 실행하고, <kbd>Ctrl</kbd>+<kbd>C</kbd>로 로그 보기를 멈추세요. 서버는 멈추지 않습니다.

## 접속하기

Minecraft에서 **멀티플레이 → 서버 추가**를 선택하고 다음을 입력합니다:

- 서버가 같은 컴퓨터에서 실행 중이면 `localhost`
- 다른 곳에서 실행 중이면 서버의 IP 주소. 클라우드 서버라면 포트 25565도 열어야 합니다. [배포](deployment.md)를 참고하세요.

## 나를 운영자로 만들기

```bash
make cmd C="op YourMinecraftName"
```

## 자주 하는 다음 단계

| 하고 싶은 것 | 참고 |
|---|---|
| 정품 Minecraft 계정이 없는 친구도 접속하게 하기 | [온라인 모드와 오프라인 모드](offline-mode.md) |
| 플러그인 추가하기 | [플러그인](plugins.md) |
| 서버에 메모리 더 주기 | [설정 참조](configuration.md#리소스)의 `MEMORY`와 `CONTAINER_MEMORY` |
| 클라우드 서버에서 실행하기 | [배포](deployment.md) |
| 모든 명령어 보기 | [운영](operations.md) |

## 나중에 설정 바꾸기

`.env`를 수정한 다음 실행합니다:

```bash
make up
```

`make up`은 새 설정을 적용하고 월드는 그대로 유지합니다. (`make restart`는 *이전* 설정으로 재시작만 합니다.)
