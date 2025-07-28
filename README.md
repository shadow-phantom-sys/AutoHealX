# AutoHealX: An AI-Enabled Self-Healing Micro-Commerce Platform

## Overview

AutoHealX is a cloud-native, container-based micro-commerce application built with Java 17 + Spring Boot that continuously observes itself, predicts failures, and repairs them automatically. The project demonstrates the integration of GitOps, AIOps, Chaos Engineering, Self-Healing Automation, and Deep Observability.

## Architecture

### Services
- **Product Service** - CRUD operations for products with low-latency caching
- **Cart Service** - Token-based shopping cart management
- **Order Service** - Transactional order placement
- **Payment Service** - Mock payment gateway with chaos testing capabilities
- **Inventory Worker** - Asynchronous inventory updates via Kafka events

### Technology Stack
- **Runtime**: Java 17, Spring Boot 3.x
- **Database**: MySQL 8 with JDBC/JPA
- **Containers**: Docker with multi-stage builds
- **Orchestration**: Kubernetes 1.30+
- **GitOps**: Helm + Argo CD
- **Observability**: OpenTelemetry, Prometheus, Grafana, Loki, Tempo
- **Self-Healing**: KEDA + HPA
- **AIOps**: Dynatrace OneAgent
- **Chaos Engineering**: Chaos Mesh
- **Security**: Trivy, Snyk, Vault

## Quick Start

### Prerequisites
- Java 17+
- Docker & Docker Compose
- Maven 3.8+
- kubectl (for Kubernetes deployment)

### Local Development
```bash
# Start all services with MySQL
docker-compose up -d

# Run tests
mvn test

# Build all services
mvn clean package
```

### Kubernetes Deployment
```bash
# Deploy to local cluster
helm install autohealx ./helm/autohealx

# GitOps deployment (requires Argo CD)
kubectl apply -f argocd/applications/
```

## Implementation Roadmap

- [x] **Sprint 0**: Bootstrap monorepo, baseline Spring Boot apps, MySQL schema
- [ ] **Sprint 1**: Dockerize services, local docker-compose setup
- [ ] **Sprint 2**: Helm charts, Kubernetes deployment
- [ ] **Sprint 3**: OpenTelemetry instrumentation, Grafana dashboards
- [ ] **Sprint 4**: GitHub Actions CI, Argo CD GitOps
- [ ] **Sprint 5**: KEDA & HPA autoscaling, K6 load testing
- [ ] **Sprint 6**: Dynatrace integration, predictive alerts
- [ ] **Sprint 7**: Chaos Mesh experiments, chaos pipeline
- [ ] **Sprint 8**: Documentation, whitepaper, demo video

## Key Features

### Self-Healing Capabilities
- Predictive anomaly detection with 15-minute lead time
- Auto-scaling based on custom Prometheus metrics
- Automatic rollback on error rate thresholds
- Database self-optimization

### Observability
- Distributed tracing with OpenTelemetry
- Metrics collection via Prometheus
- Centralized logging with Loki
- Database performance monitoring
- Business KPI dashboards

### Chaos Engineering
- Automated resilience testing
- Pod failure simulation
- Network latency injection
- Database connection chaos

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
