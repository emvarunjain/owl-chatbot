# OWL — API Overview

This document summarizes the public APIs and links to OpenAPI specs.

## Monolith (v1/v2)
- Base: /api/v1 (aliases /api), /api/v2
- Tenants: POST/GET/PUT/DELETE /api/v1/tenants, list with pagination
- Ingestion: POST /api/v1/ingest/file|url|html|sitemap
- Chat: POST /api/v1/chat { tenantId, question, allowWeb, document?, fallback? }
- Admin v1: /api/v1/admin search/sources/purge/recrawl/metrics/cluster-sample/tokens/settings/cost/usage/budget
- Admin v2: /api/v2/admin plans, connectors (list/create/sync/delete), routing, eval, credentials
- OpenAPI (static):
  - src/main/resources/static/openapi-v1.1.yaml (version 2.0.0)
  - src/main/resources/static/openapi-v2.0.yaml (alias)

## Retrieval Service
- /v1/search { tenantId, q, document?, topK } → { docs: [{ text, metadata }] }
- /v1/add { tenantId, docs: [{ text, metadata }] } → { added }
- OpenAPI: services/retrieval-service/src/main/resources/static/openapi.yaml

## Safety Service
- /v1/safety/classify { text } → { outcome: SAFE|REFUSE|REVIEW }
- OpenAPI: services/safety-service/src/main/resources/static/openapi.yaml

## Model Proxy
- /v1/chat { tenantId, provider, model, system, user } → { answer }
- Providers: ollama|openai|azure|bedrock (based on env)
- OpenAPI: services/model-proxy/src/main/resources/static/openapi.yaml

## Auth
- JWT: Authorization: Bearer <token>; tenant claim must match tenantId
- API Keys: X-API-Key or Authorization: ApiKey <token> (scoped)

## Regions
- Optional: X-Data-Region header selects per-request region (e.g., us-east-1)

## Errors
- 400: validation
- 401/403: authz
- 402: budget exceeded
- 429: rate limit
- 503: fallback search unavailable
