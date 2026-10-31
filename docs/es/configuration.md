<!-- language-switcher -->
[English](../configuration.md) · [हिन्दी](../hi/configuration.md) · **Español** · [简体中文](../zh-CN/configuration.md) · [Português (BR)](../pt-BR/configuration.md) · [日本語](../ja/configuration.md) · [한국어](../ko/configuration.md)

# <img src="../assets/icons/configuration.svg" width="28" height="28" alt=""> Referencia de configuración

Todos los ajustes están en `.env`, en la carpeta del proyecto. Empieza desde la plantilla comentada:

```bash
cp .env.example .env
```

## Aplicar cambios

| Has cambiado… | Ejecuta | Por qué |
|---|---|---|
| Cualquier ajuste de `.env` | `make up` | Vuelve a crear el contenedor con los nuevos valores. Tu mundo se conserva. |
| `MC_VERSION`, `PAPER_BUILD`, `JAVA_VERSION` | `make up` | Son ajustes de construcción; `make up` reconstruye la imagen. |
| `plugins/plugins.txt` | `make update-plugins` | Consulta [Plugins](plugins.md). |

`make restart` reinicia con la configuración *anterior*. `.env` solo se lee cuando se crea un contenedor.

Los valores no válidos se rechazan al arrancar con un mensaje que nombra el ajuste, así que una errata nunca se convierte en un bucle de fallos. Revisa `make logs` si el servidor no arranca.

## Obligatorios

