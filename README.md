# OWL Chatbot - Multi-Tenant RAG Platform

[![Build Status](https://github.com/your-org/owl-chatbot/workflows/CI/badge.svg)](https://github.com/your-org/owl-chatbot/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-17-blue.svg)](https://openjdk.java.net/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.13-brightgreen.svg)](https://spring.io/projects/spring-boot)

OWL (Open Web Language) is a sophisticated multi-tenant RAG (Retrieval-Augmented Generation) chatbot platform that provides intelligent document search and conversational AI capabilities for enterprises.

## 🚀 Features

### Core Capabilities
- **Multi-Tenant Architecture**: Isolated data and configurations per tenant
- **Vector-Based Search**: Semantic document search using Qdrant vector database
- **Multi-Model AI Support**: Integration with OpenAI, Azure OpenAI, Ollama, and AWS Bedrock
- **Document Ingestion**: Support for PDFs, Office docs, web pages, and external connectors
- **Content Safety**: Built-in guardrails and content classification
- **Rate Limiting**: Per-tenant quota management and rate limiting
- **Comprehensive Monitoring**: Prometheus metrics, Grafana dashboards, and Jaeger tracing

### Enterprise Features
- **API Key Management**: Secure authentication and authorization
- **Audit Logging**: Complete audit trail for compliance
- **Cost Tracking**: Budget management and usage analytics
- **High Availability**: Microservice architecture with health checks
- **Scalability**: Horizontal scaling with Kubernetes support

## 🏗️ Architecture

OWL follows a microservice architecture pattern with the following services:

- **Chat Service**: Main orchestration and response generation
- **Retrieval Service**: Vector search and document retrieval
- **Ingestion Service**: Document processing and vectorization
- **Tenant Service**: Tenant management and configuration
- **Model Proxy Service**: AI model routing and management
- **Safety Service**: Content safety and guardrails
- **Connector Service**: External data source integration
- **API Gateway**: Request routing and authentication

## 📋 Prerequisites

### System Requirements
- **Java 17+**: OpenJDK or Oracle JDK
- **Docker & Docker Compose**: For containerized deployment
- **Maven 3.8+**: For building the application
- **Git**: For version control

### Optional (for production)
- **Kubernetes**: For orchestrated deployment
- **Helm**: For Kubernetes package management
- **NVIDIA GPU**: For local Ollama models (optional)

## 🚀 Quick Start

### 1. Clone the Repository
```bash
git clone https://github.com/your-org/owl-chatbot.git
cd owl-chatbot
```

### 2. Build the Application
```bash
mvn clean package -DskipTests
```

### 3. Start with Docker Compose (Monolithic)
```bash
# Start all services including dependencies
docker-compose up -d

# Check service health
curl http://localhost:8080/actuator/health
```

### 4. Start with Docker Compose (Microservices)
```bash
# Start microservices architecture
docker-compose -f docker-compose.microservices.yml up -d

# Check service health
curl http://localhost:8000/actuator/health  # API Gateway
```

## 🛠️ Development Setup

### Local Development (Monolithic)

1. **Start Dependencies**:
```bash
# Start only infrastructure services
docker-compose up -d mongodb qdrant redis

# Or use the development profile
docker-compose -f docker-compose.dev.yml up -d
```

2. **Configure Environment**:
```bash
# Copy environment template
cp .env.example .env

# Edit configuration
nano .env
```

3. **Run the Application**:
```bash
# Run with development profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Or run tests
mvn test
```

### Local Development (Microservices)

1. **Start Infrastructure**:
```bash
docker-compose -f docker-compose.microservices.yml up -d mongodb qdrant redis kafka
```

2. **Build and Run Services**:
```bash
# Build all services
mvn clean package -DskipTests

# Run individual services
cd services/tenant-service && mvn spring-boot:run
cd services/chat-service && mvn spring-boot:run
# ... repeat for other services
```

## 📚 API Documentation

### Interactive API Documentation
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8080/v3/api-docs

### API Endpoints

#### Chat API
```bash
# Send a chat message
curl -X POST http://localhost:8080/api/v1/chat \
  -H "Authorization: Bearer your-api-key" \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": "acme-corp",
    "question": "What is our company policy on remote work?",
    "allowWeb": false
  }'
```

#### Document Ingestion
```bash
# Upload a file
curl -X POST http://localhost:8080/api/v1/ingest/file \
  -H "Authorization: Bearer your-api-key" \
  -F "file=@document.pdf" \
  -F "tenantId=acme-corp"
```

#### Tenant Management
```bash
# Create a tenant
curl -X POST http://localhost:8080/api/v1/tenants \
  -H "Authorization: Bearer admin-api-key" \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": "acme-corp",
    "name": "Acme Corporation"
  }'
```

## 🐳 Docker Deployment

### Monolithic Deployment
```bash
# Build and run
docker-compose up -d

# View logs
docker-compose logs -f owl-app

# Scale the application
docker-compose up -d --scale owl-app=3
```

### Microservices Deployment
```bash
# Start all microservices
docker-compose -f docker-compose.microservices.yml up -d

# Check service status
docker-compose -f docker-compose.microservices.yml ps

# View logs for specific service
docker-compose -f docker-compose.microservices.yml logs -f owl-chat-service
```

### Custom Configuration
```bash
# Override environment variables
docker-compose -f docker-compose.microservices.yml up -d \
  -e SPRING_PROFILES_ACTIVE=production \
  -e MONGODB_URI=mongodb://your-mongodb:27017/owl
```

## ☸️ Kubernetes Deployment

### Prerequisites
- Kubernetes cluster (1.20+)
- Helm 3.0+
- kubectl configured

### Deploy with Helm
```bash
# Add the Helm repository
helm repo add owl-chatbot https://your-org.github.io/owl-chatbot-helm

# Install the chart
helm install owl-chatbot owl-chatbot/owl-chatbot \
  --namespace owl-chatbot \
  --create-namespace \
  --values values.yaml

# Check deployment status
kubectl get pods -n owl-chatbot
```

### Custom Values
```yaml
# values.yaml
global:
  imageRegistry: your-registry.com
  imageTag: latest

tenantService:
  replicas: 2
  resources:
    requests:
      memory: "512Mi"
      cpu: "250m"
    limits:
      memory: "1Gi"
      cpu: "500m"

chatService:
  replicas: 3
  resources:
    requests:
      memory: "1Gi"
      cpu: "500m"
    limits:
      memory: "2Gi"
      cpu: "1000m"

mongodb:
  enabled: true
  auth:
    enabled: true
    rootPassword: "secure-password"

qdrant:
  enabled: true
  persistence:
    enabled: true
    size: 10Gi
```

## 🔧 Configuration

### Environment Variables

#### Core Configuration
```bash
# Database
SPRING_DATA_MONGODB_URI=mongodb://localhost:27017/owl
QDRANT_URL=http://localhost:6333

# Redis
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PASSWORD=password123

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Security
OWL_SECURITY_ENABLED=true
OWL_RATE_LIMIT_QPM=120

# AI Models
OPENAI_API_KEY=your-openai-key
AZURE_OPENAI_API_KEY=your-azure-key
AZURE_OPENAI_ENDPOINT=https://your-resource.openai.azure.com/
```

#### Service-Specific Configuration
```bash
# Chat Service
OWL_RETRIEVAL_SCORE_THRESHOLD=0.45
OWL_RERANK_ENABLED=true
OWL_COST_ESTIMATE_PER_CALL_USD=0.0005

# Ingestion Service
OWL_INGESTION_MAX_FILE_SIZE=512MB
OWL_INGESTION_SUPPORTED_FORMATS=pdf,doc,docx,txt,html

# Safety Service
OWL_GUARDRAILS_ENABLED=true
OWL_SAFETY_MODEL=ollama:llama-guard
```

### Application Profiles

#### Development Profile
```yaml
# application-dev.yml
spring:
  profiles:
    active: dev
  
owl:
  security:
    enabled: false  # Disable auth for development
  cache:
    similarity-threshold: 0.80
  retrieval:
    score-threshold: 0.30
```

#### Production Profile
```yaml
# application-prod.yml
spring:
  profiles:
    active: prod

owl:
  security:
    enabled: true
  cache:
    similarity-threshold: 0.90
  retrieval:
    score-threshold: 0.45
  rateLimit:
    qpm: 120
```

## 📊 Monitoring and Observability

### Metrics
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin123)

### Tracing
- **Jaeger**: http://localhost:16686

### Health Checks
```bash
# Application health
curl http://localhost:8080/actuator/health

# Detailed health info
curl http://localhost:8080/actuator/health/detailed

# Custom health indicators
curl http://localhost:8080/actuator/health/readiness
curl http://localhost:8080/actuator/health/liveness
```

### Logging
```bash
# View application logs
docker-compose logs -f owl-app

# View specific service logs
docker-compose logs -f owl-chat-service

# Follow logs in real-time
docker-compose logs -f --tail=100 owl-app
```

## 🧪 Testing

### Unit Tests
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ChatServiceTest

# Run with coverage
mvn test jacoco:report
```

### Integration Tests
```bash
# Run integration tests
mvn test -Dtest=*IT

# Run with testcontainers
mvn test -Dspring.profiles.active=testcontainers
```

### Load Testing
```bash
# Install k6
curl https://github.com/grafana/k6/releases/download/v0.47.0/k6-v0.47.0-linux-amd64.tar.gz -L | tar xvz --strip-components 1

# Run load tests
k6 run tests/load/chat-load-test.js
```

## 🔒 Security

### API Key Management
```bash
# Create API key
curl -X POST http://localhost:8080/api/v1/tenants/acme-corp/api-keys \
  -H "Authorization: Bearer admin-key" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "production-key",
    "permissions": ["chat", "ingest", "search"]
  }'
