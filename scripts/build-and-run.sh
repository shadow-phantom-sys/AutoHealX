#!/bin/bash

# AutoHealX Build and Run Script
# This script builds and deploys the entire AutoHealX platform

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
PROJECT_NAME="AutoHealX"
COMPOSE_FILE="docker-compose.yml"
NETWORK_NAME="autohealx-network"

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

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to check prerequisites
check_prerequisites() {
    print_status "Checking prerequisites..."
    
    local missing_tools=()
    
    if ! command_exists docker; then
        missing_tools+=("docker")
    fi
    
    if ! command_exists docker-compose; then
        missing_tools+=("docker-compose")
    fi
    
    if ! command_exists java; then
        missing_tools+=("java")
    fi
    
    if ! command_exists mvn; then
        missing_tools+=("maven")
    fi
    
    if [ ${#missing_tools[@]} -ne 0 ]; then
        print_error "Missing required tools: ${missing_tools[*]}"
        print_error "Please install the missing tools and try again."
        exit 1
    fi
    
    # Check Docker daemon
    if ! docker info >/dev/null 2>&1; then
        print_error "Docker daemon is not running. Please start Docker and try again."
        exit 1
    fi
    
    print_success "All prerequisites are met!"
}

# Function to clean up existing containers and networks
cleanup() {
    print_status "Cleaning up existing containers and networks..."
    
    # Stop and remove containers
    docker-compose -f $COMPOSE_FILE down --remove-orphans 2>/dev/null || true
    
    # Remove unused networks
    docker network prune -f >/dev/null 2>&1 || true
    
    # Remove unused volumes (be careful with this in production)
    if [ "$1" = "--clean-volumes" ]; then
        print_warning "Removing all volumes (data will be lost)..."
        docker-compose -f $COMPOSE_FILE down -v 2>/dev/null || true
        docker volume prune -f >/dev/null 2>&1 || true
    fi
    
    print_success "Cleanup completed!"
}

# Function to build Java applications
build_java_apps() {
    print_status "Building Java applications..."
    
    # Build shared library first
    print_status "Building shared library..."
    cd shared-lib
    mvn clean install -DskipTests
    cd ..
    
    # Build all services
    local services=("product-service")  # Add more services as they are implemented
    
    for service in "${services[@]}"; do
        if [ -d "$service" ]; then
            print_status "Building $service..."
            cd "$service"
            mvn clean package -DskipTests
            cd ..
            print_success "$service built successfully!"
        else
            print_warning "$service directory not found, skipping..."
        fi
    done
    
    print_success "All Java applications built successfully!"
}

# Function to create necessary directories
create_directories() {
    print_status "Creating necessary directories..."
    
    local dirs=(
        "logs"
        "monitoring/grafana/dashboards"
        "monitoring/grafana/provisioning/datasources"
        "monitoring/grafana/provisioning/dashboards"
        "nginx/ssl"
    )
    
    for dir in "${dirs[@]}"; do
        mkdir -p "$dir"
    done
    
    print_success "Directories created!"
}

# Function to generate SSL certificates (for development)
generate_ssl_certs() {
    print_status "Generating SSL certificates for development..."
    
    if [ ! -f "nginx/ssl/cert.pem" ] || [ ! -f "nginx/ssl/key.pem" ]; then
        openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
            -keyout nginx/ssl/key.pem \
            -out nginx/ssl/cert.pem \
            -subj "/C=US/ST=State/L=City/O=AutoHealX/CN=localhost" \
            >/dev/null 2>&1
        
        print_success "SSL certificates generated!"
    else
        print_status "SSL certificates already exist, skipping generation..."
    fi
}

# Function to create Grafana provisioning files
create_grafana_config() {
    print_status "Creating Grafana configuration..."
    
    # Create datasources configuration
    cat > monitoring/grafana/provisioning/datasources/datasources.yml << EOF
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    
  - name: Loki
    type: loki
    access: proxy
    url: http://loki:3100
    
  - name: Tempo
    type: tempo
    access: proxy
    url: http://tempo:3200
EOF

    # Create dashboards configuration
    cat > monitoring/grafana/provisioning/dashboards/dashboards.yml << EOF
apiVersion: 1

providers:
  - name: 'AutoHealX Dashboards'
    orgId: 1
    folder: 'AutoHealX'
    type: file
    disableDeletion: false
    updateIntervalSeconds: 10
    allowUiUpdates: true
    options:
      path: /var/lib/grafana/dashboards
EOF

    print_success "Grafana configuration created!"
}

# Function to start infrastructure services
start_infrastructure() {
    print_status "Starting infrastructure services..."
    
    # Start core infrastructure
    docker-compose -f $COMPOSE_FILE up -d mysql redis kafka zookeeper
    
    print_status "Waiting for database to be ready..."
    sleep 30
    
    # Wait for MySQL to be healthy
    local max_attempts=30
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        if docker-compose -f $COMPOSE_FILE exec -T mysql mysqladmin ping -h localhost --silent; then
            print_success "MySQL is ready!"
            break
        fi
        
        print_status "Waiting for MySQL... (attempt $attempt/$max_attempts)"
        sleep 5
        ((attempt++))
    done
    
    if [ $attempt -gt $max_attempts ]; then
        print_error "MySQL failed to start within expected time"
        exit 1
    fi
    
    print_success "Infrastructure services started!"
}

# Function to start monitoring stack
start_monitoring() {
    print_status "Starting monitoring stack..."
    
    docker-compose -f $COMPOSE_FILE up -d prometheus grafana loki tempo otel-collector
    
    print_status "Waiting for monitoring services to be ready..."
    sleep 15
    
    print_success "Monitoring stack started!"
}

# Function to start application services
start_applications() {
    print_status "Starting application services..."
    
    # Start services one by one to ensure proper startup order
    local services=("product-service")  # Add more as implemented
    
    for service in "${services[@]}"; do
        print_status "Starting $service..."
        docker-compose -f $COMPOSE_FILE up -d "$service"
        sleep 10  # Give each service time to start
    done
    
    # Start nginx last
    print_status "Starting nginx load balancer..."
    docker-compose -f $COMPOSE_FILE up -d nginx
    
    print_success "Application services started!"
}

# Function to verify deployment
verify_deployment() {
    print_status "Verifying deployment..."
    
    local services_to_check=(
        "http://localhost:8081/actuator/health:Product Service"
        "http://localhost:9090/-/healthy:Prometheus"
        "http://localhost:3000/api/health:Grafana"
        "http://localhost:3100/ready:Loki"
        "http://localhost:80/health:Nginx"
    )
    
    local failed_services=()
    
    for service_check in "${services_to_check[@]}"; do
        local url=$(echo "$service_check" | cut -d: -f1-2)
        local name=$(echo "$service_check" | cut -d: -f3)
        
        print_status "Checking $name..."
        
        if curl -f -s "$url" >/dev/null 2>&1; then
            print_success "$name is healthy!"
        else
            print_error "$name is not responding!"
            failed_services+=("$name")
        fi
    done
    
    if [ ${#failed_services[@]} -eq 0 ]; then
        print_success "All services are healthy!"
        return 0
    else
        print_error "Some services failed health checks: ${failed_services[*]}"
        return 1
    fi
}

# Function to show service URLs
show_service_urls() {
    echo ""
    echo "🎉 AutoHealX deployment completed successfully!"
    echo ""
    echo "📊 Service URLs:"
    echo "  • Product Service API: http://localhost:8081/api/v1/products"
    echo "  • API Gateway (Nginx): http://localhost:80/api/v1/products"
    echo "  • Grafana Dashboard: http://localhost:3000 (admin/admin123)"
    echo "  • Prometheus: http://localhost:9090"
    echo "  • Loki: http://localhost:3100"
    echo "  • Tempo: http://localhost:3200"
    echo ""
    echo "🔍 Health Checks:"
    echo "  • Product Service: http://localhost:8081/actuator/health"
    echo "  • Prometheus: http://localhost:9090/-/healthy"
    echo "  • Grafana: http://localhost:3000/api/health"
    echo ""
    echo "📝 Sample API Calls:"
    echo "  • Get all products: curl http://localhost:80/api/v1/products"
    echo "  • Get product by ID: curl http://localhost:80/api/v1/products/1"
    echo "  • Create product: curl -X POST http://localhost:80/api/v1/products \\"
    echo "    -H 'Content-Type: application/json' \\"
    echo "    -d '{\"name\":\"Test Product\",\"price\":99.99,\"stockQuantity\":100}'"
    echo ""
    echo "🤖 AI Health Monitor:"
    echo "  • Run: python3 monitoring/ai-health-monitor.py"
    echo "  • Requires: pip install aiohttp scikit-learn numpy"
    echo ""
    echo "📋 Logs:"
    echo "  • View logs: docker-compose logs -f [service-name]"
    echo "  • All services: docker-compose logs -f"
    echo ""
}

# Function to install Python dependencies for AI monitor
install_ai_monitor_deps() {
    print_status "Installing Python dependencies for AI health monitor..."
    
    if command_exists pip3; then
        pip3 install aiohttp scikit-learn numpy requests >/dev/null 2>&1 || {
            print_warning "Failed to install Python dependencies. AI monitor may not work."
            print_warning "Please manually install: pip3 install aiohttp scikit-learn numpy requests"
        }
        print_success "Python dependencies installed!"
    else
        print_warning "pip3 not found. Please install Python dependencies manually:"
        print_warning "pip3 install aiohttp scikit-learn numpy requests"
    fi
}

# Main deployment function
deploy() {
    local clean_volumes=false
    
    # Parse arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --clean-volumes)
                clean_volumes=true
                shift
                ;;
            *)
                print_error "Unknown option: $1"
                echo "Usage: $0 [--clean-volumes]"
                exit 1
                ;;
        esac
    done
    
    echo "🚀 Starting $PROJECT_NAME deployment..."
    echo ""
    
    # Run deployment steps
    check_prerequisites
    
    if [ "$clean_volumes" = true ]; then
        cleanup --clean-volumes
    else
        cleanup
    fi
    
    create_directories
    generate_ssl_certs
    create_grafana_config
    build_java_apps
    start_infrastructure
    start_monitoring
    start_applications
    
    print_status "Waiting for all services to stabilize..."
    sleep 30
    
    if verify_deployment; then
        install_ai_monitor_deps
        show_service_urls
    else
        print_error "Deployment verification failed. Check the logs for more details."
        echo "Debug commands:"
        echo "  • docker-compose logs"
        echo "  • docker-compose ps"
        exit 1
    fi
}

# Function to stop all services
stop() {
    print_status "Stopping all AutoHealX services..."
    docker-compose -f $COMPOSE_FILE down
    print_success "All services stopped!"
}

# Function to show status
status() {
    print_status "AutoHealX service status:"
    docker-compose -f $COMPOSE_FILE ps
}

# Function to show logs
logs() {
    local service=$1
    if [ -n "$service" ]; then
        docker-compose -f $COMPOSE_FILE logs -f "$service"
    else
        docker-compose -f $COMPOSE_FILE logs -f
    fi
}

# Main script logic
case "${1:-deploy}" in
    deploy)
        shift
        deploy "$@"
        ;;
    stop)
        stop
        ;;
    status)
        status
        ;;
    logs)
        shift
        logs "$@"
        ;;
    cleanup)
        shift
        cleanup "$@"
        ;;
    *)
        echo "Usage: $0 {deploy|stop|status|logs|cleanup} [options]"
        echo ""
        echo "Commands:"
        echo "  deploy [--clean-volumes]  Deploy the entire AutoHealX platform"
        echo "  stop                      Stop all services"
        echo "  status                    Show service status"
        echo "  logs [service-name]       Show logs (all services or specific service)"
        echo "  cleanup [--clean-volumes] Clean up containers and optionally volumes"
        echo ""
        exit 1
        ;;
esac