COMPOSE := docker compose
JDK_IMAGE := eclipse-temurin:25-jdk
TOOL = docker run --rm -v "$(CURDIR)/tools:/src:ro" $(JDK_IMAGE) java /src/McTool.java
env_value = $$(sed -n 's/^$(1)=//p' .env | tail -n1 | tr -d '\042\047\015 ')
# Turns MC_VERSION (e.g. "latest" or "1.21.4") into an exact version plus the Java it needs, for this shell.
RESOLVE = out=$$($(TOOL) resolve "$(call env_value,MC_VERSION)" "$(call env_value,JAVA_VERSION)") || exit 1; \
	eval "$$out"; export MC_VERSION JAVA_VERSION; echo "==> Minecraft $$MC_VERSION (Paper) on Java $$JAVA_VERSION"

.PHONY: help up down restart logs status console cmd backup backups restore update versions plugins update-plugins

help:
	@echo "make up | down | restart | logs | status | console | cmd C='say hi'"
	@echo "make backup | backups | restore FILE=mc-YYYYMMDD-HHMMSS.tar.gz | update | versions"
	@echo "make plugins | update-plugins"

up:
	@test -f .env || (echo "Missing .env - run: cp .env.example .env" && exit 1)
	@mkdir -p backups
	@$(RESOLVE); $(COMPOSE) up -d --build

down:
	$(COMPOSE) down

restart:
	$(COMPOSE) restart mc

logs:
	$(COMPOSE) logs -f --tail=200

status:
	$(COMPOSE) ps

console:
	$(COMPOSE) exec mc rcon-cli

cmd:
	$(COMPOSE) exec mc rcon-cli $(C)

backup:
	$(COMPOSE) exec mc entrypoint backup-now

backups:
	@ls -lh backups/

restore:
	@test -n "$(FILE)" || (echo "usage: make restore FILE=<name from 'make backups'>" && exit 1)
	@test -f "backups/$(notdir $(FILE))" || (echo "backups/$(notdir $(FILE)) not found; the server was left running. See: make backups" && exit 1)
	@gzip -t "backups/$(notdir $(FILE))" || (echo "backups/$(notdir $(FILE)) is corrupt; the server was left running" && exit 1)
	@$(RESOLVE); $(COMPOSE) stop backup mc && \
	  $(COMPOSE) run --rm --no-deps mc restore $(FILE) && \
	  $(COMPOSE) up -d

update:
	@$(RESOLVE); $(COMPOSE) build --build-arg CACHE_BUST=$$(date +%s) mc && $(COMPOSE) up -d

versions:
	@$(TOOL) versions

plugins:
	$(COMPOSE) exec mc plugins status

update-plugins:
	@rc=0; $(COMPOSE) exec mc entrypoint plugins update || rc=$$?; \
	if [ $$rc -eq 0 ]; then \
	  $(COMPOSE) exec mc entrypoint backup-now && \
	  $(COMPOSE) exec mc rcon-cli "say Server restarting to update plugins" && \
	  $(COMPOSE) restart mc && echo "Restarted. Check the result with: make plugins"; \
	elif [ $$rc -ne 3 ]; then exit $$rc; fi
