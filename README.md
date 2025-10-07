# 🦉 OWL — Multi‑tenant KB‑Grounded Chatbot

Delivers a multi‑tenant chatbot that answers strictly from an ingested knowledge base, with one‑call tenant onboarding and local/dev ergonomics.

## Tech

- Java 21 + Spring Boot 3.3
- Spring AI 1.0.1 (Ollama + Qdrant starter), TokenTextSplitter
- Qdrant vector DB (single collection; `tenantId` in payload filter)
- MongoDB: database‑per‑tenant for chats/PII; shared core DB for tenants/config
- Tika/Jsoup for PDF/DOCX/TXT/URL ingestion
- Basic semantic cache in Qdrant
- Simple citations
- Micrometer/Actuator for metrics + logs
- Docker Compose for local (Apple Silicon friendly)
- Optional Redpanda (Kafka API) for events

## Environment Variables

- `MONGO_URI` core DB URI (default `mongodb://localhost:27017/owl`)
- `SPRING_KAFKA_BOOTSTRAP_SERVERS` or `KAFKA_BOOTSTRAP_SERVERS` (optional; enables events)
- `OLLAMA_BASE_URL` Ollama URL (default `http://localhost:11434`)
- `OLLAMA_CHAT_MODEL` chat model id (default `llama3.1:8b-instruct`)
- `OLLAMA_EMBED_MODEL` embed model id (default `nomic-embed-text`)
- `QDRANT_HOST` (default `localhost`), `QDRANT_PORT` (default `6334` gRPC)
- `QDRANT_COLLECTION` (default `owl_kb`)
- `QDRANT_VECTOR_SIZE` optional override for vector dimension
- `SPRING_PROFILES_ACTIVE` set `docker` under Compose

Notes
- Per‑tenant DB name pattern: `owl_tenant_{tenantId}`. Core collections (e.g. `tenants`) stay in the `MONGO_URI` database.
- Actuator exposes `health, info, metrics, prometheus, loggers`.

## Local Dev (Docker Compose)

```bash
docker compose up -d --build
```

Services
- MongoDB: `localhost:27017`
- Qdrant: REST `:6333`, gRPC `:6334`
- Ollama: `:11434`
- Redpanda (optional): Kafka `:9092`
- App: `http://localhost:8080`

Apple Silicon: Images are pinned to ARM64‑compatible variants.

## Build + Run (local JDK)

```bash
mvn -q -DskipTests package
java -jar target/owl-0.0.1-SNAPSHOT.jar
```

## One-command start/stop

- Script: `scripts/owl.sh`

Examples
```bash
# Start (open endpoints at http://localhost:8080)
bash scripts/owl.sh start

# Start with JWT security enabled via Keycloak dev realm
OWL_SECURITY_ENABLED=true ISSUER_URI=http://keycloak:8080/realms/owl-dev \
  bash scripts/owl.sh start

# Stop or purge volumes
bash scripts/owl.sh stop
bash scripts/owl.sh purge   # WARNING: removes Mongo/Qdrant/Keycloak volumes

# Status / Logs
bash scripts/owl.sh status
bash scripts/owl.sh logs
```

Makefile targets
```bash
# Start/Stop/Status/Logs/Purge
make start
make stop
make status
make logs
make purge   # WARNING: removes Mongo/Qdrant/Keycloak volumes

# Start with JWT security enabled via Keycloak dev realm
make start OWL_SECURITY_ENABLED=true ISSUER_URI=http://keycloak:8080/realms/owl-dev
```

## Behavior Highlights

- Strict KB‑only answers by default. If no relevant context ≥ threshold, replies “I don't know based on the provided knowledge.”
- Reranking: CPU‑only token‑overlap reranker improves ordering; drop‑in cross‑encoder BGE support can replace it later (`owl.rerank.enabled=true`).
- Citations returned in response (`sources`) and appended for readability.
- Semantic cache: shared across channels via Qdrant (`type=cache`).
- Multi‑tenant isolation via payload filter + per‑tenant DBs.
- HTML normalization and sitemap crawling endpoints for higher‑quality ingestion.
- De‑duplication on ingest (per‑tenant) avoids repetitive answers.
- One‑call tenant provisioning; Admin endpoints for search/purge/recrawl.
- Metrics via Micrometer; hit‑ratio tracked per tenant; model latency timer.
- Governance: optional JWT (Keycloak) with tenant scopes, simple rate limiting, audit logs via Redpanda.

