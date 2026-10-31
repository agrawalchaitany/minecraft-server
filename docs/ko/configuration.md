<!-- language-switcher -->
[English](../configuration.md) · [हिन्दी](../hi/configuration.md) · [Español](../es/configuration.md) · [简体中文](../zh-CN/configuration.md) · [Português (BR)](../pt-BR/configuration.md) · [日本語](../ja/configuration.md) · **한국어**

# <img src="../assets/icons/configuration.svg" width="28" height="28" alt=""> 설정 참조

모든 설정은 프로젝트 폴더의 `.env`에 있습니다. 설명이 달린 템플릿에서 시작하세요:

```bash
cp .env.example .env
```

## 변경 사항 적용

| 바꾼 것 | 실행 | 이유 |
|---|---|---|
| `.env`의 어떤 설정이든 | `make up` | 새 값으로 컨테이너를 다시 만듭니다. 월드는 유지됩니다. |
| `MC_VERSION`, `PAPER_BUILD`, `JAVA_VERSION` | `make up` | 빌드 설정이므로 `make up`이 이미지를 다시 빌드합니다. |
| `plugins/plugins.txt` | `make update-plugins` | [플러그인](plugins.md) 참고. |

`make restart`는 *이전* 설정으로 재시작합니다. `.env`는 컨테이너를 만들 때만 읽습니다.

잘못된 값은 시작할 때 설정 이름이 담긴 메시지와 함께 거부되므로, 오타가 크래시 루프로 이어지지 않습니다. 서버가 켜지지 않으면 `make logs`를 확인하세요.

## 필수

