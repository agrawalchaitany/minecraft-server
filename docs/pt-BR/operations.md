<!-- language-switcher -->
[English](../operations.md) · [हिन्दी](../hi/operations.md) · [Español](../es/operations.md) · [简体中文](../zh-CN/operations.md) · **Português (BR)** · [日本語](../ja/operations.md) · [한국어](../ko/operations.md)

# <img src="../assets/icons/operations.svg" width="28" height="28" alt=""> Operação

Tarefas do dia a dia. Rode todos os comandos a partir da pasta do projeto.

## Referência de comandos

| Comando | O que faz |
|---|---|
| `make up` | Constrói se preciso, aplica o `.env` e inicia tudo |
| `make down` | Para tudo com segurança (os mundos são salvos) |
| `make restart` | Reinicia o servidor do jogo com segurança (mantém as configurações atuais) |
| `make status` | Estado e saúde dos contêineres |
| `make logs` | Acompanha os logs (<kbd>Ctrl</kbd>+<kbd>C</kbd> para de acompanhar, não para o servidor) |
| `make console` | Console interativo do servidor (digite `exit` para sair) |
| `make cmd C="…"` | Roda um comando do servidor |
| `make backup` | Faz um backup agora |
| `make backups` | Lista os backups |
| `make restore FILE=…` | Restaura um backup; veja [Backups e restauração](backups.md) |
| `make update` | Reconstrói com o build mais novo do Paper da sua versão do Minecraft |
| `make versions` | Lista as versões do Minecraft e o Java que cada uma precisa |
| `make plugins` | Mostra os plugins instalados e por que cada um está lá |
| `make update-plugins` | Aplica o `plugins/plugins.txt`; veja [Plugins](plugins.md) |

## Está saudável?

```bash
make status
```

`(healthy)` significa que o servidor está respondendo. Na primeira inicialização, gerar um mundo novo pode demorar; a verificação de saúde dá cinco minutos de margem. Veja [Confiabilidade](reliability.md#verificação-de-saúde) para o que "healthy" significa.

## Console do servidor

```bash
make console            # interactive
make cmd C="list"       # one command
make cmd C="op Steve"
make cmd C="whitelist add Steve"
make cmd C="say Restarting soon"
```

Você também pode se conectar ao console bruto com `docker compose attach mc`. Saia com <kbd>Ctrl</kbd>+<kbd>P</kbd> e depois <kbd>Ctrl</kbd>+<kbd>Q</kbd>. Apertar <kbd>Ctrl</kbd>+<kbd>C</kbd> ali para o servidor, com segurança.

## Logs

```bash
make logs                                  # all services, follow
docker compose logs mc --since 1h          # game server, last hour
docker compose logs backup                 # backup history
docker compose exec mc ls logs             # Paper's own log files
```

O Docker guarda no máximo 5 × 10 MB de logs por contêiner, então os logs nunca enchem o disco.

## Editar arquivos do servidor

Os arquivos do servidor ficam no volume `mc-data`. Para editar um:

```bash
docker compose cp mc:/data/config/paper-global.yml .
# edit it
docker compose cp paper-global.yml mc:/data/config/paper-global.yml
make restart
```

As configurações controladas pelo `.env` (veja a [referência de configuração](configuration.md#jogo)) são sobrescritas a cada inicialização. Mude essas no `.env`.

## Atualizar

| O quê | Como |
|---|---|
| Build mais novo do Paper, mesma versão do Minecraft | `make update` |
| Versão mais nova do Minecraft | Mude `MC_VERSION` e rode `make up`. Um backup é feito antes de o mundo ser atualizado. Veja [Versões do Minecraft](minecraft-versions.md). |
| Plugins | `make update-plugins`. Veja [Plugins](plugins.md). |
| Este projeto | Substitua os arquivos do projeto (mantenha `.env`, `plugins/plugins.txt` e `backups/`) e rode `make up` |

## Uso de disco

```bash
docker system df -v | grep mc-data     # world volume size
du -sh backups                         # backups
```

O `BACKUP_KEEP` controla quanto espaço os backups usam. Os backups se recusam a rodar se puderem encher o disco; veja [Backups e restauração](backups.md#segurança-do-espaço-em-disco).

## Começar do zero

Para apagar o mundo e todos os dados do servidor (os backups em `./backups` são mantidos):

```bash
make backup                 # optional: keep a final copy
docker compose down -v      # -v deletes the mc-data volume
make up
```
