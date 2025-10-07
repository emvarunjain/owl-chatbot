#!/bin/bash

# OWL Chatbot Deployment Script
# This script provides various deployment options for the OWL chatbot platform

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
ENVIRONMENT="development"
PROFILE="dev"
SERVICES="all"
BUILD="true"
CLEAN="false"
HELP="false"

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to show usage
show_usage() {
    cat << EOF
OWL Chatbot Deployment Script

Usage: $0 [OPTIONS]

Options:
    -e, --environment ENV    Deployment environment (development, staging, production)
    -p, --profile PROFILE    Spring profile to use
    -s, --services SERVICES  Services to deploy (all, monolithic, microservices)
    -b, --build             Build the application (default: true)
    -c, --clean             Clean build artifacts before building
    -h, --help              Show this help message

Examples:
    $0                                    # Deploy in development mode
    $0 -e production -p prod             # Deploy to production
    $0 -s microservices -b false         # Deploy microservices without building
    $0 -c -e staging                     # Clean build and deploy to staging

Environments:
    development    Local development setup
    staging        Staging environment
    production     Production environment

Services:
    all            Deploy all services (default)
    monolithic     Deploy as monolithic application
    microservices  Deploy as microservices

EOF
}

# Function to check prerequisites
check_prerequisites() {
    print_status "Checking prerequisites..."
    
    # Check if Docker is installed
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed. Please install Docker first."
        exit 1
    fi
    
    # Check if Docker Compose is installed
    if ! command -v docker-compose &> /dev/null; then
        print_error "Docker Compose is not installed. Please install Docker Compose first."
        exit 1
    fi
    
    # Check if Maven is installed (for building)
    if [ "$BUILD" = "true" ] && ! command -v mvn &> /dev/null; then
        print_error "Maven is not installed. Please install Maven first."
        exit 1
    fi
    
    # Check if Java is installed (for building)
    if [ "$BUILD" = "true" ] && ! command -v java &> /dev/null; then
        print_error "Java is not installed. Please install Java 17+ first."
        exit 1
    fi
    
    print_success "Prerequisites check passed"
}

# Function to clean build artifacts
clean_build() {
    if [ "$CLEAN" = "true" ]; then
        print_status "Cleaning build artifacts..."
        mvn clean
        print_success "Build artifacts cleaned"
    fi
}

# Function to build the application
build_application() {
    if [ "$BUILD" = "true" ]; then
        print_status "Building application..."
        
        # Set Maven options based on environment
        MAVEN_OPTS=""
        if [ "$ENVIRONMENT" = "production" ]; then
            MAVEN_OPTS="-DskipTests"
        fi
        
        mvn package $MAVEN_OPTS
        
        print_success "Application built successfully"
    fi
}

# Function to deploy monolithic application
deploy_monolithic() {
    print_status "Deploying monolithic application..."
    
    # Create environment file
    cat > .env << EOF
ENVIRONMENT=$ENVIRONMENT
SPRING_PROFILES_ACTIVE=$PROFILE
MONGODB_URI=mongodb://admin:password123@mongodb:27017/owl?authSource=admin
QDRANT_URL=http://qdrant:6333
REDIS_HOST=redis
REDIS_PASSWORD=password123
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
EOF
    
    # Start services
    docker-compose up -d
    
    # Wait for services to be healthy
    print_status "Waiting for services to be healthy..."
    sleep 30
    
    # Check health
    if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
        print_success "Monolithic application deployed successfully"
        print_status "Application is available at: http://localhost:8080"
        print_status "API Documentation: http://localhost:8080/swagger-ui.html"
    else
        print_error "Application health check failed"
        exit 1
    fi
}

# Function to deploy microservices
deploy_microservices() {
    print_status "Deploying microservices..."
    
    # Create environment file for microservices
    cat > .env.microservices << EOF
ENVIRONMENT=$ENVIRONMENT
SPRING_PROFILES_ACTIVE=$PROFILE
MONGODB_URI=mongodb://admin:password123@mongodb:27017/owl?authSource=admin
QDRANT_URL=http://qdrant:6333
REDIS_HOST=redis
REDIS_PASSWORD=password123
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
TENANT_SERVICE_URL=http://owl-tenant-service:8080
CHAT_SERVICE_URL=http://owl-chat-service:8080
RETRIEVAL_SERVICE_URL=http://owl-retrieval-service:8080
INGESTION_SERVICE_URL=http://owl-ingestion-service:8080
MODEL_PROXY_SERVICE_URL=http://owl-model-proxy-service:8080
SAFETY_SERVICE_URL=http://owl-safety-service:8080
CONNECTOR_SERVICE_URL=http://owl-connector-service:8080
EOF
    
    # Start microservices
    docker-compose -f docker-compose.microservices.yml up -d
    
    # Wait for services to be healthy
    print_status "Waiting for microservices to be healthy..."
    sleep 60
    
    # Check health of key services
    services=("owl-tenant-service:8081" "owl-chat-service:8082" "owl-retrieval-service:8083")
    
    for service in "${services[@]}"; do
        IFS=':' read -r name port <<< "$service"
        if curl -f http://localhost:$port/actuator/health > /dev/null 2>&1; then
            print_success "$name is healthy"
        else
            print_error "$name health check failed"
            exit 1
        fi
    done
    
    print_success "Microservices deployed successfully"
    print_status "API Gateway is available at: http://localhost:8000"
    print_status "Individual services:"
    print_status "  - Tenant Service: http://localhost:8081"
    print_status "  - Chat Service: http://localhost:8082"
    print_status "  - Retrieval Service: http://localhost:8083"
    print_status "  - Ingestion Service: http://localhost:8084"
    print_status "  - Model Proxy Service: http://localhost:8085"
    print_status "  - Safety Service: http://localhost:8086"
    print_status "  - Connector Service: http://localhost:8087"
}

