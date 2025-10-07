SHELL := /bin/bash

.PHONY: start stop restart status logs purge test coverage help

# Default target
start: ## Start all services (builds app). Honors OWL_SECURITY_ENABLED, ISSUER_URI
	@bash scripts/owl.sh start

stop: ## Stop all services (preserves volumes)
	@bash scripts/owl.sh stop

restart: ## Restart all services
	@bash scripts/owl.sh restart

status: ## Show compose status
	@bash scripts/owl.sh status

logs: ## Tail app logs
	@bash scripts/owl.sh logs

purge: ## Stop and remove volumes (DANGER)
	@bash scripts/owl.sh purge

test: ## Run unit tests with coverage
	@mvn -q test

coverage: ## Generate JaCoCo coverage report (opens index if on macOS)
	@mvn -q verify
	@echo "Coverage report: target/site/jacoco/index.html"
	@([ "$(shell uname)" = "Darwin" ] && open target/site/jacoco/index.html) || true

# Microservices
.PHONY: build-retrieval run-retrieval build-safety run-safety build-modelproxy run-modelproxy

build-retrieval: ## Build retrieval-service Docker image
	@docker build -t owl-retrieval-service ./services/retrieval-service

run-retrieval: ## Run retrieval-service via Compose
	@docker compose up -d retrieval-service

build-safety: ## Build safety-service Docker image
	@docker build -t owl-safety-service ./services/safety-service

run-safety: ## Run safety-service via Compose
	@docker compose up -d safety-service

build-modelproxy: ## Build model-proxy Docker image
	@docker build -t owl-model-proxy ./services/model-proxy

run-modelproxy: ## Run model-proxy via Compose
	@docker compose up -d model-proxy

.PHONY: scale-retrieval
scale-retrieval: ## Scale retrieval-service replicas (Compose)
	@docker compose up -d --scale retrieval-service=2

.PHONY: build-kong-oidc
build-kong-oidc: ## Build custom Kong image with community OIDC plugin
	@docker build -t kong-oidc-custom ./deploy/gateway/kong-oidc

help: ## Show this help
	@echo "Available targets:"
	@grep -E '^[a-zA-Z_-]+:.*?## ' $(MAKEFILE_LIST) | awk 'BEGIN {FS=":.*?## "}; {printf "\033[36m%-12s\033[0m %s\n", $$1, $$2}'
	@echo
	@echo "Env (optional): OWL_SECURITY_ENABLED=true|false, ISSUER_URI=http://keycloak:8080/realms/owl-dev"
