<!-- language-switcher -->
[English](../deployment.md) · [हिन्दी](../hi/deployment.md) · **Español** · [简体中文](../zh-CN/deployment.md) · [Português (BR)](../pt-BR/deployment.md) · [日本語](../ja/deployment.md) · [한국어](../ko/deployment.md)

# <img src="../assets/icons/deployment.svg" width="28" height="28" alt=""> Despliegue

El mismo proyecto funciona en macOS (para pruebas o para jugar con amigos) y en servidores Linux (para algo siempre encendido). Las imágenes se construyen de forma nativa tanto para Intel/AMD (`amd64`) como para ARM (`arm64`, incluido Apple Silicon).

## Servidor Linux (VPS, servidor doméstico)

### 1. Instala Docker

Instala Docker Engine y el plugin Compose siguiendo las instrucciones de Docker para tu distribución y luego:

```bash
sudo systemctl enable --now docker      # start Docker on boot
sudo usermod -aG docker "$USER"          # run docker without sudo (log out and in again)
```

### 2. Copia el proyecto

```bash
git clone <your-repository> minecraft && cd minecraft
# or: scp -r Minecraft-Server/ user@server:~/minecraft
```

### 3. Configura

```bash
cp .env.example .env
id -u && id -g          # put these into PUID / PGID
```

En `.env`, define como mínimo `EULA`, `MC_VERSION` (fija una versión exacta), `MEMORY` y `CONTAINER_MEMORY`.

| RAM del servidor | `MEMORY` | `CONTAINER_MEMORY` |
|---|---|---|
| 4 GB | `2G` | `3G` |
| 8 GB | `5G` | `6G` |
| 16 GB | `10G` | `12G` |

Deja aproximadamente 1 GB para el sistema operativo.

### 4. Abre el puerto del juego

```bash
sudo ufw allow 25565/tcp
```

Permite también TCP 25565 en el firewall o grupo de seguridad de tu proveedor en la nube, si lo tiene. **Nunca abras el 25575** (RCON); es solo interno.

### 5. Arranca

```bash
make up
```

Los contenedores vuelven automáticamente tras un reinicio.

## macOS

Instala [Docker Desktop](https://www.docker.com/products/docker-desktop/) y sigue [Primeros pasos](getting-started.md). A tener en cuenta:

- Docker Desktop tiene su propio límite de memoria (**Settings → Resources**). Debe ser mayor que `CONTAINER_MEMORY`.
- Los amigos en tu red doméstica se conectan a la IP local de tu Mac. Para amigos de fuera, redirige el puerto TCP 25565 de tu router a tu Mac.
- Docker Desktop debe estar en marcha para que funcione el servidor. Activa "Start Docker Desktop when you sign in" para que vuelva tras reiniciar.

## Copias fuera del servidor

`./backups` está en el mismo disco que el servidor. Cópiala a otro sitio de forma programada, por ejemplo con `rsync` desde cron en el servidor:

```bash
# crontab -e   (every day at 05:00)
0 5 * * * rsync -a --delete /home/me/minecraft/backups/ backup-host:/srv/minecraft-backups/
```

Sirve cualquier herramienta (`rclone` a almacenamiento en la nube, `restic`, etc.). Copia solo los archivos `mc-*.tar.gz` terminados; los archivos ocultos `.partial` son copias en curso.

## Migrar a otra máquina

1. `make backup` en la máquina antigua.
2. Instala el proyecto en la máquina nueva con el mismo `.env` y `plugins/plugins.txt`.
3. Copia la copia de seguridad al nuevo `./backups/`.
4. `make up` y luego `make restore FILE=…`. Consulta [Copias de seguridad y restauración](backups.md#restaurar-en-una-máquina-nueva).
5. Indica a los jugadores (o a tu nombre DNS) la nueva dirección.

## Actualizar el propio proyecto

Sustituye los archivos del proyecto por la nueva versión, pero conserva `.env`, `plugins/plugins.txt` y `backups/`. Luego:

```bash
make up
```

Tu mundo vive en el volumen de Docker y no se ve afectado al sustituir los archivos.

## Lista de seguridad

- Mantén `ONLINE_MODE=true` salvo que necesites el modo sin conexión, y si lo necesitas, lee [los riesgos](offline-mode.md#el-riesgo-los-nombres-de-usuario-no-se-verifican).
- Deja `RCON_PASSWORD` vacío para que se genere uno, o usa un valor largo y aleatorio. Nunca publiques el puerto 25575.
- Mantén `.env` en privado; contiene la contraseña RCON. Está excluido de git y del build de Docker.
- Mantén el sistema actualizado y usa claves SSH en lugar de contraseñas.
- Haz operadores solo a quienes lo necesiten; `make cmd C="deop Name"` lo quita.
- Fija `MC_VERSION` y revisa las actualizaciones de plugins antes de aplicarlas.