## API v1

Base path: `/api/v1` (legacy aliases under `/api` also work)

### Tenants

- Create: `POST /api/v1/tenants`
  - Request: `{ "tenantId": "acme", "name": "Acme Inc" }`
  - Response: `200 OK` tenant object

- Get: `GET /api/v1/tenants/{tenantId}`

- Update: `PUT /api/v1/tenants/{tenantId}`
  - Request: `{ "name": "New Name" }`

- Delete: `DELETE /api/v1/tenants/{tenantId}`

- List: `GET /api/v1/tenants?page=0&size=20`

Example
```bash
curl -sS -X POST http://localhost:8080/api/v1/tenants \
  -H 'Content-Type: application/json' \
  -d '{"tenantId":"acme","name":"Acme Inc"}'
```

### Ingestion

- File: `POST /api/v1/ingest/file?tenantId={id}`
  - Multipart: `file=@/path/to/file.(pdf|docx|txt)`
  - Response: `Ingested chunks: <N>`

- URL: `POST /api/v1/ingest/url?tenantId={id}&url={http...}`
  - Response: `Ingested chunks: <N>`

Examples
```bash
curl -sS -X POST "http://localhost:8080/api/v1/ingest/file?tenantId=acme" \
  -F "file=@/path/to/your.pdf"

curl -sS -X POST "http://localhost:8080/api/v1/ingest/url?tenantId=acme&url=https://example.com/policy"
```

### Chat

- `POST /api/v1/chat`
  - Request: `{ "tenantId": "acme", "question": "What is our refund policy?", "allowWeb": false, "document": "optional filename or url scope" }`
  - Response: `{ "answer": "...", "sources": ["policy.pdf"] }`

Examples
```bash
curl -sS -X POST http://localhost:8080/api/v1/chat \
  -H 'Content-Type: application/json' \
  -d '{"tenantId":"acme","question":"What is our refund policy?","allowWeb":false}'
```

Notes
- `allowWeb` is accepted but retrieval is KB‑only by default; web retrieval is not enabled in this build.
- `document` scopes retrieval to a filename or URL if provided.

## API v1.1 (Admin + Ingestion Enhancements)

Base path: `/api/v1` (aliases under `/api`)

- Admin Search: `GET /api/v1/admin/search?tenantId={id}&q={query}&k=10`
- List Sources: `GET /api/v1/admin/sources?tenantId={id}&sample=200`
- Purge by Source: `DELETE /api/v1/admin/purge?tenantId={id}&source={filename|url}&includeCache=false`
- Re-crawl URL: `POST /api/v1/admin/recrawl?tenantId={id}&url={url}&sitemap=false&max=50`
- Cluster Sample: `GET /api/v1/admin/cluster-sample?tenantId={id}&sample=200` (rough simhash-based grouping)
- Ingest HTML (normalized): `POST /api/v1/ingest/html?tenantId={id}&url={url}`
- Ingest Sitemap: `POST /api/v1/ingest/sitemap?tenantId={id}&url={sitemapUrl}&max=50`

Auth
- Optional JWT (Keycloak): set `owl.security.enabled=true` and configure standard Spring `spring.security.oauth2.resourceserver.jwt.*`.
- Claims: expects `tenant` or `tenant_id` claim. API checks that claim matches `tenantId` in requests.
- Header: `Authorization: Bearer <token>`
- Without JWT, all endpoints are open except Admin which still requires auth if `owl.security.enabled=true`.

Rate Limiting
- Per‑tenant QPM via `owl.rateLimit.qpm` (default 120). Returns `429` when exceeded. Excludes `/actuator`.

Audit Logs
- Admin actions emit events to topic `owl.events.audit` when Kafka is configured.

