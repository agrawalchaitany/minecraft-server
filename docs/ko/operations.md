<!-- language-switcher -->
[English](../operations.md) · [हिन्दी](../hi/operations.md) · [Español](../es/operations.md) · [简体中文](../zh-CN/operations.md) · [Português (BR)](../pt-BR/operations.md) · [日本語](../ja/operations.md) · **한국어**

# <img src="../assets/icons/operations.svg" width="28" height="28" alt=""> 운영

일상 작업입니다. 모든 명령어는 프로젝트 폴더에서 실행하세요.

## 명령어 참조

| 명령어 | 하는 일 |
|---|---|
| `make up` | 필요하면 빌드하고, `.env`를 적용하고, 모든 것을 시작 |
| `make down` | 모든 것을 안전하게 중지 (월드는 저장됨) |
| `make restart` | 게임 서버를 안전하게 재시작 (현재 설정 유지) |
| `make status` | 컨테이너 상태와 헬스 |
| `make logs` | 로그 계속 보기 (<kbd>Ctrl</kbd>+<kbd>C</kbd>는 보기만 멈추고 서버는 멈추지 않음) |
| `make console` | 대화형 서버 콘솔 (`exit`를 입력해 나가기) |
| `make cmd C="…"` | 서버 명령어 하나 실행 |
| `make backup` | 지금 백업 |
| `make backups` | 백업 목록 |
| `make restore FILE=…` | 백업 복원. [백업과 복원](backups.md) 참고 |
| `make update` | 현재 Minecraft 버전의 최신 Paper 빌드로 다시 빌드 |
| `make versions` | Minecraft 버전과 각 버전에 필요한 Java 목록 |
| `make plugins` | 설치된 플러그인과 각각이 설치된 이유 보기 |
| `make update-plugins` | `plugins/plugins.txt` 적용. [플러그인](plugins.md) 참고 |

## 정상인가요?

```bash
make status
```

`(healthy)`는 서버가 응답하고 있다는 뜻입니다. 처음 시작할 때는 새 월드를 만드느라 시간이 걸릴 수 있으며, 헬스 체크는 5분을 기다려 줍니다. "healthy"의 의미는 [안정성](reliability.md#헬스-체크)을 참고하세요.

## 서버 콘솔

```bash
make console            # interactive
make cmd C="list"       # one command
make cmd C="op Steve"
make cmd C="whitelist add Steve"
make cmd C="say Restarting soon"
```

`docker compose attach mc`로 원시 콘솔에 직접 연결할 수도 있습니다. 빠져나오려면 <kbd>Ctrl</kbd>+<kbd>P</kbd> 다음 <kbd>Ctrl</kbd>+<kbd>Q</kbd>를 누르세요. 그 안에서 <kbd>Ctrl</kbd>+<kbd>C</kbd>를 누르면 서버가 안전하게 멈춥니다.

## 로그

```bash
make logs                                  # all services, follow
docker compose logs mc --since 1h          # game server, last hour
docker compose logs backup                 # backup history
docker compose exec mc ls logs             # Paper's own log files
```

Docker는 컨테이너마다 최대 5 × 10 MB의 로그만 보관하므로, 로그 때문에 디스크가 가득 차는 일은 없습니다.

## 서버 파일 수정

서버 파일은 `mc-data` 볼륨에 있습니다. 파일 하나를 수정하려면:

```bash
docker compose cp mc:/data/config/paper-global.yml .
# edit it
docker compose cp paper-global.yml mc:/data/config/paper-global.yml
make restart
```

`.env`가 관리하는 설정([설정 참조](configuration.md#게임플레이) 참고)은 시작할 때마다 덮어쓰입니다. 그런 설정은 `.env`에서 바꾸세요.

## 업데이트

| 대상 | 방법 |
|---|---|
| 같은 Minecraft 버전의 새 Paper 빌드 | `make update` |
| 새 Minecraft 버전 | `MC_VERSION`을 바꾸고 `make up`. 월드를 업그레이드하기 전에 백업합니다. [Minecraft 버전](minecraft-versions.md) 참고. |
| 플러그인 | `make update-plugins`. [플러그인](plugins.md) 참고. |
| 이 프로젝트 자체 | 프로젝트 파일을 교체하고(`.env`, `plugins/plugins.txt`, `backups/`는 유지) `make up` |

## 디스크 사용량

```bash
docker system df -v | grep mc-data     # world volume size
du -sh backups                         # backups
```

백업이 차지하는 공간은 `BACKUP_KEEP`으로 정합니다. 디스크가 가득 찰 수 있으면 백업은 실행을 거부합니다. [백업과 복원](backups.md#디스크-공간-안전)을 참고하세요.

## 처음부터 다시 시작

월드와 서버 데이터를 모두 지우려면(`./backups`의 백업은 유지됨):

```bash
make backup                 # optional: keep a final copy
docker compose down -v      # -v deletes the mc-data volume
make up
```
