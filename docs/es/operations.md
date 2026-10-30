<!-- language-switcher -->
[English](../operations.md) · [हिन्दी](../hi/operations.md) · **Español** · [简体中文](../zh-CN/operations.md) · [Português (BR)](../pt-BR/operations.md) · [日本語](../ja/operations.md) · [한국어](../ko/operations.md)

# <img src="../assets/icons/operations.svg" width="28" height="28" alt=""> Operaciones

Tareas del día a día. Ejecuta todos los comandos desde la carpeta del proyecto.

## Referencia de comandos

| Comando | Qué hace |
|---|---|
| `make up` | Construye si hace falta, aplica `.env` y arranca todo |
| `make down` | Detiene todo de forma segura (los mundos se guardan) |
| `make restart` | Reinicia el servidor de juego de forma segura (mantiene la configuración actual) |
| `make status` | Estado y salud de los contenedores |
| `make logs` | Sigue los logs (<kbd>Ctrl</kbd>+<kbd>C</kbd> deja de seguirlos, no detiene el servidor) |
| `make console` | Consola interactiva del servidor (escribe `exit` para salir) |
| `make cmd C="…"` | Ejecuta un comando del servidor |
| `make backup` | Hace una copia de seguridad ahora |
| `make backups` | Lista las copias de seguridad |
| `make restore FILE=…` | Restaura una copia; consulta [Copias de seguridad y restauración](backups.md) |
| `make update` | Reconstruye con el build de Paper más reciente de tu versión de Minecraft |
| `make versions` | Lista las versiones de Minecraft y el Java que necesita cada una |
| `make plugins` | Muestra los plugins instalados y por qué está cada uno |
| `make update-plugins` | Aplica `plugins/plugins.txt`; consulta [Plugins](plugins.md) |

## ¿Está sano?

```bash
make status
```

`(healthy)` significa que el servidor responde. En el primer arranque, generar un mundo nuevo puede tardar; la comprobación de salud da cinco minutos de margen. Consulta [Fiabilidad](reliability.md#comprobación-de-salud) para saber qué significa "healthy".

## Consola del servidor

```bash
make console            # interactive
make cmd C="list"       # one command
make cmd C="op Steve"
make cmd C="whitelist add Steve"
make cmd C="say Restarting soon"
```

También puedes conectarte a la consola directa con `docker compose attach mc`. Desconéctate con <kbd>Ctrl</kbd>+<kbd>P</kbd> y luego <kbd>Ctrl</kbd>+<kbd>Q</kbd>. Pulsar <kbd>Ctrl</kbd>+<kbd>C</kbd> ahí detiene el servidor, de forma segura.

## Logs

```bash
make logs                                  # all services, follow
docker compose logs mc --since 1h          # game server, last hour
docker compose logs backup                 # backup history
docker compose exec mc ls logs             # Paper's own log files
```

Docker guarda como máximo 5 × 10 MB de logs por contenedor, así que los logs nunca llenan el disco.

## Editar archivos del servidor

Los archivos del servidor están en el volumen `mc-data`. Para editar uno:

```bash
docker compose cp mc:/data/config/paper-global.yml .
# edit it
docker compose cp paper-global.yml mc:/data/config/paper-global.yml
make restart
```

Los ajustes que controla `.env` (consulta la [referencia de configuración](configuration.md#juego)) se sobrescriben en cada arranque. Cámbialos en `.env`.

## Actualizar

| Qué | Cómo |
|---|---|
| Build de Paper más nuevo, misma versión de Minecraft | `make update` |
| Versión de Minecraft más nueva | Cambia `MC_VERSION` y luego `make up`. Se hace una copia de seguridad antes de actualizar el mundo. Consulta [Versiones de Minecraft](minecraft-versions.md). |
| Plugins | `make update-plugins`. Consulta [Plugins](plugins.md). |
| Este proyecto | Sustituye los archivos del proyecto (conserva `.env`, `plugins/plugins.txt` y `backups/`) y luego `make up` |

## Uso de disco

```bash
docker system df -v | grep mc-data     # world volume size
du -sh backups                         # backups
```

`BACKUP_KEEP` controla cuánto espacio usan las copias. Las copias se niegan a ejecutarse si pudieran llenar el disco; consulta [Copias de seguridad y restauración](backups.md#seguridad-del-espacio-en-disco).

## Empezar de nuevo

Para borrar el mundo y todos los datos del servidor (las copias en `./backups` se conservan):

```bash
make backup                 # optional: keep a final copy
docker compose down -v      # -v deletes the mc-data volume
make up
```