Observability
- Per‑tenant counters and hit‑ratio (`/api/v1/admin/metrics?tenantId=...`).
- Micrometer timers for model latency: `chat.model.time{tenantId}`.
 - SLO alerts provided via Prometheus rules: see `deploy/observability/rules/slo-alerts.yml`.

## SDKs

### Web Widget

Add to your page and mount:

```html
<script src="/sdk/web/owl-widget.js"></script>
<script>
  window.OWL_TENANT_ID = 'acme';
  OwlWidget.mount('');
  // Optionally: OwlWidget.mount('https://your.host');
</script>
```

### Android / iOS

- Android: see `sdk/android/README.md`
- iOS: see `sdk/ios/README.md`

## OpenAPI

- Swagger UI: `/swagger-ui.html`
- JSON: `/v3/api-docs`
- Static YAML: `/openapi-v1.1.yaml`

## Keycloak Dev Realm (Quick Start)

Compose includes an optional Keycloak service with a preloaded realm `owl-dev` and a test user.

Enable security and issuer in Compose:

```bash
export OWL_SECURITY_ENABLED=true
export ISSUER_URI=http://keycloak:8080/realms/owl-dev
docker compose up -d --build
```

Keycloak Admin:
- URL: `http://localhost:8081/admin`
- User: `admin` / `admin`

Realm `owl-dev`:
- Client `owl-api` (bearer-only)
- Client `owl-web` (public, direct grants enabled)
- User `alice` / `alice` with user attribute `tenant=acme`

Getting a token (password grant, dev only):

```bash
curl -sS -X POST \
  -d 'client_id=owl-web' \
  -d 'grant_type=password' \
  -d 'username=alice' \
  -d 'password=alice' \
  http://localhost:8081/realms/owl-dev/protocol/openid-connect/token | jq -r .access_token
```

Use it with APIs:

```bash
TOKEN=$(# obtain as above)
curl -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"tenantId":"acme","question":"Hello","allowWeb":false}' \
  http://localhost:8080/api/v1/chat
```

## API v1.2 (Feedback, Usage/Cost, Ops)

New endpoints:
- Submit Feedback: `POST /api/v1/feedback`
  - Request: `{ "tenantId": "acme", "chatId": "...", "rating": 1..5, "helpful": true|false, "comment": "..." }`
  - On positive feedback (>=4 or helpful), the answer is saved to a preference memory and will be preferred for similar future questions.

- Admin Usage: `GET /api/v1/admin/usage?tenantId={id}` (per‑tenant hit/miss ratios)
- Admin Cost: `GET /api/v1/admin/cost?tenantId={id}` (budget snapshot)
- Admin Budget: `POST /api/v1/admin/budget` body `{ tenantId, monthlyBudgetUsd }`

Cost guardrails:
- Properties: `owl.cost.estimatePerCallUsd`, `owl.cost.budget.enabled`, `owl.cost.budget.defaultUsd`
- When enabled, budget is enforced per month; exceeding requests get a budget message response.

Stronger isolation (optional):
- Set `owl.isolation.collectionPerTenant=true` to create a Qdrant collection per tenant during provisioning. Current runtime uses single-collection mode with tenant filters; collection-per-tenant is provided as an option for stricter isolation.

Security & DLP:
- DLP redaction on ingest and responses: `owl.dlp.enabled=true` replaces common PII with redaction tokens.
- Per‑tenant encryption keys (scaffold): generate and manage per‑tenant keys; see runbooks.

Analytics streams:
- Events published to Kafka/Redpanda: `owl.events.chat`, `owl.events.feedback`, `owl.events.cost`, `owl.events.ingest`, `owl.events.audit`.
- Use Kafka Connect sinks (e.g., S3/GCS) and build dashboards with Grafana/Metabase.

## Testing & Coverage

Run tests and generate coverage via JaCoCo:

```bash
mvn -q test           # run unit tests
mvn -q verify         # run tests and build coverage report
# macOS quick open:
open target/site/jacoco/index.html
```

Makefile shortcuts:

```bash
make test
make coverage
```

## Kubernetes (Production Runbook)

Manifests in `deploy/k8s/` include Deployment, Service, HPA, and PDB with zone spread constraints.