| Variable | Por defecto | Descripción |
|---|---|---|
| `EULA` | `false` | Debe ser `TRUE` para aceptar el [EULA de Minecraft](https://aka.ms/MinecraftEULA). |
| `RCON_PASSWORD` | vacío | Contraseña interna opcional para guardados seguros, copias de seguridad y la consola. Si la dejas vacía, se genera una aleatoria en el primer arranque y se guarda en el volumen de datos como `.rcon-password`. Si pones la tuya: sin barras invertidas ni espacios al principio o al final. `openssl rand -hex 24` genera una buena. |

## Versión

| Variable | Por defecto | Descripción |
|---|---|---|
| `MC_VERSION` | `latest` | Versión de Minecraft: `latest` (la versión estable más reciente de Paper) o una versión exacta como `1.21.4`. Minecraft 1.17 o posterior. Lístalas con `make versions`. |
| `PAPER_BUILD` | `latest` | Un número de build de Paper concreto. Solo es válido con un `MC_VERSION` exacto, porque los números de build son por versión. |
| `JAVA_VERSION` | *(sin definir)* | Normalmente se deja sin definir; se elige Java 17, 21 o 25 según la versión de Minecraft. Defínelo solo para forzar un Java más nuevo, por ejemplo cuando un plugin lo necesita. |
| `ALLOW_DOWNGRADE` | `false` | Permite abrir un mundo con una versión de Minecraft más antigua que la última con la que se ejecutó. Esto puede corromper el mundo. Consulta [Versiones de Minecraft](minecraft-versions.md#bajar-de-versión). |

## Recursos

| Variable | Por defecto | Descripción |
|---|---|---|
| `MEMORY` | `2G` | Tamaño del heap de Java, como `4G` o `1536M`. Se reserva al arrancar (`-Xms` = `-Xmx`). |
| `CONTAINER_MEMORY` | `3G` | Límite estricto de memoria del contenedor. Debe ser al menos `MEMORY` + 512M; se recomienda `MEMORY` + 1G. El servidor se niega a arrancar si el heap no cabe. |
| `JVM_OPTS` | *(vacío)* | Flags extra de la JVM, añadidos después de los incluidos. |

Tamaños orientativos: `2G` para unos pocos amigos, `4G` para unos 10 jugadores, `6G`–`8G` para más de 20 jugadores o plugins pesados.

## Red

| Variable | Por defecto | Descripción |
|---|---|---|
| `SERVER_PORT` | `25565` | Puerto del equipo al que se conectan los jugadores. Dentro del contenedor el servidor siempre usa el 25565. |

## Juego

Se escriben en `server.properties` en cada arranque. Deja un valor **vacío** para gestionar esa clave directamente en `server.properties`.

| Variable | Por defecto | Clave de `server.properties` |
|---|---|---|
| `MOTD` | `A reliable Paper server` | `motd`. Cualquier idioma y emoji valen; los códigos de color `§` funcionan. |
| `MAX_PLAYERS` | `20` | `max-players` |
| `DIFFICULTY` | `normal` | `difficulty`: `peaceful`, `easy`, `normal`, `hard` |
| `GAMEMODE` | `survival` | `gamemode`: `survival`, `creative`, `adventure`, `spectator` |
| `ONLINE_MODE` | `true` | `online-mode`. `false` deja entrar a jugadores sin cuenta oficial. Consulta [Modo con y sin conexión](offline-mode.md). |
| `ENFORCE_USERNAME_RULES` | `true` | No es una propiedad. En modo sin conexión, exige nombres de usuario al estilo de Mojang. |
| `VIEW_DISTANCE` | `10` | `view-distance` |
| `SIMULATION_DISTANCE` | `10` | `simulation-distance` |
| `LEVEL_NAME` | `world` | `level-name`. Cambiarlo pasa a otro mundo (o lo crea). |
| `SEED` | *(vacío)* | `level-seed`. Solo afecta a mundos generados nuevos. |
| `PVP` | `true` | `pvp` |
| `WHITELIST` | `false` | `white-list` y `enforce-whitelist` |
| `MIGRATE_PLAYER_DATA` | `true` | No es una propiedad. Conserva los inventarios de los jugadores cuando cambia `ONLINE_MODE`. |

El servidor siempre fija estos valores, diga lo que diga `server.properties`: `server-port=25565`, `server-ip=` (escuchar en todas las interfaces), `enable-rcon=true`, `rcon.port` y `rcon.password`.

## Plugins

| Variable | Por defecto | Descripción |
|---|---|---|
| `PLUGIN_AUTO_UPDATE` | `false` | Busca actualizaciones de plugins cada día y reinicia para aplicarlas. Consulta [Plugins](plugins.md#actualizaciones-automáticas). |
| `PLUGIN_UPDATE_TIME` | `04:00` | Cuándo buscarlas, como hora `HH:MM` de 24 horas en `TZ`. |
| `TZ` | `UTC` | Zona horaria, como `Asia/Kolkata` o `Europe/London`. |

## Copias de seguridad

| Variable | Por defecto | Descripción |
|---|---|---|
| `BACKUP_INTERVAL` | `1h` | Tiempo entre copias programadas: un número con `s`, `m`, `h` o `d`, como `30m` o `6h`. |
| `BACKUP_KEEP` | `24` | Cuántas copias conservar (al menos 1). Las más antiguas se borran. |

## Fiabilidad

| Variable | Por defecto | Descripción |
|---|---|---|
| `STOP_TIMEOUT` | `2m` | Cuánto espera Docker a que se guarde el mundo al apagar antes de forzar el cierre. Súbelo para mundos muy grandes. |
| `WATCHDOG_INTERVAL` | `60` | Segundos entre comprobaciones de bloqueo. `0` desactiva el watchdog. |
| `WATCHDOG_FAILURES` | `5` | Comprobaciones fallidas seguidas antes de reiniciar un servidor bloqueado. |

## Propiedad de archivos

| Variable | Por defecto | Descripción |
|---|---|---|
| `PUID` / `PGID` | `1000` | Usuario y grupo propietarios de los archivos del servidor y de `./backups`. En Linux, pon la salida de `id -u` e `id -g`. |

## Avanzado

| Variable | Por defecto | Descripción |
|---|---|---|
| `RCON_PORT` | `25575` | Puerto RCON interno. Nunca se publica; cámbialo solo si un plugin necesita el 25575. |
