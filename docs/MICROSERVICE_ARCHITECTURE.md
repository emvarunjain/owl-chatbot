# OWL Chatbot - Microservice Architecture Plan

## Current State Analysis

The OWL chatbot is currently a monolithic Spring Boot application with the following main components:

### Core Services (Monolithic)
- **ChatService**: Main orchestration service handling chat requests
- **DocumentRetrievalService**: Vector search and document retrieval
- **IngestionService**: Document ingestion and processing
- **TenantService**: Tenant management and provisioning
- **ConnectorService**: External data source connectors
- **SafetyModelService**: Content safety and guardrails
- **ModelProviderRouter**: AI model routing and management
- **CacheService**: Semantic caching
- **BudgetService**: Cost tracking and budget management
- **QuotaService**: Rate limiting and quota management

## Proposed Microservice Architecture

### 1. **Chat Service** (`owl-chat-service`)
**Responsibility**: Main chat orchestration and response generation
- Chat request handling
- Response orchestration
- User interaction management
- Chat history management

**APIs**:
- `POST /api/v1/chat` - Process chat requests
- `GET /api/v1/chat/history/{tenantId}` - Get chat history
- `DELETE /api/v1/chat/history/{chatId}` - Delete chat history

**Dependencies**: Retrieval Service, Safety Service, Model Proxy Service

### 2. **Retrieval Service** (`owl-retrieval-service`)
**Responsibility**: Document search and retrieval
- Vector similarity search
- Document ranking and scoring
- Tenant-specific search isolation
- Search result caching

**APIs**:
- `POST /api/v1/search` - Search documents
- `GET /api/v1/search/suggestions` - Get search suggestions
- `POST /api/v1/search/feedback` - Search feedback

**Dependencies**: Vector Store (Qdrant), Tenant Service

### 3. **Ingestion Service** (`owl-ingestion-service`)
**Responsibility**: Document ingestion and processing
- File upload and processing
- Document parsing (PDF, Office, HTML)
- Text extraction and chunking
- Vector embedding generation
- Document deduplication

**APIs**:
- `POST /api/v1/ingest/file` - Upload and process files
- `POST /api/v1/ingest/url` - Ingest from URL
- `GET /api/v1/ingest/status/{jobId}` - Get ingestion status
- `DELETE /api/v1/ingest/{documentId}` - Delete document

**Dependencies**: Vector Store (Qdrant), Tenant Service, Safety Service

### 4. **Tenant Service** (`owl-tenant-service`)
**Responsibility**: Tenant management and configuration
- Tenant CRUD operations
- Tenant settings management
- Tenant provisioning
- API key management
- Plan and quota management

**APIs**:
- `POST /api/v1/tenants` - Create tenant
- `GET /api/v1/tenants/{tenantId}` - Get tenant details
- `PUT /api/v1/tenants/{tenantId}` - Update tenant
- `DELETE /api/v1/tenants/{tenantId}` - Delete tenant
- `POST /api/v1/tenants/{tenantId}/api-keys` - Create API key
- `GET /api/v1/tenants/{tenantId}/settings` - Get tenant settings

**Dependencies**: MongoDB, Redis (for caching)

### 5. **Model Proxy Service** (`owl-model-proxy-service`)
**Responsibility**: AI model routing and management
- Model provider routing (OpenAI, Azure, Ollama, Bedrock)
- Model credential management
- Model response caching
- Cost tracking per model

**APIs**:
- `POST /api/v1/models/chat` - Route chat requests to appropriate model
- `GET /api/v1/models/providers` - List available model providers
- `GET /api/v1/models/costs` - Get model usage costs

**Dependencies**: External AI providers, Tenant Service

### 6. **Safety Service** (`owl-safety-service`)
**Responsibility**: Content safety and guardrails
- Content classification
- Safety policy enforcement
- Guardrail management
- Content filtering

**APIs**:
- `POST /api/v1/safety/classify` - Classify content safety
- `GET /api/v1/safety/policies` - Get safety policies
- `POST /api/v1/safety/feedback` - Submit safety feedback

**Dependencies**: Safety models, Tenant Service

### 7. **Connector Service** (`owl-connector-service`)
**Responsibility**: External data source integration
- Google Drive integration
- SharePoint integration
- Web scraping
- Sitemap crawling
- Connector configuration management

**APIs**:
- `POST /api/v1/connectors` - Create connector
- `GET /api/v1/connectors/{tenantId}` - List connectors
- `POST /api/v1/connectors/{connectorId}/sync` - Trigger sync
- `DELETE /api/v1/connectors/{connectorId}` - Delete connector

**Dependencies**: External APIs, Ingestion Service

### 8. **Gateway Service** (`owl-gateway-service`)
**Responsibility**: API Gateway and routing
- Request routing to microservices
- Authentication and authorization
- Rate limiting
- Request/response logging
- API versioning

**APIs**:
- All external APIs are routed through the gateway

**Dependencies**: All microservices

## Data Architecture

### Databases
- **MongoDB**: Primary database for tenant data, chat history, configurations
- **Qdrant**: Vector database for document embeddings
- **Redis**: Caching and session management
- **PostgreSQL**: Audit logs and analytics (optional)

### Message Queue
- **Apache Kafka**: Inter-service communication and event streaming
- **Topics**:
  - `chat.events` - Chat events
  - `ingestion.events` - Document ingestion events
  - `tenant.events` - Tenant management events
  - `safety.events` - Safety classification events

## Deployment Architecture

### Container Orchestration
- **Kubernetes**: Container orchestration
- **Helm Charts**: Deployment management
- **Istio**: Service mesh for traffic management

### Monitoring and Observability
- **Prometheus**: Metrics collection
- **Grafana**: Metrics visualization
- **Jaeger**: Distributed tracing
- **ELK Stack**: Log aggregation and analysis

### CI/CD Pipeline
- **GitHub Actions**: CI/CD pipeline
- **Docker**: Containerization
- **Helm**: Kubernetes deployment
- **ArgoCD**: GitOps deployment

## Migration Strategy

### Phase 1: Preparation
1. Extract shared libraries and DTOs
2. Set up microservice infrastructure
3. Create API contracts and documentation
4. Set up monitoring and logging

### Phase 2: Service Extraction
1. Extract Tenant Service (lowest dependency)
2. Extract Safety Service (independent)
3. Extract Model Proxy Service (independent)
4. Extract Connector Service (independent)

### Phase 3: Core Services
1. Extract Retrieval Service
2. Extract Ingestion Service
3. Extract Chat Service (highest dependency)

### Phase 4: Gateway and Optimization
1. Implement API Gateway
2. Add service mesh
3. Optimize performance
4. Add comprehensive monitoring

## Benefits of Microservice Architecture

1. **Scalability**: Independent scaling of services
2. **Maintainability**: Smaller, focused codebases
3. **Technology Diversity**: Different services can use different technologies
4. **Fault Isolation**: Failure in one service doesn't affect others
5. **Team Autonomy**: Different teams can work on different services
6. **Deployment Independence**: Services can be deployed independently

## Challenges and Mitigation

1. **Distributed System Complexity**: Use service mesh and proper monitoring
2. **Data Consistency**: Implement eventual consistency patterns
3. **Network Latency**: Use caching and optimize communication
4. **Service Discovery**: Use Kubernetes service discovery
5. **Testing Complexity**: Implement comprehensive integration tests
