<!-- language-switcher -->
[English](../configuration.md) · [हिन्दी](../hi/configuration.md) · [Español](../es/configuration.md) · [简体中文](../zh-CN/configuration.md) · **Português (BR)** · [日本語](../ja/configuration.md) · [한국어](../ko/configuration.md)

# <img src="../assets/icons/configuration.svg" width="28" height="28" alt=""> Referência de configuração

Todas as configurações ficam no `.env`, na pasta do projeto. Comece pelo modelo comentado:

```bash
cp .env.example .env
```

## Aplicar mudanças

| Você mudou… | Rode | Por quê |
|---|---|---|
| Qualquer configuração do `.env` | `make up` | Recria o contêiner com os novos valores. Seu mundo é mantido. |
| `MC_VERSION`, `PAPER_BUILD`, `JAVA_VERSION` | `make up` | São configurações de build; o `make up` reconstrói a imagem. |
| `plugins/plugins.txt` | `make update-plugins` | Veja [Plugins](plugins.md). |

O `make restart` reinicia com as configurações *antigas*. O `.env` só é lido quando um contêiner é criado.

Valores inválidos são recusados na inicialização com uma mensagem que cita a configuração, então um erro de digitação nunca vira um loop de travamento. Confira `make logs` se o servidor não subir.

## Obrigatórias

| Variável | Padrão | Descrição |
|---|---|---|
| `EULA` | `false` | Precisa ser `TRUE` para aceitar o [EULA do Minecraft](https://aka.ms/MinecraftEULA). |
| `RCON_PASSWORD` | valor de exemplo | Senha interna para salvamentos seguros, backups e o console. Precisa ser trocada do valor de exemplo. Sem barras invertidas e sem espaços no começo ou no fim. `openssl rand -hex 24` gera uma boa. |

## Versão

| Variável | Padrão | Descrição |
|---|---|---|
| `MC_VERSION` | `latest` | Versão do Minecraft: `latest` (versão estável mais nova do Paper) ou uma versão exata como `1.21.4`. Minecraft 1.17 ou mais novo. Liste com `make versions`. |
| `PAPER_BUILD` | `latest` | Um número de build específico do Paper. Só vale com um `MC_VERSION` exato, porque os números de build são por versão. |
| `JAVA_VERSION` | *(não definido)* | Normalmente fica sem definir; o Java 17, 21 ou 25 é escolhido conforme a versão do Minecraft. Defina só para forçar um Java mais novo, por exemplo quando um plugin precisa. |
| `ALLOW_DOWNGRADE` | `false` | Permite abrir um mundo numa versão do Minecraft mais antiga que a última em que ele rodou. Isso pode corromper o mundo. Veja [Versões do Minecraft](minecraft-versions.md#fazer-downgrade). |

## Recursos

| Variável | Padrão | Descrição |
|---|---|---|
| `MEMORY` | `2G` | Tamanho do heap do Java, como `4G` ou `1536M`. É reservado na inicialização (`-Xms` = `-Xmx`). |
| `CONTAINER_MEMORY` | `3G` | Limite rígido de memória do contêiner. Precisa ser pelo menos `MEMORY` + 512M; recomenda-se `MEMORY` + 1G. O servidor se recusa a iniciar se o heap não couber. |
| `JVM_OPTS` | *(vazio)* | Flags extras da JVM, adicionadas depois das embutidas. |

Tamanho aproximado: `2G` para poucos amigos, `4G` para uns 10 jogadores, `6G`–`8G` para mais de 20 jogadores ou plugins pesados.

## Rede

| Variável | Padrão | Descrição |
|---|---|---|
| `SERVER_PORT` | `25565` | Porta da máquina em que os jogadores se conectam. Dentro do contêiner o servidor sempre usa a 25565. |

## Jogo

Estas são gravadas no `server.properties` a cada inicialização. Deixe um valor **vazio** para gerenciar aquela chave direto no `server.properties`.

| Variável | Padrão | Chave do `server.properties` |
|---|---|---|
| `MOTD` | `A reliable Paper server` | `motd`. Qualquer idioma e emoji servem; códigos de cor `§` funcionam. |
| `MAX_PLAYERS` | `20` | `max-players` |
| `DIFFICULTY` | `normal` | `difficulty`: `peaceful`, `easy`, `normal`, `hard` |
| `GAMEMODE` | `survival` | `gamemode`: `survival`, `creative`, `adventure`, `spectator` |
| `ONLINE_MODE` | `true` | `online-mode`. `false` deixa entrar jogadores sem conta oficial. Veja [Modo online e offline](offline-mode.md). |
| `ENFORCE_USERNAME_RULES` | `true` | Não é uma propriedade. No modo offline, exige nomes de usuário no estilo da Mojang. |
| `VIEW_DISTANCE` | `10` | `view-distance` |
| `SIMULATION_DISTANCE` | `10` | `simulation-distance` |
| `LEVEL_NAME` | `world` | `level-name`. Mudar troca para outro mundo (ou cria um). |
| `SEED` | *(vazio)* | `level-seed`. Só afeta mundos gerados do zero. |
| `PVP` | `true` | `pvp` |
| `WHITELIST` | `false` | `white-list` e `enforce-whitelist` |
| `MIGRATE_PLAYER_DATA` | `true` | Não é uma propriedade. Mantém os inventários dos jogadores quando `ONLINE_MODE` muda. |

O servidor sempre define estas, não importa o que esteja no `server.properties`: `server-port=25565`, `server-ip=` (escutar em todas as interfaces), `enable-rcon=true`, `rcon.port` e `rcon.password`.

## Plugins

| Variável | Padrão | Descrição |
|---|---|---|
| `PLUGIN_AUTO_UPDATE` | `false` | Procura atualizações de plugins todo dia e reinicia para aplicá-las. Veja [Plugins](plugins.md#atualizações-automáticas). |
| `PLUGIN_UPDATE_TIME` | `04:00` | Quando procurar, no formato `HH:MM` de 24 horas no `TZ`. |
| `TZ` | `UTC` | Fuso horário, como `America/Sao_Paulo` ou `Europe/London`. |

## Backups

| Variável | Padrão | Descrição |
|---|---|---|
| `BACKUP_INTERVAL` | `1h` | Tempo entre backups programados: um número com `s`, `m`, `h` ou `d`, como `30m` ou `6h`. |
| `BACKUP_KEEP` | `24` | Quantos backups manter (pelo menos 1). Os mais antigos são apagados. |

## Confiabilidade

| Variável | Padrão | Descrição |
|---|---|---|
| `STOP_TIMEOUT` | `2m` | Quanto o Docker espera o mundo ser salvo no desligamento antes de forçar o encerramento. Aumente para mundos muito grandes. |
| `WATCHDOG_INTERVAL` | `60` | Segundos entre verificações de travamento. `0` desativa o watchdog. |
| `WATCHDOG_FAILURES` | `5` | Verificações com falha seguidas antes de reiniciar um servidor travado. |

## Dono dos arquivos

| Variável | Padrão | Descrição |
|---|---|---|
| `PUID` / `PGID` | `1000` | Usuário e grupo donos dos arquivos do servidor e do `./backups`. No Linux, use a saída de `id -u` e `id -g`. |

## Avançado

| Variável | Padrão | Descrição |
|---|---|---|
| `RCON_PORT` | `25575` | Porta RCON interna. Nunca é publicada; mude só se um plugin precisar da 25575. |