| 변수 | 기본값 | 설명 |
|---|---|---|
| `EULA` | `false` | [Minecraft EULA](https://aka.ms/MinecraftEULA)에 동의하려면 `TRUE`여야 합니다. |
| `RCON_PASSWORD` | 비어 있음 | 안전한 저장, 백업, 콘솔에 쓰는 선택 사항인 내부 비밀번호. 비워 두면 처음 시작할 때 무작위 비밀번호가 만들어져 데이터 볼륨에 `.rcon-password`로 저장됩니다. 직접 정한다면: 백슬래시와 앞뒤 공백은 안 됩니다. `openssl rand -hex 24`로 좋은 비밀번호를 만들 수 있습니다. |

## 버전

| 변수 | 기본값 | 설명 |
|---|---|---|
| `MC_VERSION` | `latest` | Minecraft 버전: `latest`(가장 최신의 안정 Paper 릴리스) 또는 `1.21.4` 같은 정확한 버전. Minecraft 1.17 이상. `make versions`로 목록을 봅니다. |
| `PAPER_BUILD` | `latest` | 특정 Paper 빌드 번호. 빌드 번호는 버전마다 따로 매겨지므로 정확한 `MC_VERSION`과 함께일 때만 유효합니다. |
| `JAVA_VERSION` | *(설정 안 함)* | 보통은 비워 둡니다. Minecraft 버전에 맞춰 Java 17, 21, 25 중 하나가 선택됩니다. 플러그인이 필요로 할 때처럼 더 새로운 Java를 강제할 때만 설정하세요. |
| `ALLOW_DOWNGRADE` | `false` | 월드가 마지막으로 실행된 버전보다 오래된 Minecraft로 월드를 여는 것을 허용합니다. 월드가 손상될 수 있습니다. [Minecraft 버전](minecraft-versions.md#다운그레이드) 참고. |

## 리소스

| 변수 | 기본값 | 설명 |
|---|---|---|
| `MEMORY` | `2G` | Java 힙 크기. `4G`나 `1536M` 등. 시작할 때 미리 확보됩니다(`-Xms` = `-Xmx`). |
| `CONTAINER_MEMORY` | `3G` | 컨테이너의 메모리 상한. 최소 `MEMORY` + 512M이어야 하며 `MEMORY` + 1G를 권장합니다. 힙이 들어가지 않으면 서버가 시작을 거부합니다. |
| `JVM_OPTS` | *(비어 있음)* | 추가 JVM 플래그. 내장 플래그 뒤에 붙습니다. |

대략적인 기준: 친구 몇 명이면 `2G`, 10명 정도면 `4G`, 20명 이상이거나 무거운 플러그인이 있으면 `6G`–`8G`.

## 네트워크

| 변수 | 기본값 | 설명 |
|---|---|---|
| `SERVER_PORT` | `25565` | 플레이어가 접속하는 호스트의 포트. 컨테이너 안의 서버는 항상 25565를 씁니다. |

## 게임플레이

이 값들은 시작할 때마다 `server.properties`에 기록됩니다. 그 키를 `server.properties`에서 직접 관리하려면 값을 **비워** 두세요.

| 변수 | 기본값 | `server.properties` 키 |
|---|---|---|
| `MOTD` | `A reliable Paper server` | `motd`. 어떤 언어나 이모지도 괜찮고, `§` 색상 코드도 동작합니다. |
| `MAX_PLAYERS` | `20` | `max-players` |
| `DIFFICULTY` | `normal` | `difficulty`: `peaceful`, `easy`, `normal`, `hard` |
| `GAMEMODE` | `survival` | `gamemode`: `survival`, `creative`, `adventure`, `spectator` |
| `ONLINE_MODE` | `true` | `online-mode`. `false`면 정품 계정이 없는 플레이어도 접속할 수 있습니다. [온라인 모드와 오프라인 모드](offline-mode.md) 참고. |
| `ENFORCE_USERNAME_RULES` | `true` | 속성이 아닙니다. 오프라인 모드에서 Mojang 방식의 사용자 이름을 요구합니다. |
| `VIEW_DISTANCE` | `10` | `view-distance` |
| `SIMULATION_DISTANCE` | `10` | `simulation-distance` |
| `LEVEL_NAME` | `world` | `level-name`. 바꾸면 다른 월드로 전환합니다(없으면 새로 만듭니다). |
| `SEED` | *(비어 있음)* | `level-seed`. 새로 생성되는 월드에만 영향을 줍니다. |
| `PVP` | `true` | `pvp` |
| `WHITELIST` | `false` | `white-list`와 `enforce-whitelist` |
| `MIGRATE_PLAYER_DATA` | `true` | 속성이 아닙니다. `ONLINE_MODE`가 바뀔 때 플레이어 인벤토리를 유지합니다. |

`server.properties`의 내용과 관계없이 서버는 항상 다음을 설정합니다: `server-port=25565`, `server-ip=`(모든 인터페이스에서 수신), `enable-rcon=true`, `rcon.port`, `rcon.password`.

## 플러그인

| 변수 | 기본값 | 설명 |
|---|---|---|
| `PLUGIN_AUTO_UPDATE` | `false` | 매일 플러그인 업데이트를 확인하고 재시작해서 적용합니다. [플러그인](plugins.md#자동-업데이트) 참고. |
| `PLUGIN_UPDATE_TIME` | `04:00` | 확인 시각. `TZ` 기준 24시간제 `HH:MM`. |
| `TZ` | `UTC` | 시간대. `Asia/Seoul`이나 `Europe/London` 등. |

## 백업

| 변수 | 기본값 | 설명 |
|---|---|---|
| `BACKUP_INTERVAL` | `1h` | 정기 백업 사이의 간격: `30m`이나 `6h`처럼 숫자에 `s`, `m`, `h`, `d`를 붙입니다. |
| `BACKUP_KEEP` | `24` | 보관할 백업 개수(최소 1). 오래된 것은 삭제됩니다. |

## 안정성

| 변수 | 기본값 | 설명 |
|---|---|---|
| `STOP_TIMEOUT` | `2m` | 종료할 때 Docker가 강제 종료하기 전에 월드 저장을 기다리는 시간. 아주 큰 월드라면 늘리세요. |
| `WATCHDOG_INTERVAL` | `60` | 멈춤 확인 사이의 초. `0`이면 워치독을 끕니다. |
| `WATCHDOG_FAILURES` | `5` | 멈춘 서버를 재시작하기 전까지 연속으로 실패해야 하는 확인 횟수. |

## 파일 소유권

| 변수 | 기본값 | 설명 |
|---|---|---|
| `PUID` / `PGID` | `1000` | 서버 파일과 `./backups`를 소유하는 사용자와 그룹. Linux에서는 `id -u`와 `id -g`의 출력으로 설정하세요. |

## 고급

| 변수 | 기본값 | 설명 |
|---|---|---|
| `RCON_PORT` | `25575` | 내부 RCON 포트. 절대 공개되지 않습니다. 플러그인이 25575를 필요로 할 때만 바꾸세요. |