- Health probes: enabled at `/actuator/health/liveness` and `/actuator/health/readiness` (see `management.endpoint.health.probes.enabled`)
- HPA: scales by CPU util; adjust thresholds as needed
- PDB: prevents full eviction
- Multi‑AZ: topology spread across zones

Secrets/Config:
- Provide `MONGO_URI`, Qdrant host/port, and Ollama (or managed LLM) via env/Secrets

Backups/DR:
- Mongo backup: `bash scripts/backup_mongo.sh` (uses `MONGO_URI`)
- Qdrant snapshots: `bash scripts/backup_qdrant.sh` (ensure snapshots enabled)
- Regular DR drills: restore snapshots to a staging cluster; validate ingestion and chat readiness

## Observability Stack (Optional)

Compose includes Prometheus and Grafana for local dashboards:

- Prometheus: http://localhost:9090 (scrapes `/actuator/prometheus`)
- Grafana: http://localhost:3000 (admin/admin)
- Dashboard auto-provisioned: "Owl — Chat Ops" with cache hit ratio, request rates, latency per tenant, cost rate, and budget vs. monthly spend.

Configs:
- Prometheus config: `deploy/observability/prometheus.yml`
- Grafana provisioning: `deploy/observability/grafana/provisioning` and dashboard JSON `deploy/observability/grafana/dashboard-owl.json`

## Data Lake via Kafka Connect (Examples)

Examples for S3 and GCS sink connectors to ship `owl.events.*` topics to object storage.

- S3: `deploy/connectors/s3/sink-owl-events.json`
- GCS: `deploy/connectors/gcs/sink-owl-events-gcs.json`

