<!-- language-switcher -->
[English](../edge-cases.md) · [हिन्दी](../hi/edge-cases.md) · **Español** · [简体中文](../zh-CN/edge-cases.md) · [Português (BR)](../pt-BR/edge-cases.md) · [日本語](../ja/edge-cases.md) · [한국어](../ko/edge-cases.md)

# <img src="../assets/icons/edge-cases.svg" width="28" height="28" alt=""> Casos límite

Cómo se comporta el servidor en situaciones inusuales. Las entradas marcadas como **probado** se reprodujeron en un servidor real y se observó el comportamiento descrito.

## Errores de configuración

| Situación | Comportamiento | |
|---|---|---|
| `EULA` no es `TRUE` | Se niega a arrancar y explica cómo aceptarlo | probado |
| `RCON_PASSWORD` con el valor de ejemplo | Se niega a arrancar | probado |
| `RCON_PASSWORD` con barra invertida o espacios en los extremos | Se niega a arrancar. `server.properties` la cambiaría en silencio y rompería los guardados seguros. | probado |
| `MEMORY=2GB` (formato incorrecto) | Se niega a arrancar: "must look like 2G or 1536M" | probado |
| `MEMORY` mayor de lo que permite `CONTAINER_MEMORY` | Se niega a arrancar e indica qué `CONTAINER_MEMORY` poner, en lugar de un bucle de cierres por falta de memoria | probado |
| `WATCHDOG_INTERVAL=1m` | Se niega a arrancar: debe ser un número entero de segundos | probado |
| `PLUGIN_UPDATE_TIME=25:00` con la actualización automática activada | Se niega a arrancar, en lugar de que la actualización automática no se ejecute en silencio | probado |
| `BACKUP_INTERVAL=abc` | El contenedor de copias se detiene con un error, en lugar de hacer copias sin parar | probado |
| `BACKUP_KEEP=0` | Se rechaza la copia, en lugar de borrar todas las copias, incluida la nueva | probado |
| `PAPER_BUILD` definido con `MC_VERSION=latest` | La construcción se detiene: los números de build solo tienen sentido para una versión exacta | probado |
| `.env` guardado con finales de línea de Windows | `make` los elimina al leer las versiones; para ir sobre seguro, guarda `.env` con finales LF | |
| Se cambió `.env` pero se ejecutó `make restart` | Se mantiene la configuración anterior; `make restart` no vuelve a leer `.env`. Usa `make up`. | |
| `MOTD` con acentos, otros alfabetos o emoji | Se escribe como escapes `\uXXXX`, así que todas las versiones de Minecraft lo muestran bien | probado |
| `enable-status=false` (oculto de las listas de servidores) | La comprobación de salud recurre a RCON; el servidor sigue sano | probado |
| `server-ip` definido en `server.properties` | Se vacía al arrancar; dentro de Docker el servidor debe escuchar en todas las interfaces | |

## Versiones de Minecraft

| Situación | Comportamiento | |
|---|---|---|
| `MC_VERSION` no existe (`9.9`) | `make` se detiene: no está disponible, ejecuta `make versions` | probado |
| `MC_VERSION` demasiado antigua (`1.16.5`) | `make` se detiene: se soporta la 1.17 o posterior | probado |
| Una versión que solo tiene builds beta de Paper | `make` se detiene: aún no hay build estable | |
| `latest` pasa a una nueva versión de Minecraft | Solo ocurre al reconstruir la imagen. Fija `MC_VERSION` para evitar sorpresas. | |
| Se sube `MC_VERSION` | Copia del mundo y luego actualización; los plugins se vuelven a resolver | probado |
| Se baja `MC_VERSION` | Se niega a arrancar con instrucciones (protección contra bajadas de versión) | probado |
| Se restaura una copia de una versión más nueva | La rechaza la protección contra bajadas de versión en el siguiente arranque | |
| `JAVA_VERSION` demasiado antiguo para la versión de Minecraft | `make` se detiene indicando el Java mínimo necesario | |
| `docker compose up --build` directo con un `MC_VERSION` antiguo | La construcción se detiene con "Build with 'make up'"; Compose directo usa Java 25 por defecto | |

## Copias de seguridad y restauración

| Situación | Comportamiento | |
|---|---|---|
| Copia manual y programada en el mismo momento | Se ejecutan una tras otra (bloqueo compartido), así que el autoguardado nunca se reactiva a mitad de copia | probado |
| No hay espacio suficiente en disco | Se rechaza la copia con los tamaños en el mensaje; el servidor sigue funcionando con seguridad | probado |
| Copia interrumpida (contenedor matado) | Solo queda un archivo oculto `.partial`, que la siguiente copia elimina. Las copias terminadas nunca están a medias. | |
| Archivo corrupto tras escribirlo | Lo detecta `gzip -t`, se descarta y se conservan las copias anteriores | |
| Dos copias en el mismo segundo | La segunda recibe un sufijo único en lugar de sobrescribir | |
| Servidor detenido durante una copia programada | La copia sigue sin pausar los guardados (no hay nada escribiendo) | |
| `make restore` con un nombre de archivo mal escrito | Se detiene antes de tocar nada; el servidor sigue en marcha | probado |
| `make restore` con un archivo corrupto | Se detiene antes de tocar nada (`gzip -t`) | |
| Falla la extracción durante la restauración (disco lleno) | Los datos actuales quedan intactos, porque la extracción se hace primero en una carpeta temporal | |
| Restaurar mientras el servidor, por algún motivo, sigue en marcha | Se rechaza | |

