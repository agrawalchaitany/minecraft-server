# Lobby network

One address for players, a lobby with a game menu, and game servers you switch on and off.

```bash
cp .env.example .env       # set EULA=true
make up                    # proxy + login room + lobby
make on GAME=survival      # switch a game on; make off GAME=survival to switch it off
make games                 # what is on
```

Full guide: [docs/network.md](../docs/network.md).

| Path | |
|---|---|
| `games.yml` | Games in the lobby menu (also registered on the proxy) |
| `compose.yaml` | One service per server; each game is a profile you switch on and off |
| `servers/<id>/plugins.txt` | Plugins per server |
| `proxy/` | The Velocity proxy image and its `plugins.txt` (login plugin) |
| `backups/<id>/` | Backups per server |
