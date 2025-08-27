# 🦉 OWL — Multi-tenant RAG Chatbot

A Java-based RAG (Retrieval-Augmented Generation) chatbot that supports multiple tenants, document ingestion, and AI-powered chat.

## Tech Stack

- Java 21
- Spring Boot 3.3
- Spring AI 1.0.1
- Qdrant (Vector Database)
- MongoDB (Document Storage)
- Redpanda (Kafka API)
- Ollama (LLM)

## Prerequisites

- Java 21 JDK
- Docker and Docker Compose
- Ollama running on the host machine (Mac)

## Getting Started

```bash
# Clone the repository
git clone https://github.com/yourusername/owl.git
cd owl

# Build and run with Docker
docker compose up -d --build
```

## API Usage

### Create a Tenant

```bash
curl -X POST http://localhost:8080/api/tenants \
  -H "Content-Type: application/json" \
  -d '{"tenantId":"acme","name":"Acme Inc."}'
```

### Ingest Documents

```bash
curl -X POST "http://localhost:8080/api/ingest/file?tenantId=acme" \
  -F "file=@/path/to/your.pdf"
```

### Chat with the Bot

```bash
# Knowledge-base only query
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"tenantId":"acme","question":"What is our refund policy?","allowWeb":false}'

# With web search enabled
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"tenantId":"acme","question":"What is our refund policy?","allowWeb":true}'
```

## Architecture

- **Multi-tenant**: Isolated data and conversations per tenant
- **RAG Pipeline**: Document ingestion, vectorization, and retrieval
- **AI Integration**: Uses Ollama models for text generation
- **Document Processing**: Support for PDF documents

## Configuration

Configuration options are available in `application.yml`:

- Vector database settings
- MongoDB connection
- AI model parameters
- Caching behavior

## Development

To set up your development environment:

1. Install Java 21
2. Install Ollama locally
3. Clone the repository and open in your favorite IDE
4. Run the application with Maven: `mvn spring-boot:run`

## License

[MIT License](LICENSE)