Usage (with a running Kafka Connect at http://localhost:8083):

```bash
curl -sS -X POST -H 'Content-Type: application/json' \
  --data @deploy/connectors/s3/sink-owl-events.json \
  http://localhost:8083/connectors

# Or for GCS
curl -sS -X POST -H 'Content-Type: application/json' \
  --data @deploy/connectors/gcs/sink-owl-events-gcs.json \
  http://localhost:8083/connectors
```

Notes:
- Ensure Kafka Connect image includes the corresponding sink plugins (Confluent S3/GCS). For Redpanda, use Redpanda Connect or run a standard Kafka Connect with plugins mounted.
- Provide credentials files mounted inside the container as referenced in the configs.

Docker Compose includes a Kafka Connect service (`kafka-connect`) listening on `:8083`. You still need to provide plugins and credentials (mount into `/data/plugins` and `/data/creds`).

Prometheus recording rules are added to compute approximate monthly spend per tenant from cost counters (see `deploy/observability/rules/budget-rules.yml`).

Kubernetes (Prometheus Operator) integration manifests:
- ServiceMonitor: `deploy/k8s/observability/servicemonitor-owl-app.yaml`
- Grafana Dashboard ConfigMap: `deploy/k8s/observability/grafana-dashboard-cm.yaml`

## API v1.3 (Fallback, Safety, RBAC/API Keys)

Chat Request now supports optional fallback policy and returns safety labels:

- Request: `POST /api/v1/chat`
  - Fields:
    - `tenantId` string (required)
    - `question` string (required)
    - `document` string (optional scope)
    - `allowWeb` boolean (legacy; still honored)
    - `fallback` object (optional): `{ enabled: boolean, budgetUsd?: number, maxWebCalls?: number }`

- Response:
  - `{ answer: string, sources: string[], chatId: string }`

Safety and Guardrails
- Enable per-tenant via tenant settings or `owl.guardrails.enabled=true` for coarse global toggle.
- Safety outcomes include internal checks of prompt/answer and may return a refusal message.

Prompt Caching
- Exact prompt caching reduces LLM calls for repeated questions. Enabled by default internally; stored per-tenant with a 7-day TTL.
- Semantic cache remains enabled to short-circuit near-duplicates.

RBAC & API Keys
- API keys support header `X-API-Key: <token>` or `Authorization: ApiKey <token>`.
- Scopes map to authorities (`SCOPE_<scope>`). Suggested scopes:
  - `chat:write` – call chat API
  - `ingest:write` – ingest endpoints
  - `admin:read`, `admin:write` – admin endpoints
- Admin token endpoints:
  - Create: `POST /api/v1/admin/tokens` `{ tenantId, name, scopes: [] }` → `{ id, token }`
  - List: `GET /api/v1/admin/tokens?tenantId=...`
- Revoke: `DELETE /api/v1/admin/tokens/{id}?tenantId=...`

Tenant Settings
- Update settings: `POST /api/v1/admin/settings` `{ tenantId, fallbackEnabled?, guardrailsEnabled? }`

SSO (SAML/OIDC)
- OIDC JWT resource server supported; enable via `owl.security.enabled=true` and issuer URI.
- Optional SAML login for admin/UI can be enabled via Spring SAML. Provide your IdP metadata in standard Spring Boot SAML properties.

Error Codes
- `429` rate limited
- `403` forbidden (tenant mismatch or insufficient scope)
- `402` budget exceeded (message body indicates budget guardrail)
- `503` external search unavailable (fallback tried but tool disabled/unavailable)

Fallback Web Search Provider
- Configure one of:
  - SerpAPI: set `SERPAPI_API_KEY` and `owl.fallback.provider=serpapi`
  - Bing: set `BING_SEARCH_KEY` and optionally `BING_SEARCH_ENDPOINT`, `owl.fallback.provider=bing`
- Enable globally with `owl.fallback.enabled=true` and/or per tenant via Admin settings.
- The fallback builds a small grounded context from top web results (title + snippet) and applies safety checks before responding.

## API v2.0 (Plans/Quotas, Multi‑region, Connectors, Model Routing)

Headers
- `X-Data-Region`: optional request hint for data residency (e.g., `us-east-1`, `eu-west-1`). Tenant default region is used if omitted.

Plans & Quotas
- List plans: `GET /api/v2/admin/plans`
- Assign plan: `POST /api/v2/admin/plans/assign` `{ tenantId, planName }`
- Quotas enforced per plan with burst credits; budget guardrails remain in effect.

Connectors Marketplace (lifecycle)
- List connectors: `GET /api/v2/admin/connectors?tenantId=...`
- Create connector: `POST /api/v2/admin/connectors` `{ tenantId, type, config }`
- Start sync: `POST /api/v2/admin/connectors/{id}/sync?tenantId=...`
- Delete: `DELETE /api/v2/admin/connectors/{id}?tenantId=...`
Types: gdrive|confluence|notion|s3 (scaffold; provide credentials via `config`).

Model Routing
- Set routing: `POST /api/v2/admin/routing` `{ tenantId, provider, chatModel, embedModel }`
- Provider examples: `ollama`, `openai`, `azure`, `bedrock` (router stores preference; actual provider selection is applied by the service).

Continuous Evaluation
- Run eval: `POST /api/v2/admin/eval` `{ tenantId, tests: [{ question, mustContain }] }` → `{ total, passed, failures }`

Multi‑region data residency
- Tenant DBs and vector collections include region in names. Requests may pass `X-Data-Region` to influence per‑request routing where allowed.

## API Compatibility & Deprecation

- Breaking changes are introduced only with major versions (v2.x). v1.x endpoints remain as aliases until EOL.
- Deprecations are announced in minor releases and marked in OpenAPI under `deprecated: true` where applicable.
- Compatibility: Request/response fields are additive; unknown fields are ignored by the server.

## Metrics & Ops

- Actuator: `GET /actuator/health`, `/actuator/metrics`, `/actuator/prometheus`
- Primary metric: `chat.requests{tenantId,cache=[hit|miss],answer=[ok|empty]}`

## Data Model

- Qdrant: single collection, documents carry `tenantId`, `type` in payload; cache entries use `type=cache`.
- MongoDB core: `tenants` collection.
- MongoDB per‑tenant: DB `owl_tenant_{tenantId}`, collection `chats` for chat history (PII stays per‑tenant).

## Deployment

1. Ensure Docker (or provide Mongo, Qdrant, Ollama endpoints)
2. Configure env vars (see above)
3. `docker compose up -d --build`
4. Create a tenant, ingest docs, then chat

---

Happy building! 🦉