# Function to deploy all services
deploy_all() {
    print_status "Deploying all services..."
    
    # Deploy infrastructure first
    print_status "Starting infrastructure services..."
    docker-compose up -d mongodb qdrant redis kafka zookeeper prometheus grafana jaeger
    
    # Wait for infrastructure to be ready
    print_status "Waiting for infrastructure services..."
    sleep 30
    
    # Deploy application services
    case $SERVICES in
        "monolithic")
            deploy_monolithic
            ;;
        "microservices")
            deploy_microservices
            ;;
        "all")
            deploy_microservices
            ;;
    esac
}

# Function to show deployment status
show_status() {
    print_status "Deployment Status:"
    echo ""
    
    if [ "$SERVICES" = "microservices" ] || [ "$SERVICES" = "all" ]; then
        docker-compose -f docker-compose.microservices.yml ps
    else
        docker-compose ps
    fi
    
    echo ""
    print_status "Service URLs:"
    if [ "$SERVICES" = "microservices" ] || [ "$SERVICES" = "all" ]; then
        echo "  - API Gateway: http://localhost:8000"
        echo "  - Grafana: http://localhost:3000 (admin/admin123)"
        echo "  - Prometheus: http://localhost:9090"
        echo "  - Jaeger: http://localhost:16686"
    else
        echo "  - Application: http://localhost:8080"
        echo "  - Swagger UI: http://localhost:8080/swagger-ui.html"
    fi
}

# Function to stop services
stop_services() {
    print_status "Stopping services..."
    
    if [ "$SERVICES" = "microservices" ] || [ "$SERVICES" = "all" ]; then
        docker-compose -f docker-compose.microservices.yml down
    else
        docker-compose down
    fi
    
    print_success "Services stopped"
}

# Function to show logs
show_logs() {
    print_status "Showing logs..."
    
    if [ "$SERVICES" = "microservices" ] || [ "$SERVICES" = "all" ]; then
        docker-compose -f docker-compose.microservices.yml logs -f
    else
        docker-compose logs -f
    fi
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -e|--environment)
            ENVIRONMENT="$2"
            shift 2
            ;;
        -p|--profile)
            PROFILE="$2"
            shift 2
            ;;
        -s|--services)
            SERVICES="$2"
            shift 2
            ;;
        -b|--build)
            BUILD="true"
            shift
            ;;
        -c|--clean)
            CLEAN="true"
            shift
            ;;
        -h|--help)
            HELP="true"
            shift
            ;;
        --no-build)
            BUILD="false"
            shift
            ;;
        --stop)
            stop_services
            exit 0
            ;;
        --status)
            show_status
            exit 0
            ;;
        --logs)
            show_logs
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            show_usage
            exit 1
            ;;
    esac
done

# Show help if requested
if [ "$HELP" = "true" ]; then
    show_usage
    exit 0
fi

# Validate environment
case $ENVIRONMENT in
    "development"|"staging"|"production")
        ;;
    *)
        print_error "Invalid environment: $ENVIRONMENT"
        print_error "Valid environments: development, staging, production"
        exit 1
        ;;
esac

# Validate services
case $SERVICES in
    "all"|"monolithic"|"microservices")
        ;;
    *)
        print_error "Invalid services: $SERVICES"
        print_error "Valid services: all, monolithic, microservices"
        exit 1
        ;;
esac

# Main deployment flow
print_status "Starting OWL Chatbot deployment..."
print_status "Environment: $ENVIRONMENT"
print_status "Profile: $PROFILE"
print_status "Services: $SERVICES"
print_status "Build: $BUILD"
print_status "Clean: $CLEAN"
echo ""

# Execute deployment steps
check_prerequisites
clean_build
build_application
deploy_all
show_status

print_success "Deployment completed successfully!"
print_status "You can now access the OWL Chatbot platform."