```

### Content Safety
- Automatic content classification
- Configurable safety policies
- Content filtering and blocking
- Audit logging for safety events

### Data Privacy
- Tenant data isolation
- Encryption at rest and in transit
- GDPR compliance features
- Data retention policies

## 🚀 Performance Optimization

### Caching
- Redis for session and response caching
- Vector search result caching
- Model response caching

### Scaling
```bash
# Scale specific services
docker-compose up -d --scale owl-chat-service=3
docker-compose up -d --scale owl-retrieval-service=2

# Kubernetes scaling
kubectl scale deployment owl-chat-service --replicas=5
```

### Database Optimization
- MongoDB indexing strategies
- Qdrant collection optimization
- Connection pooling configuration

## 🐛 Troubleshooting

### Common Issues

#### Service Won't Start
```bash
# Check logs
docker-compose logs owl-app

# Check health
curl http://localhost:8080/actuator/health

# Check dependencies
docker-compose ps
```

#### Database Connection Issues
```bash
# Test MongoDB connection
docker exec -it owl-mongodb mongosh --eval "db.adminCommand('ping')"

# Test Qdrant connection
curl http://localhost:6333/collections
```

#### Memory Issues
```bash
# Check memory usage
docker stats

# Increase JVM heap size
export JAVA_OPTS="-Xmx2g -Xms1g"
```

### Debug Mode
```bash
# Enable debug logging
export LOGGING_LEVEL_COM_OWL=DEBUG

