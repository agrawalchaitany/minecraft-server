# Lobby network commands. Run them from this network/ folder. S=<server> is proxy, limbo, lobby or a game id.
COMPOSE := docker compose
JDK_IMAGE := eclipse-temurin:25-jdk
TOOL = docker run --rm -v "$(CURDIR)/../tools:/src:ro" $(JDK_IMAGE) java /src/McTool.java
env_value = $$(sed -n 's/^$(1)=//p' .env | tail -n1 | tr -d '\042\047\015 ')
# Turns MC_VERSION (e.g. "latest" or "1.21.4") into an exact version plus the Java it needs, for this shell.
RESOLVE = out=$$($(TOOL) resolve "$(call env_value,MC_VERSION)" "$(call env_value,JAVA_VERSION)") || exit 1; \
	eval "$$out"; export MC_VERSION JAVA_VERSION; echo "==> Minecraft $$MC_VERSION (Paper) on Java $$JAVA_VERSION"

# Every game is a compose profile named after it (see compose.yaml).
GAMES := $(shell $(COMPOSE) config --profiles 2>/dev/null)
ALL := $(foreach g,$(GAMES),--profile $(g))
CORE := proxy limbo lobby lobby-backup
SERVERS := proxy limbo lobby $(GAMES)

need_game = test -n "$(GAME)" && test -n "$(filter $(GAME),$(GAMES))" \
	|| { echo "usage: make $@ GAME=<one of: $(GAMES)>"; exit 1; }
need_server = test -n "$(S)" && test -n "$(filter $(S),$(SERVERS))" \
	|| { echo "usage: make $@ S=<one of: $(SERVERS)>"; exit 1; }
need_paper = test -n "$(S)" && test -n "$(filter $(S),lobby $(GAMES))" \
	|| { echo "usage: make $@ S=<one of: lobby $(GAMES)>"; exit 1; }

.PHONY: help check-env secret up down status games on off logs console cmd restart \
	backup backups restore plugins update-plugins update versions

help:
	@echo "make up | down | status | games"
	@echo "make on GAME=<id> | off GAME=<id>          games: $(GAMES)"
	@echo "make logs [S=<server>] | console S=<server> | cmd S=<server> C='say hi' | restart S=<server>"
	@echo "make backup S=<server> | backups | restore S=<server> FILE=<file>"
	@echo "make plugins S=<server> | update-plugins S=<server> | update | versions"

check-env:
	@test -f .env || { echo "Missing .env - run: cp .env.example .env"; exit 1; }

# The proxy and every server must share one secret; create it the first time.
secret: check-env
	@if ! grep -Eq '^PROXY_SECRET=[A-Za-z0-9_-]{16,}' .env; then \
	  s=$$(openssl rand -hex 24 2>/dev/null || LC_ALL=C tr -dc a-f0-9 </dev/urandom | head -c 48); \
	  if grep -q '^PROXY_SECRET=' .env; then sed -i.bak "s/^PROXY_SECRET=.*/PROXY_SECRET=$$s/" .env && rm -f .env.bak; \
	  else printf '\nPROXY_SECRET=%s\n' "$$s" >> .env; fi; \
	  echo "Generated PROXY_SECRET in .env"; \
	fi

up: secret
	@mkdir -p $(addprefix backups/,lobby $(GAMES))
	@$(RESOLVE); $(COMPOSE) build lobby proxy && \
	  echo "==> Starting proxy, limbo and lobby (the first start downloads plugins and creates worlds: a few minutes)" && \
	  $(COMPOSE) up -d $(CORE) && \
	  echo "==> Ready. Join localhost:$(call env_value,SERVER_PORT). Switch games on with: make on GAME=<id>  ($(GAMES))"

down:
	$(COMPOSE) $(ALL) down

status:
	@$(COMPOSE) $(ALL) ps -a

