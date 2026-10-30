<!-- language-switcher -->
[English](../getting-started.md) · [हिन्दी](../hi/getting-started.md) · **Español** · [简体中文](../zh-CN/getting-started.md) · [Português (BR)](../pt-BR/getting-started.md) · [日本語](../ja/getting-started.md) · [한국어](../ko/getting-started.md)

# <img src="../assets/icons/start.svg" width="28" height="28" alt=""> Primeros pasos

Esta guía te lleva de cero a un servidor de Minecraft funcionando al que puedes entrar, en unos cinco minutos.

## Qué obtienes

Ejecutar `make up` arranca dos contenedores:

- **`mc`**: un servidor de Minecraft: Java Edition con [Paper](https://papermc.io). Paper es un reemplazo directo del servidor oficial, más rápido y compatible con plugins.
- **`backup`**: un pequeño contenedor auxiliar que hace una copia de seguridad de tu mundo cada hora.

El servidor se reinicia solo tras un fallo, guarda el mundo correctamente al detenerse y rechaza configuraciones que lo romperían. No necesitas saber Docker para usarlo a diario; todo es un comando `make`.

## Requisitos

| | |
|---|---|
| Docker | Docker Desktop (macOS, Windows) o Docker Engine 24+ con el plugin Compose (Linux) |
| `make` | Viene instalado en macOS y en la mayoría de distribuciones Linux |
| Memoria | Unos 3 GB libres con la configuración por defecto |
| Red | Acceso a internet durante la instalación para descargar Paper, Java y plugins |

## Instalación

**1. Crea tu archivo de configuración.**

```bash
cp .env.example .env
```

**2. Cambia tres valores en `.env`.**

```env
EULA=TRUE
RCON_PASSWORD=paste-a-long-random-value-here
MC_VERSION=latest
```

- `EULA=TRUE` significa que aceptas el [EULA de Minecraft](https://aka.ms/MinecraftEULA). Sin él, el servidor no arranca.
- `RCON_PASSWORD` se usa internamente para guardar el mundo de forma segura durante las copias de seguridad y los apagados. Genera una con `openssl rand -hex 24`. Nunca se expone fuera de Docker.
- `MC_VERSION` es la versión de Minecraft que se ejecutará. Deja `latest` o elige una de `make versions`, por ejemplo `1.21.4`. **Elige ahora:** un mundo puede actualizarse después a una versión más nueva, pero nunca volver a una más antigua. Consulta [Versiones de Minecraft](minecraft-versions.md).

**3. Arráncalo.**

```bash
make up
```

La primera vez se descargan Java y Paper y se construye la imagen, lo que tarda unos minutos. Verás qué versión se ha elegido:

```
==> Minecraft 26.3 (Paper) on Java 25
```

**4. Espera a que esté healthy.**

```bash
make status
```

Cuando el contenedor `mc` muestre `(healthy)`, el servidor está listo. Para verlo arrancar, ejecuta `make logs` y pulsa <kbd>Ctrl</kbd>+<kbd>C</kbd> para dejar de seguir los logs. Eso no detiene el servidor.

## Conéctate

En Minecraft, elige **Multijugador → Añadir servidor** e introduce:

- `localhost` si el servidor está en el mismo ordenador, o
- la dirección IP del servidor si está en otro sitio. En un servidor en la nube, abre también el puerto 25565; consulta [Despliegue](deployment.md).

## Hazte operador

```bash
make cmd C="op YourMinecraftName"
```

## Siguientes pasos habituales

| Quiero… | Ve a |
|---|---|
| Dejar entrar a amigos sin cuenta oficial de Minecraft | [Modo con y sin conexión](offline-mode.md) |
| Añadir plugins | [Plugins](plugins.md) |
| Darle más memoria al servidor | `MEMORY` y `CONTAINER_MEMORY` en la [referencia de configuración](configuration.md#recursos) |
| Ejecutarlo en un servidor en la nube | [Despliegue](deployment.md) |
| Ver todos los comandos | [Operaciones](operations.md) |

## Cambiar la configuración más tarde

Edita `.env` y ejecuta:

```bash
make up
```

`make up` aplica la nueva configuración y conserva tu mundo. (`make restart` solo reinicia con la configuración *anterior*.)