# Enable Spring Boot debug
export DEBUG=true
```

## 🤝 Contributing

### Development Workflow
1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Make your changes
4. Add tests for your changes
5. Run the test suite: `mvn test`
6. Commit your changes: `git commit -m 'Add amazing feature'`
7. Push to the branch: `git push origin feature/amazing-feature`
8. Open a Pull Request

### Code Style
- Follow Java coding conventions
- Use meaningful variable and method names
- Add Javadoc for public APIs
- Maintain test coverage above 80%

### Testing Requirements
- Unit tests for all new functionality
- Integration tests for API endpoints
- Performance tests for critical paths
- Security tests for authentication/authorization

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- [Spring Boot](https://spring.io/projects/spring-boot) - Application framework
- [Spring AI](https://spring.io/projects/spring-ai) - AI integration framework
- [Qdrant](https://qdrant.tech/) - Vector database
- [MongoDB](https://www.mongodb.com/) - Document database
- [Docker](https://www.docker.com/) - Containerization platform
- [Kubernetes](https://kubernetes.io/) - Container orchestration

## 📞 Support

- **Documentation**: [docs.owl-chatbot.com](https://docs.owl-chatbot.com)
- **Issues**: [GitHub Issues](https://github.com/your-org/owl-chatbot/issues)
- **Discussions**: [GitHub Discussions](https://github.com/your-org/owl-chatbot/discussions)
- **Email**: support@owl-chatbot.com

## 🔄 Changelog

### v3.0.0 (Latest)
- Microservice architecture implementation
- Enhanced API documentation
- Improved monitoring and observability
- Performance optimizations
- Security enhancements

### v2.0.0
- Multi-tenant support
- Vector search implementation
- AI model integration
- Content safety features

### v1.0.0
- Initial release
- Basic chat functionality
- Document ingestion
- Simple web interface

---

**Made with ❤️ by the OWL Team**