## Plugins

| Situación | Comportamiento | |
|---|---|---|
| El sitio del plugin dice que una versión soporta tu Minecraft, pero necesita un Java más nuevo | Se detecta en el jar; se usa la versión más reciente que funciona | probado |
| Ninguna versión funciona en este servidor | Se omite con una explicación y una pista sobre `JAVA_VERSION`; se elimina una copia instalada que no puede cargarse | |
| Una dependencia no está declarada en el sitio pero sí en `plugin.yml` | Se busca por nombre en Hangar o Modrinth, y solo se acepta si el nombre de plugin del jar coincide | |
| Una dependencia no se encuentra en ningún sitio | Aviso con su nombre; el servidor arranca igualmente | |
| El mismo plugin listado desde Modrinth y desde Hangar | Se conserva el primero y se omite el segundo, así Paper nunca ve duplicados | |
| Plugin también instalado a mano | Se omite la copia gestionada | |
| Errata en el nombre de un proyecto, o proyecto borrado | Aviso; el resto de plugins no se ven afectados | probado |
| Modrinth o Hangar caídos durante el arranque | Aviso; se mantienen las versiones instaladas; se reintenta en el siguiente arranque | |
| Dos actualizaciones de plugins a la vez | La segunda se rechaza con "already running" | probado |
| Contenedor detenido mientras se aplican plugins preparados | El paso de aplicación se puede repetir con seguridad y termina en el siguiente arranque | |
| `plugins.txt` guardado con marca de orden de bytes o finales de línea de Windows | Se gestiona | |
| Falta `plugins.txt` | Los plugins se dejan como están (no se desinstala nada) | |
| Se borran todas las líneas de `plugins.txt` | Se desinstalan todos los plugins gestionados; los jars instalados a mano se quedan | |

## Jugadores y modos

| Situación | Comportamiento | |
|---|---|---|
| Un jugador sin conexión entra como `steve` cuando existe `Steve` | Rechazado: "already taken, log in as exactly Steve" | probado |
| Nombre sin conexión con espacios, guiones, acentos o puntos | Rechazado, con la regla explicada | probado |
| Pasar de con conexión → sin conexión con jugadores existentes | Datos movidos a los ID sin conexión, tras una copia | probado |
| Pasar de sin conexión → con conexión, jugador sin cuenta | Sus datos se conservan bajo el ID sin conexión para cuando se vuelva a cambiar | probado |
| Pasar de sin conexión → con conexión sin acceso a Mojang | El servidor no arranca (evita datos repartidos); Docker reintenta | |
| El jugador tiene datos bajo los dos ID | Se conserva el archivo más nuevo; el más antiguo va a `player-migration-conflicts/` | probado |
| Jugadores de Bedrock mediante Geyser/Floodgate (`.Name`) | Rechazados por las reglas de nombres; pon `ENFORCE_USERNAME_RULES=false` | |

## En ejecución

| Situación | Comportamiento | |
|---|---|---|
| El proceso del servidor falla (`kill -9`) | Docker lo reinicia; vuelve a estar sano en segundos | probado |
| El servidor se congela pero el proceso sigue vivo | El watchdog lo reinicia tras unos 5 minutos sin respuesta | |
| Primer arranque muy lento (generación del mundo) | El watchdog espera a la primera respuesta; la comprobación de salud da 5 minutos | |
| Java se queda sin memoria | Termina limpiamente y Docker lo reinicia | |
| Se escribe `/stop` en el juego | Guarda y se detiene; Docker lo vuelve a arrancar (usa `make down` para mantenerlo parado) | |
| Un mundo muy grande no termina de guardarse en 2 minutos | Sube `STOP_TIMEOUT` | |
| El puerto 25565 del equipo ya está en uso | `make up` falla con "port is already allocated"; define `SERVER_PORT` | |
| Reinicio del equipo | Los contenedores vuelven cuando arranca Docker | |

## Límites conocidos

- Solo Minecraft: Java Edition 1.17 y posteriores, sobre Paper.
- Solo se buscan plugins automáticamente en Modrinth y Hangar.
- Los datos de plugins guardados bajo UUID de jugadores no se migran entre modo con y sin conexión.
- `make update-plugins`, `make backup` y `make console` necesitan que el contenedor del servidor esté en marcha.
- `make` necesita que Docker pueda descargar `eclipse-temurin:25-jdk` (una vez) para resolver versiones.
