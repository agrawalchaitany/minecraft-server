<!-- language-switcher -->
[English](../troubleshooting.md) · [हिन्दी](../hi/troubleshooting.md) · [Español](../es/troubleshooting.md) · [简体中文](../zh-CN/troubleshooting.md) · **Português (BR)** · [日本語](../ja/troubleshooting.md) · [한국어](../ko/troubleshooting.md)

# <img src="../assets/icons/troubleshooting.svg" width="28" height="28" alt=""> Solução de problemas

Comece com:

```bash
make status          # is it running and healthy?
make logs            # what did it say?
```

A maioria dos problemas gera uma linha `ERROR:` clara nos logs, indicando a configuração ou a causa. As mensagens abaixo estão em inglês, do jeito que aparecem nos logs.

## O servidor não inicia

**"You must accept the Minecraft EULA"**
Defina `EULA=TRUE` no `.env` e rode `make up`.

**"Set RCON_PASSWORD in .env to your own random value"** ou **"RCON_PASSWORD must not contain backslashes…"**
Use `openssl rand -hex 24` e coloque o resultado no `.env`.

**"MEMORY … does not fit in the container limit"**
Aumente `CONTAINER_MEMORY` para o valor sugerido na mensagem ou diminua `MEMORY`. No macOS, confira também a memória do Docker Desktop em **Settings → Resources**.

**"This world was last run on Minecraft X, but the server is now Y"**
Você diminuiu o `MC_VERSION`. Volte ao valor anterior ou restaure um backup feito na versão mais antiga. Veja [Versões do Minecraft](minecraft-versions.md#fazer-downgrade).

**"Player data migration failed; not starting…"**
Normalmente a API da Mojang não pôde ser acessada ao mudar para o modo online. Confira a conexão do servidor com a internet; o Docker tenta de novo automaticamente. Veja [Modo online e offline](offline-mode.md#dados-dos-jogadores-entre-modos).

**Fica reiniciando com código de saída 137**
O contêiner atingiu o limite de memória. Aumente `CONTAINER_MEMORY` (pelo menos `MEMORY` + 1G) ou diminua `MEMORY`.

**"port is already allocated"**
Outra coisa está usando a porta 25565 na máquina. Pare-a, ou defina `SERVER_PORT=25566` e conecte nessa porta.

## Problemas de build

**"Minecraft X needs Java N but the image is being built with Java M"**
Você construiu direto com `docker compose`. Use `make up`, que escolhe o Java automaticamente. Se você definiu `JAVA_VERSION` por conta própria, remova.

**"Minecraft X is not available on Paper" / "is too old"**
Escolha uma versão de `make versions` (1.17 ou mais nova).

**"Patching Paper failed (attempt N), retrying"**
Um download da Mojang falhou durante o build; ele tenta de novo automaticamente. Se todas as tentativas falharem, confira sua conexão com a internet e rode `make up` de novo.

## Jogadores não conseguem conectar

1. `make status` deve mostrar `(healthy)`.
2. Servidor na mesma máquina: conecte em `localhost`.
3. Servidor na nuvem: libere TCP 25565 no firewall da máquina (`sudo ufw allow 25565/tcp`) **e** no firewall ou security group do provedor.
4. Rede de casa: redirecione TCP 25565 no roteador para a máquina.
5. Jogadores sem conta oficial: defina `ONLINE_MODE=false`. Veja [Modo online e offline](offline-mode.md).
6. "Invalid username" ou "already taken" no modo offline: veja as [regras de nome de usuário](offline-mode.md#regras-de-nome-de-usuário).

## Unhealthy

Na primeira inicialização, gerar o mundo pode levar alguns minutos; a verificação de saúde dá cinco. Se continuar unhealthy, procure erros em `make logs`. Um servidor travado é reiniciado automaticamente pelo [watchdog](reliability.md#watchdog-contra-travamentos).

## Plugins

**Um plugin não carregou**
Procure linhas `[plugins] WARNING` em `make logs`. Elas explicam o motivo: nenhuma versão compatível, dependência faltando, precisa de um Java mais novo etc. `make plugins` mostra o que está instalado.

**"needs Java N (this server runs Java M)"**
Nenhuma versão desse plugin funciona ao mesmo tempo com sua versão do Minecraft e o Java dela. Use um `MC_VERSION` mais novo ou defina `JAVA_VERSION=N` se sua versão do Minecraft roda nele.

**"requires the plugin X, which could not be found automatically"**
Encontre X no Modrinth ou no Hangar e adicione ao `plugins.txt`, ou instale [manualmente](plugins.md#plugins-de-outros-sites).

**"another plugin install/update is already running"**
Há uma atualização programada em andamento. Tente de novo em um minuto.

## Backups

**"not enough disk space"**
Libere espaço na máquina ou diminua `BACKUP_KEEP`. Os backups se recusam a encher o disco, porque um disco cheio pode impedir o servidor de salvar o mundo.

**"another backup has been running for 15 minutes"**
Um backup anterior travou, geralmente por causa de um disco muito lento. Confira `docker compose logs backup`.

**`./backups` permission denied (Linux)**
Defina `PUID` e `PGID` no `.env` com os valores de `id -u` / `id -g` e rode `make up`.

## As configurações parecem não ter efeito

O `make restart` mantém as configurações antigas; rode `make up` depois de editar o `.env`. As configurações de jogo do `.env` sobrescrevem o `server.properties` a cada inicialização, então deixe a configuração vazia no `.env` se quiser gerenciá-la no `server.properties`.