games:
	@for g in $(GAMES); do \
	  state=$$($(COMPOSE) ps -a --format '{{.State}} {{.Health}}' $$g 2>/dev/null | head -n1); \
	  case "$$state" in \
	    running*unhealthy*) s="ON, not responding (see: make logs S=$$g)" ;; \
	    running*healthy*)   s="ON" ;; \
	    running*)           s="starting..." ;; \
	    *)                  s="off" ;; \
	  esac; \
	  printf '  %-10s %s\n' "$$g" "$$s"; \
	done
	@echo "Switch with: make on GAME=<id> / make off GAME=<id>"

on: check-env
	@$(need_game)
	@mkdir -p backups/$(GAME)
	@echo "==> Switching on $(GAME) (the first start downloads plugins and creates the world: a few minutes)"
	@$(COMPOSE) up -d $(GAME) $(GAME)-backup && echo "==> $(GAME) is on; it shows as Online in the lobby menu"

# Players are sent back to the lobby first, then the world is saved and the container stops.
off:
	@$(need_game)
	@$(COMPOSE) exec -T $(GAME) rcon-cli lobbyall 2>/dev/null && sleep 2 || true
	@$(COMPOSE) stop $(GAME)-backup $(GAME) && echo "==> $(GAME) is off (its world and settings are kept)"

logs:
	$(COMPOSE) $(ALL) logs -f --tail=200 $(S)

console:
	@$(need_server)
	@if [ "$(S)" = proxy ]; then \
	  echo "Velocity console. Detach with Ctrl-P then Ctrl-Q (Ctrl-C stops the proxy)."; $(COMPOSE) attach proxy; \
	else $(COMPOSE) exec $(S) rcon-cli; fi

cmd:
	@$(need_server)
	@if [ "$(S)" = proxy ]; then echo "The proxy has no RCON; use: make console S=proxy"; exit 1; fi
	$(COMPOSE) exec $(S) rcon-cli $(C)

restart:
	@$(need_server)
	$(COMPOSE) restart $(S)

backup:
	@$(need_paper)
	$(COMPOSE) exec $(S) entrypoint backup-now

backups:
	@ls -lh backups/*/

restore:
	@$(need_paper)
	@test -n "$(FILE)" || { echo "usage: make restore S=<server> FILE=<name from 'make backups'>"; exit 1; }
	@test -f "backups/$(S)/$(notdir $(FILE))" || { echo "backups/$(S)/$(notdir $(FILE)) not found; nothing was changed. See: make backups"; exit 1; }
	@gzip -t "backups/$(S)/$(notdir $(FILE))" || { echo "backups/$(S)/$(notdir $(FILE)) is corrupt; nothing was changed"; exit 1; }
	@$(RESOLVE); $(COMPOSE) stop $(S)-backup $(S) && \
	  $(COMPOSE) run --rm --no-deps $(S) restore $(notdir $(FILE)) && \
	  $(COMPOSE) up -d $(S) $(S)-backup

plugins:
	@$(need_server)
	$(COMPOSE) exec $(S) plugins status

update-plugins:
	@$(need_server)
	@rc=0; $(COMPOSE) exec $(S) entrypoint plugins update || rc=$$?; \
	if [ $$rc -eq 0 ]; then \
	  if [ -n "$(filter $(S),lobby $(GAMES))" ]; then \
	    $(COMPOSE) exec $(S) entrypoint backup-now && \
	    $(COMPOSE) exec $(S) rcon-cli "say Server restarting to update plugins" || exit 1; \
	  fi; \
	  $(COMPOSE) restart $(S) && echo "Restarted $(S). Check the result with: make plugins S=$(S)"; \
	elif [ $$rc -ne 3 ]; then exit $$rc; fi

# Rebuild with the newest Paper/Velocity for your versions, then restart what is running.
update: secret
	@$(RESOLVE); export CACHE_BUST=$$(date +%s); \
	running=$$($(COMPOSE) $(ALL) ps --status running --services); \
	$(COMPOSE) build lobby proxy && $(COMPOSE) up -d $(CORE) || exit 1; \
	for g in $(GAMES); do \
	  if echo "$$running" | grep -qx "$$g"; then $(COMPOSE) up -d $$g $$g-backup || exit 1; fi; \
	done

versions:
	@$(TOOL) versions
