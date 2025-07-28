# AutoHealX: Getting Started Guide

Welcome to AutoHealX, an AI-Enabled Self-Healing Micro-Commerce Platform! This guide will help you get the system up and running quickly.

## 🎯 What is AutoHealX?

AutoHealX is a cutting-edge cloud-native micro-commerce platform that demonstrates:

- **Self-Healing Microservices** - Automatically detects and repairs failures
- **AI-Driven Monitoring** - Uses machine learning for predictive anomaly detection
- **GitOps Deployment** - Declarative infrastructure and application management
- **Deep Observability** - Comprehensive metrics, logs, and traces
- **Chaos Engineering** - Built-in resilience testing
- **Container-Native** - Kubernetes-ready with Docker containerization

## 🏗️ Architecture Overview

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Product       │    │   Cart          │    │   Order         │
│   Service       │    │   Service       │    │   Service       │
│   (Port 8081)   │    │   (Port 8082)   │    │   (Port 8083)   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
                    ┌─────────────────┐
                    │   Payment       │
                    │   Service       │
                    │   (Port 8084)   │
                    └─────────────────┘
                                 │
         ┌───────────────────────┼───────────────────────┐
         │                       │                       │
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   MySQL         │    │   Redis         │    │   Kafka         │
│   Database      │    │   Cache         │    │   Message Queue │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

**Observability Stack:**
- **Prometheus** - Metrics collection
- **Grafana** - Visualization dashboards
- **Loki** - Log aggregation
- **Tempo** - Distributed tracing
- **OpenTelemetry** - Observability instrumentation

## 🚀 Quick Start (5 Minutes)

### Prerequisites

Ensure you have the following installed:

- **Docker** (20.10+) and **Docker Compose** (2.0+)
- **Java 17+** and **Maven 3.8+**
- **curl** (for testing APIs)
- **Python 3.8+** (for AI health monitor)

### One-Command Deployment

```bash
# Clone and deploy in one go
git clone <repository-url>
cd autohealx
./scripts/build-and-run.sh
```

That's it! The script will:
1. ✅ Check prerequisites
2. 🏗️ Build all Java services
3. 🐳 Start all Docker containers
4. 🔍 Verify deployment health
5. 📊 Show you all service URLs

## 📋 Step-by-Step Manual Setup

If you prefer to understand each step:

### 1. Build the Applications

```bash
# Build shared library
cd shared-lib
mvn clean install

# Build product service
cd ../product-service
mvn clean package

# Return to root
cd ..
```

### 2. Start Infrastructure

```bash
# Start database and cache
docker-compose up -d mysql redis kafka zookeeper

# Wait for services to be ready
sleep 30
```

### 3. Start Monitoring Stack

```bash
# Start observability services
docker-compose up -d prometheus grafana loki tempo otel-collector
```

### 4. Start Application Services

```bash
# Start microservices
docker-compose up -d product-service

# Start load balancer
docker-compose up -d nginx
```

### 5. Verify Deployment

```bash
# Check service health
curl http://localhost:8081/actuator/health
curl http://localhost:80/health
```

## 🧪 Testing the System

### Basic API Testing

```bash
# Get all products
curl http://localhost:80/api/v1/products

# Get specific product
curl http://localhost:80/api/v1/products/1

# Create a new product
curl -X POST http://localhost:80/api/v1/products \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "Test Product",
    "description": "A test product",
    "price": 99.99,
    "stockQuantity": 100,
    "category": "Electronics"
  }'

# Update product stock
curl -X POST http://localhost:80/api/v1/products/1/stock/reduce \
  -H 'Content-Type: application/json' \
  -d '{"quantity": 5}'

# Check stock availability
curl "http://localhost:80/api/v1/products/1/stock/check?quantity=10"
```

### Load Testing with K6

```bash
# Install k6 (if not already installed)
# brew install k6  # macOS
# apt install k6   # Ubuntu

# Create a simple load test
cat > load-test.js << 'EOF'
import http from 'k6/http';
import { check } from 'k6';

export let options = {
  stages: [
    { duration: '30s', target: 10 },
    { duration: '1m', target: 50 },
    { duration: '30s', target: 0 },
  ],
};

export default function() {
  let response = http.get('http://localhost:80/api/v1/products');
  check(response, {
    'status is 200': (r) => r.status === 200,
    'response time < 500ms': (r) => r.timings.duration < 500,
  });
}
EOF

# Run the load test
k6 run load-test.js
```

## 🤖 AI Health Monitoring

AutoHealX includes an AI-powered health monitor that demonstrates self-healing capabilities:

### Install Dependencies

```bash
pip3 install aiohttp scikit-learn numpy requests
```

### Run the AI Monitor

