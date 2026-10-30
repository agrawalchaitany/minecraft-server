<!-- language-switcher -->
[English](../edge-cases.md) · [हिन्दी](../hi/edge-cases.md) · [Español](../es/edge-cases.md) · [简体中文](../zh-CN/edge-cases.md) · **Português (BR)** · [日本語](../ja/edge-cases.md) · [한국어](../ko/edge-cases.md)

# <img src="../assets/icons/edge-cases.svg" width="28" height="28" alt=""> Casos extremos

Como o servidor se comporta em situações incomuns. As entradas marcadas como **testado** foram reproduzidas num servidor real e o comportamento descrito foi observado.

## Erros de configuração

| Situação | Comportamento | |
|---|---|---|
| `EULA` não é `TRUE` | Recusa iniciar e explica como aceitar | testado |
| `RCON_PASSWORD` com o valor de exemplo | Recusa iniciar | testado |
| `RCON_PASSWORD` com barra invertida ou espaços nas pontas | Recusa iniciar. O `server.properties` mudaria a senha em silêncio, quebrando os salvamentos seguros. | testado |
| `MEMORY=2GB` (formato errado) | Recusa iniciar: "must look like 2G or 1536M" | testado |
| `MEMORY` maior do que o `CONTAINER_MEMORY` permite | Recusa iniciar e diz qual `CONTAINER_MEMORY` definir, em vez de um loop de encerramentos por falta de memória | testado |
| `WATCHDOG_INTERVAL=1m` | Recusa iniciar: precisa ser um número inteiro de segundos | testado |
| `PLUGIN_UPDATE_TIME=25:00` com atualização automática ligada | Recusa iniciar, em vez de a atualização automática simplesmente não rodar | testado |
| `BACKUP_INTERVAL=abc` | O contêiner de backup para com um erro, em vez de fazer backups sem parar | testado |
| `BACKUP_KEEP=0` | O backup é recusado, em vez de apagar todos os backups, inclusive o novo | testado |
| `PAPER_BUILD` definido com `MC_VERSION=latest` | O build para: números de build só fazem sentido para uma versão exata | testado |
| `.env` salvo com quebras de linha do Windows | O `make` as remove ao ler as versões; para garantir, salve o `.env` com quebras LF | |
| Mudou o `.env` mas rodou `make restart` | As configurações antigas continuam; o `make restart` não relê o `.env`. Use `make up`. | |
| `MOTD` com acentos, outros alfabetos ou emoji | Gravado como escapes `\uXXXX`, então toda versão do Minecraft mostra certo | testado |
| `enable-status=false` (oculto das listas de servidores) | A verificação de saúde usa o RCON; o servidor continua saudável | testado |
| `server-ip` definido no `server.properties` | É zerado na inicialização; dentro do Docker o servidor precisa escutar em todas as interfaces | |

## Versões do Minecraft

| Situação | Comportamento | |
|---|---|---|
| `MC_VERSION` não existe (`9.9`) | O `make` para: indisponível, rode `make versions` | testado |
| `MC_VERSION` antiga demais (`1.16.5`) | O `make` para: a 1.17 ou mais nova é suportada | testado |
| Uma versão que só tem builds beta do Paper | O `make` para: ainda não há build estável | |
| `latest` passa para uma nova versão do Minecraft | Só acontece quando a imagem é reconstruída. Fixe o `MC_VERSION` para evitar surpresas. | |
| `MC_VERSION` aumentado | Backup do mundo e depois atualização; plugins resolvidos de novo | testado |
| `MC_VERSION` diminuído | Recusa iniciar, com instruções (proteção contra downgrade) | testado |
| Um backup de uma versão mais nova é restaurado | Recusado pela proteção contra downgrade na próxima inicialização | |
| `JAVA_VERSION` antigo demais para a versão do Minecraft | O `make` para informando o Java mínimo necessário | |
| `docker compose up --build` direto com um `MC_VERSION` antigo | O build para com "Build with 'make up'"; o Compose direto usa Java 25 por padrão | |

## Backups e restauração

| Situação | Comportamento | |
|---|---|---|
| Backup manual e programado no mesmo instante | Rodam um depois do outro (trava compartilhada), então o salvamento automático nunca é religado no meio do backup | testado |
| Espaço em disco insuficiente | Backup recusado com os tamanhos na mensagem; o servidor continua rodando com segurança | testado |
| Backup interrompido (contêiner encerrado) | Só sobra um arquivo oculto `.partial`, que o próximo backup remove. Backups concluídos nunca ficam pela metade. | |
| Arquivo corrompido após a gravação | Detectado pelo `gzip -t`, descartado, e os backups antigos são mantidos | |
| Dois backups no mesmo segundo | O segundo ganha um sufixo único em vez de sobrescrever | |
| Servidor parado durante um backup programado | O backup segue sem pausar os salvamentos (nada está gravando) | |
| `make restore` com nome de arquivo digitado errado | Para antes de mexer em qualquer coisa; o servidor continua rodando | testado |
| `make restore` com um arquivo corrompido | Para antes de mexer em qualquer coisa (`gzip -t`) | |
| A extração falha durante a restauração (disco cheio) | Os dados atuais ficam intactos, porque a extração é feita primeiro numa pasta temporária | |
| Restaurar com o servidor, por algum motivo, ainda rodando | Recusado | |

