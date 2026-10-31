<div align="center">

<img src="../assets/banner.svg" alt="Minecraft Server: um servidor Paper feito do zero e pronto para produção com Docker" width="100%">

[![Minecraft](https://img.shields.io/badge/Minecraft-1.17%20to%20latest-62B47A)](../pt-BR/minecraft-versions.md)
[![Paper](https://img.shields.io/badge/Paper-stable%20builds-1f6feb)](https://papermc.io)
[![Java](https://img.shields.io/badge/Java-17%20%7C%2021%20%7C%2025%20(auto)-orange?logo=openjdk&logoColor=white)](../pt-BR/minecraft-versions.md#como-o-java-é-escolhido)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)](https://docs.docker.com/compose/)
[![Platforms](https://img.shields.io/badge/platform-linux%2Famd64%20%7C%20linux%2Farm64%20%7C%20macOS-lightgrey)](../pt-BR/deployment.md)

[Início rápido](#início-rápido) ·
[Recursos](#recursos) ·
[Documentação](../pt-BR/README.md) ·
[Solução de problemas](../pt-BR/troubleshooting.md)

<!-- language-switcher -->
[English](../../README.md) · [हिन्दी](README.hi.md) · [Español](README.es.md) · [简体中文](README.zh-CN.md) · **Português (BR)** · [日本語](README.ja.md) · [한국어](README.ko.md)

</div>

---

Rode qualquer versão do Minecraft: Java Edition, da 1.17 até a mais recente, no [Paper](https://papermc.io), com um único comando. O servidor se reinicia sozinho, salva o mundo corretamente, faz backups programados, mantém os plugins compatíveis e recusa configurações que o quebrariam.

Tudo foi construído do zero: nenhuma imagem ou script de servidor de terceiros. As únicas fontes são as imagens oficiais de Java `eclipse-temurin` e os serviços oficiais de download da PaperMC, Mojang, Modrinth e Hangar. O checksum de cada jar de servidor e de plugin é verificado.

## Início rápido

### Imagem pronta

Sem build: Minecraft 26.3 (última estável) no Java 25, sem plugins, para Intel/AMD e ARM.

```bash
docker run -d --name minecraft --restart unless-stopped \
  -p 25565:25565 -e EULA=TRUE -e MEMORY=2G \
  -v minecraft-data:/data \
  agrawalchaitany/minecraft-server:26.3
```

Definir `EULA=TRUE` significa aceitar o [EULA do Minecraft](https://aka.ms/MinecraftEULA). Conecte em `localhost:25565`; abra o console com `docker exec -it minecraft rcon-cli`. A mesma imagem está no GitHub Packages como `ghcr.io/agrawalchaitany/minecraft-server`.

Com backups programados (em `./backups`), use o [arquivo compose](../../quickstart/compose.yaml) pronto:

```bash
mkdir minecraft && cd minecraft
curl -fsSLO https://raw.githubusercontent.com/agrawalchaitany/minecraft-server/main/quickstart/compose.yaml
echo EULA=TRUE > .env
docker compose up -d
```

### Construir a partir do código

Para escolher a versão do Minecraft, adicionar plugins ou mudar qualquer outra coisa, construa você mesmo:

```bash
cp .env.example .env
```

Edite o `.env`:

```env
EULA=TRUE                         # accept https://aka.ms/MinecraftEULA
RCON_PASSWORD=                    # optional; generated automatically if empty
MC_VERSION=latest                 # or an exact version, e.g. 1.21.4 (see: make versions)
```

Inicie:

```bash
make up
make status        # wait for (healthy), then connect to localhost:25565
```

Passo a passo completo: **[Primeiros passos](../pt-BR/getting-started.md)**.

## Recursos

<table>
<tr>
<td width="33%" valign="top">

<img src="../assets/icons/versions.svg" width="32" height="32" alt=""><br>**Qualquer versão**

Minecraft 1.17 até a mais recente. O Java certo (17, 21 ou 25) é escolhido automaticamente. Atualizações fazem backup do mundo antes; downgrades que o corromperiam são recusados. [Mais](../pt-BR/minecraft-versions.md)

</td>
<td width="33%" valign="top">

<img src="../assets/icons/reliability.svg" width="32" height="32" alt=""><br>**Autorrecuperação**

Reinicia após travamentos e falta de memória. Uma verificação de saúde e um watchdog que reinicia um servidor congelado. [Mais](../pt-BR/reliability.md)

</td>
<td width="33%" valign="top">

<img src="../assets/icons/shutdown.svg" width="32" height="32" alt=""><br>**Desligamento seguro**

Toda parada salva antes todos os mundos pelo console do servidor. [Mais](../pt-BR/reliability.md#desligamento-seguro)

</td>
</tr>
<tr>
<td width="33%" valign="top">

<img src="../assets/icons/backups.svg" width="32" height="32" alt=""><br>**Backups**

Programados e consistentes mesmo com jogadores online, com trava para nunca se sobreporem, verificados, com checagem de espaço em disco e rotação. Feitos automaticamente antes de atualizações, migrações, atualizações de plugins e restaurações. [Mais](../pt-BR/backups.md)

</td>
<td width="33%" valign="top">

<img src="../assets/icons/restore.svg" width="32" height="32" alt=""><br>**Restauração segura**

Um comando. Verifica o arquivo primeiro, faz um backup de segurança e extrai numa pasta temporária, para que uma falha não faça você perder o mundo atual. [Mais](../pt-BR/backups.md#restaurar)

</td>
<td width="33%" valign="top">

<img src="../assets/icons/plugins.svg" width="32" height="32" alt=""><br>**Gerenciador de plugins**

Liste os plugins no `plugins.txt`. Para cada um, é escolhida a versão mais recente que realmente funciona no seu Minecraft e Java, inspecionando o próprio jar. Dependências obrigatórias são instaladas. Conflitos e duplicatas são detectados. Atualizações sob demanda ou diárias. [Mais](../pt-BR/plugins.md)

</td>
</tr>
<tr>
<td width="33%" valign="top">

<img src="../assets/icons/players.svg" width="32" height="32" alt=""><br>**Modo offline**

Deixe entrar jogadores sem conta oficial, com as regras de nome de usuário da Mojang aplicadas e os inventários mantidos ao trocar de modo. [Mais](../pt-BR/offline-mode.md)

</td>
<td width="33%" valign="top">

<img src="../assets/icons/guard.svg" width="32" height="32" alt=""><br>**Proteções**

Cada configuração é validada na inicialização com uma mensagem clara. Nada de loops de travamento por erros de digitação. [Mais](../pt-BR/reliability.md#validação-da-configuração)

</td>
<td width="33%" valign="top">

<img src="../assets/icons/security.svg" width="32" height="32" alt=""><br>**Privilégio mínimo**

Roda como usuário sem privilégios de root. O console remoto nunca fica exposto fora do Docker. Os logs são rotacionados.

</td>
</tr>
</table>

## Comandos do dia a dia

| Comando | |
|---|---|
| `make up` | Iniciar, ou aplicar mudanças do `.env` |
| `make down` | Parar (os mundos são salvos) |
| `make status` / `make logs` | Saúde e logs |
| `make console` / `make cmd C="op Steve"` | Console do servidor |
| `make backup` / `make restore FILE=…` | Backup e restauração |
| `make update-plugins` / `make plugins` | Aplicar o `plugins.txt` e ver os plugins instalados |
| `make versions` / `make update` | Listar versões do Minecraft; atualizar o Paper |

Todos os comandos: [Operação](../pt-BR/operations.md).

## Documentação

| | Guia | |
|:-:|---|---|
| <img src="../assets/icons/start.svg" width="20" height="20" alt=""> | [Primeiros passos](../pt-BR/getting-started.md) | Funcionando em cinco minutos |
| <img src="../assets/icons/architecture.svg" width="20" height="20" alt=""> | [Arquitetura](../pt-BR/architecture.md) | Como funciona, do `make up` ao servidor rodando |
| <img src="../assets/icons/configuration.svg" width="20" height="20" alt=""> | [Configuração](../pt-BR/configuration.md) | Cada configuração do `.env` |
| <img src="../assets/icons/operations.svg" width="20" height="20" alt=""> | [Operação](../pt-BR/operations.md) | Tarefas do dia a dia |
| <img src="../assets/icons/versions.svg" width="20" height="20" alt=""> | [Versões do Minecraft](../pt-BR/minecraft-versions.md) | Versões, Java, atualizações e downgrades |
| <img src="../assets/icons/plugins.svg" width="20" height="20" alt=""> | [Plugins](../pt-BR/plugins.md) | `plugins.txt`, resolução de compatibilidade, atualizações automáticas, mods |
| <img src="../assets/icons/backups.svg" width="20" height="20" alt=""> | [Backups e restauração](../pt-BR/backups.md) | Como os backups ficam consistentes e as restaurações seguras |
| <img src="../assets/icons/reliability.svg" width="20" height="20" alt=""> | [Confiabilidade](../pt-BR/reliability.md) | Verificação de saúde, watchdog, desligamento, memória, validação |
| <img src="../assets/icons/players.svg" width="20" height="20" alt=""> | [Modo online e offline](../pt-BR/offline-mode.md) | Jogadores sem conta, regras de nome, dados de jogadores |
| <img src="../assets/icons/deployment.svg" width="20" height="20" alt=""> | [Implantação](../pt-BR/deployment.md) | Servidores Linux, macOS, firewalls, backups externos |
| <img src="../assets/icons/edge-cases.svg" width="20" height="20" alt=""> | [Casos extremos](../pt-BR/edge-cases.md) | Situações incomuns e como são tratadas |
| <img src="../assets/icons/troubleshooting.svg" width="20" height="20" alt=""> | [Solução de problemas](../pt-BR/troubleshooting.md) | Sintomas e soluções |

## Requisitos

- Docker Desktop (macOS/Windows) ou Docker Engine 24+ com o plugin Compose (Linux)
- `make`
- Cerca de 3 GB de memória livre com as configurações padrão

## Estrutura do projeto

```
Dockerfile, compose.yaml, Makefile, .env.example
scripts/     startup, backup, restore, and helper scripts
tools/       dependency-free Java tools (RCON, versions, plugins, player migration)
plugins/     plugins.txt and the built-in username-rules plugin
docs/        documentation
```

## Aviso legal

Sem afiliação com a Mojang Studios, Microsoft ou PaperMC. Minecraft é uma marca registrada da Mojang Studios. Para rodar o servidor é preciso aceitar o [EULA do Minecraft](https://aka.ms/MinecraftEULA).
