<!-- language-switcher -->
[English](../deployment.md) · [हिन्दी](../hi/deployment.md) · [Español](../es/deployment.md) · [简体中文](../zh-CN/deployment.md) · **Português (BR)** · [日本語](../ja/deployment.md) · [한국어](../ko/deployment.md)

# <img src="../assets/icons/deployment.svg" width="28" height="28" alt=""> Implantação

O mesmo projeto roda no macOS (para testar ou jogar com amigos) e em servidores Linux (para algo sempre ligado). As imagens são construídas nativamente para Intel/AMD (`amd64`) e ARM (`arm64`, incluindo Apple Silicon).

## Servidor Linux (VPS, servidor em casa)

### 1. Instale o Docker

Instale o Docker Engine e o plugin Compose seguindo as instruções do Docker para a sua distribuição, depois:

```bash
sudo systemctl enable --now docker      # start Docker on boot
sudo usermod -aG docker "$USER"          # run docker without sudo (log out and in again)
```

### 2. Copie o projeto

```bash
git clone <your-repository> minecraft && cd minecraft
# or: scp -r Minecraft-Server/ user@server:~/minecraft
```

### 3. Configure

```bash
cp .env.example .env
id -u && id -g          # put these into PUID / PGID
```

No `.env`, defina pelo menos `EULA`, `MC_VERSION` (fixe uma versão exata), `MEMORY` e `CONTAINER_MEMORY`.

| RAM do servidor | `MEMORY` | `CONTAINER_MEMORY` |
|---|---|---|
| 4 GB | `2G` | `3G` |
| 8 GB | `5G` | `6G` |
| 16 GB | `10G` | `12G` |

Deixe cerca de 1 GB para o sistema operacional.

### 4. Abra a porta do jogo

```bash
sudo ufw allow 25565/tcp
```

Libere também TCP 25565 no firewall ou security group do seu provedor de nuvem, se houver. **Nunca abra a 25575** (RCON); ela é só interna.

### 5. Inicie

```bash
make up
```

Os contêineres voltam sozinhos após reinicializações.

## macOS

Instale o [Docker Desktop](https://www.docker.com/products/docker-desktop/) e siga os [Primeiros passos](getting-started.md). Bom saber:

- O Docker Desktop tem seu próprio limite de memória (**Settings → Resources**). Ele precisa ser maior que `CONTAINER_MEMORY`.
- Amigos na sua rede de casa se conectam ao IP local do seu Mac. Para amigos de fora, redirecione a porta TCP 25565 no roteador para o seu Mac.
- O Docker Desktop precisa estar aberto para o servidor rodar. Ative "Start Docker Desktop when you sign in" para ele voltar após reiniciar.

## Backups externos

O `./backups` fica no mesmo disco que o servidor. Copie-o para outro lugar periodicamente, por exemplo com `rsync` via cron no servidor:

```bash
# crontab -e   (every day at 05:00)
0 5 * * * rsync -a --delete /home/me/minecraft/backups/ backup-host:/srv/minecraft-backups/
```

Qualquer ferramenta serve (`rclone` para armazenamento na nuvem, `restic` etc.). Copie apenas os arquivos `mc-*.tar.gz` finalizados; os arquivos ocultos `.partial` são backups em andamento.

## Migrar para outra máquina

1. `make backup` na máquina antiga.
2. Instale o projeto na máquina nova com o mesmo `.env` e `plugins/plugins.txt`.
3. Copie o backup para o novo `./backups/`.
4. `make up` e depois `make restore FILE=…`. Veja [Backups e restauração](backups.md#restaurar-numa-máquina-nova).
5. Aponte os jogadores (ou seu nome DNS) para o novo endereço.

## Atualizar o próprio projeto

Substitua os arquivos do projeto pela nova versão, mas mantenha `.env`, `plugins/plugins.txt` e `backups/`. Depois:

```bash
make up
```

Seu mundo fica no volume do Docker e não é afetado pela troca de arquivos.

## Checklist de segurança

- Mantenha `ONLINE_MODE=true`, a menos que precise do modo offline, e se precisar, leia [os riscos](offline-mode.md#o-risco-nomes-de-usuário-não-são-verificados).
- Deixe `RCON_PASSWORD` vazio para ter um gerado, ou use um valor longo e aleatório. Nunca publique a porta 25575.
- Mantenha o `.env` privado; ele contém a senha do RCON. Ele fica fora do git e do build do Docker.
- Mantenha o sistema atualizado e use chaves SSH em vez de senhas.
- Dê operador só a quem precisa; `make cmd C="deop Name"` remove.
- Fixe `MC_VERSION` e revise as atualizações de plugins antes de aplicá-las.