## Plugins

| Situação | Comportamento | |
|---|---|---|
| O site do plugin diz que uma versão suporta seu Minecraft, mas ela precisa de um Java mais novo | Detectado no jar; é usada a versão mais nova que funciona | testado |
| Nenhuma versão funciona neste servidor | Pulado com uma explicação e uma dica sobre `JAVA_VERSION`; uma cópia instalada que não carrega é removida | |
| Uma dependência não está declarada no site, mas está no `plugin.yml` | Procurada pelo nome no Hangar ou no Modrinth, e aceita só se o nome do plugin no jar bater | |
| Uma dependência não é encontrada em lugar nenhum | Aviso com o nome dela; o servidor inicia mesmo assim | |
| O mesmo plugin listado pelo Modrinth e pelo Hangar | O primeiro é mantido e o segundo pulado, para o Paper nunca ver duplicatas | |
| Plugin também instalado manualmente | A cópia gerenciada é pulada | |
| Erro de digitação no nome de um projeto, ou projeto apagado | Aviso; os outros plugins não são afetados | testado |
| Modrinth ou Hangar fora do ar durante a inicialização | Aviso; as versões instaladas continuam; nova tentativa na próxima inicialização | |
| Duas atualizações de plugins ao mesmo tempo | A segunda é recusada com "already running" | testado |
| Contêiner parado enquanto aplicava plugins preparados | A etapa de aplicação pode ser repetida com segurança e termina na próxima inicialização | |
| `plugins.txt` salvo com marca de ordem de bytes ou quebras de linha do Windows | Tratado | |
| `plugins.txt` ausente | Os plugins ficam como estão (nada é desinstalado) | |
| Todas as linhas removidas do `plugins.txt` | Todos os plugins gerenciados são desinstalados; jars instalados manualmente ficam | |

## Jogadores e modos

| Situação | Comportamento | |
|---|---|---|
| Jogador offline entra como `steve` quando `Steve` já existe | Recusado: "already taken, log in as exactly Steve" | testado |
| Nome offline com espaços, hifens, acentos ou pontos | Recusado, com a regra explicada | testado |
| Trocar de online → offline com jogadores existentes | Dados movidos para os IDs offline, após um backup | testado |
| Trocar de offline → online, jogador sem conta | Os dados dele ficam guardados sob o ID offline para quando você voltar | testado |
| Trocar de offline → online com a Mojang inacessível | O servidor não inicia (evita dados divididos); o Docker tenta de novo | |
| O jogador tem dados sob os dois IDs | O arquivo mais novo é mantido; o mais antigo vai para `player-migration-conflicts/` | testado |
| Jogadores do Bedrock via Geyser/Floodgate (`.Name`) | Recusados pelas regras de nome; defina `ENFORCE_USERNAME_RULES=false` | |

## Em execução

| Situação | Comportamento | |
|---|---|---|
| O processo do servidor trava (`kill -9`) | O Docker o reinicia; saudável de novo em segundos | testado |
| O servidor congela mas o processo continua vivo | O watchdog o reinicia após cerca de 5 minutos sem resposta | |
| Primeira inicialização muito lenta (geração do mundo) | O watchdog espera a primeira resposta; a verificação de saúde dá 5 minutos | |
| Java sem memória | Sai de forma limpa e o Docker o reinicia | |
| `/stop` digitado no jogo | Salva e para; o Docker inicia de novo (use `make down` para mantê-lo parado) | |
| Um mundo muito grande não termina de salvar em 2 minutos | Aumente `STOP_TIMEOUT` | |
| A porta 25565 da máquina já está em uso | O `make up` falha com "port is already allocated"; defina `SERVER_PORT` | |
| Reinicialização da máquina | Os contêineres voltam quando o Docker inicia | |

## Limites conhecidos

- Só Minecraft: Java Edition 1.17 e mais novos, no Paper.
- Só Modrinth e Hangar são pesquisados automaticamente para plugins.
- Dados de plugins guardados sob UUIDs de jogadores não são migrados entre modo online e offline.
- `make update-plugins`, `make backup` e `make console` precisam que o contêiner do servidor esteja rodando.
- O `make` precisa que o Docker consiga baixar `eclipse-temurin:25-jdk` (uma vez) para resolver versões.
