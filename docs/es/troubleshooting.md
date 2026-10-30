<!-- language-switcher -->
[English](../troubleshooting.md) · [हिन्दी](../hi/troubleshooting.md) · **Español** · [简体中文](../zh-CN/troubleshooting.md) · [Português (BR)](../pt-BR/troubleshooting.md) · [日本語](../ja/troubleshooting.md) · [한국어](../ko/troubleshooting.md)

# <img src="../assets/icons/troubleshooting.svg" width="28" height="28" alt=""> Solución de problemas

Empieza por:

```bash
make status          # is it running and healthy?
make logs            # what did it say?
```

La mayoría de los problemas producen una línea `ERROR:` clara en los logs que nombra el ajuste o la causa. Los mensajes de abajo aparecen en inglés, tal como salen en los logs.

## El servidor no arranca

**"You must accept the Minecraft EULA"**
Pon `EULA=TRUE` en `.env` y luego `make up`.

**"Set RCON_PASSWORD in .env to your own random value"** o **"RCON_PASSWORD must not contain backslashes…"**
Usa `openssl rand -hex 24` y pon el resultado en `.env`.

**"MEMORY … does not fit in the container limit"**
Sube `CONTAINER_MEMORY` al valor que sugiere el mensaje o baja `MEMORY`. En macOS, revisa también la memoria de Docker Desktop en **Settings → Resources**.

**"This world was last run on Minecraft X, but the server is now Y"**
Has bajado `MC_VERSION`. Vuelve a ponerla como estaba o restaura una copia hecha con la versión anterior. Consulta [Versiones de Minecraft](minecraft-versions.md#bajar-de-versión).

**"Player data migration failed; not starting…"**
Normalmente no se pudo contactar con la API de Mojang al pasar al modo con conexión. Revisa la conexión a internet del servidor; Docker reintenta automáticamente. Consulta [Modo con y sin conexión](offline-mode.md#datos-de-jugadores-entre-modos).

**Se reinicia una y otra vez con código de salida 137**
El contenedor llegó a su límite de memoria. Sube `CONTAINER_MEMORY` (al menos `MEMORY` + 1G) o baja `MEMORY`.

**"port is already allocated"**
Otra cosa usa el puerto 25565 en el equipo. Detenla, o pon `SERVER_PORT=25566` y conéctate a ese puerto.

## Problemas de construcción

**"Minecraft X needs Java N but the image is being built with Java M"**
Has construido con `docker compose` directamente. Usa `make up`, que elige Java automáticamente. Si definiste `JAVA_VERSION` tú mismo, quítalo.

**"Minecraft X is not available on Paper" / "is too old"**
Elige una versión de `make versions` (1.17 o más reciente).

**"Patching Paper failed (attempt N), retrying"**
Falló una descarga de Mojang durante la construcción; se reintenta automáticamente. Si fallan todos los intentos, revisa tu conexión a internet y ejecuta `make up` de nuevo.

## Los jugadores no pueden conectarse

1. `make status` debería mostrar `(healthy)`.
2. Servidor en la misma máquina: conéctate a `localhost`.
3. Servidor en la nube: abre TCP 25565 en el firewall del equipo (`sudo ufw allow 25565/tcp`) **y** en el firewall o grupo de seguridad del proveedor.
4. Red doméstica: redirige TCP 25565 del router al equipo.
5. Jugadores sin cuenta oficial: pon `ONLINE_MODE=false`. Consulta [Modo con y sin conexión](offline-mode.md).
6. "Invalid username" o "already taken" en modo sin conexión: consulta las [reglas de nombres de usuario](offline-mode.md#reglas-de-nombres-de-usuario).

## Unhealthy

En el primer arranque, generar el mundo puede tardar unos minutos; la comprobación de salud da cinco. Si sigue unhealthy, busca errores en `make logs`. El [watchdog](reliability.md#watchdog-contra-bloqueos) reinicia automáticamente un servidor bloqueado.

## Plugins

**Un plugin no se cargó**
Busca líneas `[plugins] WARNING` en `make logs`. Explican el motivo: no hay versión compatible, falta una dependencia, necesita un Java más nuevo, etc. `make plugins` muestra qué está instalado.

**"needs Java N (this server runs Java M)"**
Ninguna versión de ese plugin es compatible a la vez con tu versión de Minecraft y su Java. Usa un `MC_VERSION` más nuevo, o pon `JAVA_VERSION=N` si tu versión de Minecraft funciona con él.

**"requires the plugin X, which could not be found automatically"**
Busca X en Modrinth o Hangar y añádelo a `plugins.txt`, o instálalo [a mano](plugins.md#plugins-de-otros-sitios).

**"another plugin install/update is already running"**
Hay una actualización programada en curso. Vuelve a intentarlo en un minuto.

## Copias de seguridad

**"not enough disk space"**
Libera espacio en el equipo o baja `BACKUP_KEEP`. Las copias se niegan a llenar el disco, porque un disco lleno puede impedir que el servidor guarde el mundo.

**"another backup has been running for 15 minutes"**
Una copia anterior está atascada, normalmente por un disco muy lento. Revisa `docker compose logs backup`.

**`./backups` permission denied (Linux)**
Pon `PUID` y `PGID` en `.env` con los valores de `id -u` / `id -g` y luego `make up`.

## Los ajustes no parecen aplicarse

`make restart` mantiene la configuración anterior; ejecuta `make up` después de editar `.env`. Los ajustes de juego de `.env` sobrescriben `server.properties` en cada arranque, así que deja un ajuste vacío en `.env` si quieres gestionarlo en `server.properties`.
