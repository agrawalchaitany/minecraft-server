<!-- language-switcher -->
[English](../getting-started.md) · [हिन्दी](../hi/getting-started.md) · [Español](../es/getting-started.md) · [简体中文](../zh-CN/getting-started.md) · **Português (BR)** · [日本語](../ja/getting-started.md) · [한국어](../ko/getting-started.md)

# <img src="../assets/icons/start.svg" width="28" height="28" alt=""> Primeiros passos

Este guia leva você do zero a um servidor de Minecraft rodando, no qual dá para entrar, em uns cinco minutos.

## O que você ganha

Rodar `make up` inicia dois contêineres:

- **`mc`**: um servidor de Minecraft: Java Edition com [Paper](https://papermc.io). O Paper substitui diretamente o servidor oficial, é mais rápido e aceita plugins.
- **`backup`**: um pequeno contêiner auxiliar que faz backup do seu mundo a cada hora.

O servidor se reinicia sozinho após travar, salva o mundo corretamente ao parar e recusa configurações que o quebrariam. Você não precisa saber Docker para usar no dia a dia; tudo é um comando `make`.

## Requisitos

| | |
|---|---|
| Docker | Docker Desktop (macOS, Windows) ou Docker Engine 24+ com o plugin Compose (Linux) |
| `make` | Já vem instalado no macOS e na maioria das distribuições Linux |
| Memória | Cerca de 3 GB livres com as configurações padrão |
| Rede | Acesso à internet na instalação para baixar Paper, Java e plugins |

## Instalação

**1. Crie seu arquivo de configuração.**

```bash
cp .env.example .env
```

**2. Altere dois valores no `.env`.**

```env
EULA=TRUE
MC_VERSION=latest
```

- `EULA=TRUE` significa que você aceita o [EULA do Minecraft](https://aka.ms/MinecraftEULA). Sem isso, o servidor não inicia.
- `RCON_PASSWORD` é opcional. É usado internamente para salvar o mundo com segurança durante backups e desligamentos. Se ficar vazio, um aleatório é gerado e guardado no volume de dados. Ele nunca fica exposto fora do Docker.
- `MC_VERSION` é a versão do Minecraft a rodar. Mantenha `latest` ou escolha uma de `make versions`, como `1.21.4`. **Escolha agora:** o mundo pode ser atualizado para uma versão mais nova depois, mas nunca voltar para uma mais antiga. Veja [Versões do Minecraft](minecraft-versions.md).

**3. Inicie.**

```bash
make up
```

Na primeira vez, o Java e o Paper são baixados e a imagem é construída, o que leva alguns minutos. Você verá qual versão foi escolhida:

```
==> Minecraft 26.3 (Paper) on Java 25
```

**4. Espere ficar healthy.**

```bash
make status
```

Quando o contêiner `mc` mostrar `(healthy)`, o servidor está pronto. Para ver a inicialização, rode `make logs` e aperte <kbd>Ctrl</kbd>+<kbd>C</kbd> para parar de acompanhar. Isso não para o servidor.

## Conecte-se

No Minecraft, escolha **Multijogador → Adicionar servidor** e digite:

- `localhost`, se o servidor roda no mesmo computador, ou
- o endereço IP do servidor, se ele roda em outro lugar. Num servidor na nuvem, abra também a porta 25565; veja [Implantação](deployment.md).

## Torne-se operador

```bash
make cmd C="op YourMinecraftName"
```

## Próximos passos comuns

| Eu quero… | Vá para |
|---|---|
| Deixar entrar amigos sem conta oficial do Minecraft | [Modo online e offline](offline-mode.md) |
| Adicionar plugins | [Plugins](plugins.md) |
| Dar mais memória ao servidor | `MEMORY` e `CONTAINER_MEMORY` na [referência de configuração](configuration.md#recursos) |
| Rodar num servidor na nuvem | [Implantação](deployment.md) |
| Ver todos os comandos | [Operação](operations.md) |

## Mudar configurações depois

Edite o `.env` e rode:

```bash
make up
```

O `make up` aplica as novas configurações e mantém seu mundo. (`make restart` só reinicia com as configurações *antigas*.)
