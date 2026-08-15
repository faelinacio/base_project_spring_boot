COMPOSE := docker compose

.PHONY: help up down migrate reset logs dev

help: ## Show this help
	@grep -hE '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-10s\033[0m %s\n", $$1, $$2}'

up: ## Start the local Postgres container
	$(COMPOSE) up -d postgres

migrate: ## Apply Flyway migrations against the local Postgres
	$(COMPOSE) run --rm flyway

down: ## Stop the local Postgres container
	$(COMPOSE) down

reset: ## Stop Postgres and wipe its data volume
	$(COMPOSE) down -v

logs: ## Tail Postgres logs
	$(COMPOSE) logs -f postgres

dev: up migrate ## Start Postgres and apply migrations — everything needed to run the app locally
	@echo ""
	@echo "Database ready on localhost:5432. Now, from the project root, run:"
	@echo "  SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run"
