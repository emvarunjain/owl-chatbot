# OWL — Architecture and Functionality

This document describes the overall architecture, major components, data flow and deployment model of OWL.

## Goals
- Multi-tenant, KB-grounded chatbot with strict grounding and citations
- Easy tenant onboarding, per-tenant isolation (DB + vector), and security guardrails
- Local developer ergonomics and production-ready microservices
- Observability, budgets/quotas, and cost controls

## High-Level Architecture

- API Gateway (Kong) routes incoming HTTP to the monolith (owl-app) and to extracted microservices.
- Monolith (owl-app) provides API v1/v2, control-plane, and chat orchestration. It can call microservices or run in local mode.
- Microservices (extracted):
  - Retrieval Service: vector add/search over Qdrant; embedding via Spring AI
  - Safety Service: model-in-the-loop safety classification (Llama Guard/NeMo-style)
  - Model Proxy: provider routing (Ollama/OpenAI/Azure/Bedrock)
- Data plane:
  - MongoDB: shared “core” DB for tenants/config; per-tenant DB for chat/PII
  - Qdrant: single collection (tenantId payload) or collection-per-tenant mode; optional per-region routing
- Events:
  - Redpanda/Kafka for `owl.events.*` (chat, cost, ingest, feedback, audit) to support data-lake and analytics

## Request Flow
1) Client → Gateway → /api/v1 or /api/v2 (monolith)
2) Monolith validates request, applies rate limiting and quota checks
3) Safety pre-check (model-in-the-loop via Safety Service or local safety client)
4) Prompt cache or preference memory short-circuits when possible
5) Retrieval via Retrieval Service or local vector store
6) Chat LLM call via Model Proxy or local ChatClient; apply citations and safety post-check
7) Persist chat history (per-tenant DB), emit events, record cost

## Multi-Region
- X-Data-Region header or tenant setting selects region
- Region-aware DB names and Qdrant collections; optional per-region Mongo/Qdrant URIs
- Kong and umbrella Helm charts deploy a regional stack; global control-plane replicates tenants/plans if needed

## Security & Governance
- OIDC/JWT or API keys with scopes; optional SAML for UI
- Guardrails (SAFE/REFUSE/REVIEW) with policy; DLP redaction on ingest
- Per-tenant encryption keys for chat history; audit logs via events

## Cost & Reliability
- Semantic + prompt cache; budget guardrails per tenant
- Quotas/burst credits by plan; SLAs/SLOs and Prometheus alert rules
- Horizontal autoscale per service (HPA); bulkheads for safety, model proxy, ingestion

## Observability
- Micrometer/Actuator endpoints; Prometheus + Grafana dashboards (latency, cache hit ratio, cost rate, budgets)
- Recording rules + SLO alerts; per-tenant drill-down

## Microservices at a Glance
- Retrieval Service
  - /v1/search, /v1/add
  - Qdrant client + embeddings
- Safety Service
  - /v1/safety/classify → SAFE|REFUSE|REVIEW
  - Can run an Ollama-based safety model
- Model Proxy
  - /v1/chat { provider, model, system, user }
  - Supports openai/azure/bedrock (env keys)

## Deployment Model
- Local: Docker Compose (owl-app + microservices + Mongo + Qdrant + Ollama)
- Kubernetes: Helm umbrella chart for services; Helm chart for Kong (DB-less)

See docs/API.md for endpoint details and scripts/deploy.sh for single-command deployment.