```bash
python3 monitoring/ai-health-monitor.py
```

The AI monitor will:
- 📊 Collect metrics from all services
- 🧠 Use ML models to detect anomalies
- 🚨 Generate alerts for threshold violations
- 🔧 Trigger automated remediation actions
- 📝 Log all activities for observability

### Simulating Failures

To see self-healing in action:

```bash
# Simulate high CPU usage
docker exec autohealx-product-service stress --cpu 8 --timeout 60s

# Simulate memory pressure
docker exec autohealx-product-service stress --vm 2 --vm-bytes 512M --timeout 60s

# Simulate network issues
docker exec autohealx-product-service tc qdisc add dev eth0 root netem delay 2000ms

# The AI monitor will detect these issues and trigger remediation
```

## 📊 Observability Dashboards

### Grafana Dashboards

Access Grafana at http://localhost:3000 (admin/admin123)

**Pre-configured dashboards include:**
- 📈 **Service Performance** - Response times, throughput, error rates
- 🖥️ **JVM Metrics** - Memory usage, GC activity, thread pools
- 🗄️ **Database Metrics** - Connection pools, query performance
- 🌐 **Infrastructure** - Container resources, network traffic
- 🔍 **Business KPIs** - Orders per minute, revenue metrics

### Prometheus Queries

Access Prometheus at http://localhost:9090

**Useful queries:**
```promql
# Request rate per service
rate(http_server_requests_seconds_count[5m])

# Error rate
rate(http_server_requests_seconds_count{status=~"4..|5.."}[5m]) / rate(http_server_requests_seconds_count[5m])

# Response time percentiles
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# JVM memory usage
jvm_memory_used_bytes / jvm_memory_max_bytes

# Database connections
hikaricp_connections_active
```

### Log Analysis with Loki

Access Loki through Grafana's Explore tab:

```logql
# Application logs
{service="product-service"} |= "ERROR"

# Performance logs
{service="product-service"} | json | duration > 1s

# Business events
{service="product-service"} |= "product.created"
```

## 🔧 Troubleshooting

### Common Issues

**Services not starting:**
```bash
# Check Docker resources
docker system df
docker system prune  # If needed

# Check logs
docker-compose logs product-service
```

**Database connection issues:**
```bash
# Verify MySQL is running
docker-compose exec mysql mysqladmin ping

# Check database logs
docker-compose logs mysql
```

**Memory issues:**
```bash
# Increase Docker memory limits
# Docker Desktop: Settings > Resources > Memory (8GB recommended)

# Check container resource usage
docker stats
```

### Health Check Commands

```bash
# Service health
curl http://localhost:8081/actuator/health

# Database health
docker-compose exec mysql mysqladmin ping

# Redis health
docker-compose exec redis redis-cli ping

# Kafka health
docker-compose exec kafka kafka-topics --bootstrap-server localhost:9092 --list
```

### Log Locations

```bash
# Application logs
docker-compose logs -f product-service

# Infrastructure logs
docker-compose logs -f mysql
docker-compose logs -f prometheus

# AI monitor logs
tail -f /tmp/ai-health-monitor.log

# Notifications
tail -f /tmp/autohealx-notifications.json
```

## 🎯 Next Steps

### Development Workflow

1. **Make code changes** in your IDE
2. **Rebuild specific service:**
   ```bash
   cd product-service
   mvn clean package
   docker-compose up -d --build product-service
   ```
3. **Test changes** using API calls
4. **Monitor** via Grafana dashboards

### Adding New Services

1. **Create service module** (copy product-service structure)
2. **Add to parent POM** and docker-compose.yml
3. **Update Nginx configuration** for routing
4. **Add monitoring** configuration
5. **Update build script**

### Production Deployment

1. **Kubernetes setup** with Helm charts
2. **GitOps pipeline** with Argo CD
3. **External monitoring** (Datadog, New Relic)
4. **Secret management** with Vault
5. **Security scanning** with Trivy

## 📚 Additional Resources

- **Architecture Documentation**: [ARCHITECTURE.md](ARCHITECTURE.md)
- **API Documentation**: http://localhost:8081/swagger-ui.html
- **Monitoring Runbooks**: [monitoring/runbooks/](monitoring/runbooks/)
- **Chaos Engineering Guide**: [chaos/README.md](chaos/README.md)
- **Performance Tuning**: [docs/performance.md](docs/performance.md)

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## 📞 Support

- **Issues**: GitHub Issues
- **Discussions**: GitHub Discussions
- **Documentation**: Wiki
- **Community**: Discord/Slack

---

🎉 **Congratulations!** You now have a fully functional AI-enabled self-healing micro-commerce platform running locally. Explore the dashboards, test the APIs, and watch the AI monitor in action!

Happy coding! 🚀