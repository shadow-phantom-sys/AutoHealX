@echo off
REM AutoHealX Build and Run Script for Windows
REM This script builds and deploys the entire AutoHealX platform

setlocal enabledelayedexpansion

REM Colors for output (limited in batch)
set "INFO=[INFO]"
set "SUCCESS=[SUCCESS]"
set "ERROR=[ERROR]"
set "WARNING=[WARNING]"

REM Configuration
set "PROJECT_NAME=AutoHealX"
set "COMPOSE_FILE=docker-compose.yml"

echo 🚀 Starting %PROJECT_NAME% deployment...
echo.

REM Function to check if command exists
where docker >nul 2>&1
if %errorlevel% neq 0 (
    echo %ERROR% Docker is not installed or not in PATH
    echo Please install Docker Desktop from https://docker.com/products/docker-desktop
    pause
    exit /b 1
)

where docker-compose >nul 2>&1
if %errorlevel% neq 0 (
    echo %ERROR% Docker Compose is not installed or not in PATH
    pause
    exit /b 1
)

where java >nul 2>&1
if %errorlevel% neq 0 (
    echo %ERROR% Java is not installed or not in PATH
    echo Please install Java 17+ from https://adoptium.net/
    pause
    exit /b 1
)

where mvn >nul 2>&1
if %errorlevel% neq 0 (
    echo %ERROR% Maven is not installed or not in PATH
    echo Please install Maven from https://maven.apache.org/download.cgi
    pause
    exit /b 1
)

echo %SUCCESS% All prerequisites are met!
echo.

REM Check Docker daemon
docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo %ERROR% Docker daemon is not running. Please start Docker Desktop and try again.
    pause
    exit /b 1
)

REM Clean up existing containers
echo %INFO% Cleaning up existing containers and networks...
docker-compose -f %COMPOSE_FILE% down --remove-orphans >nul 2>&1

REM Create necessary directories
echo %INFO% Creating necessary directories...
if not exist "logs" mkdir logs
if not exist "monitoring\grafana\dashboards" mkdir monitoring\grafana\dashboards
if not exist "monitoring\grafana\provisioning\datasources" mkdir monitoring\grafana\provisioning\datasources
if not exist "monitoring\grafana\provisioning\dashboards" mkdir monitoring\grafana\provisioning\dashboards
if not exist "nginx\ssl" mkdir nginx\ssl

REM Create Grafana configuration
echo %INFO% Creating Grafana configuration...
(
echo apiVersion: 1
echo.
echo datasources:
echo   - name: Prometheus
echo     type: prometheus
echo     access: proxy
echo     url: http://prometheus:9090
echo     isDefault: true
echo.
echo   - name: Loki
echo     type: loki
echo     access: proxy
echo     url: http://loki:3100
echo.
echo   - name: Tempo
echo     type: tempo
echo     access: proxy
echo     url: http://tempo:3200
) > monitoring\grafana\provisioning\datasources\datasources.yml

(
echo apiVersion: 1
echo.
echo providers:
echo   - name: 'AutoHealX Dashboards'
echo     orgId: 1
echo     folder: 'AutoHealX'
echo     type: file
echo     disableDeletion: false
echo     updateIntervalSeconds: 10
echo     allowUiUpdates: true
echo     options:
echo       path: /var/lib/grafana/dashboards
) > monitoring\grafana\provisioning\dashboards\dashboards.yml

REM Build Java applications
echo %INFO% Building Java applications...

echo %INFO% Building shared library...
cd shared-lib
call mvn clean install -DskipTests
if %errorlevel% neq 0 (
    echo %ERROR% Failed to build shared library
    cd ..
    pause
    exit /b 1
)
cd ..
echo %SUCCESS% shared-lib built successfully!

echo %INFO% Building product-service...
cd product-service
call mvn clean package -DskipTests
if %errorlevel% neq 0 (
    echo %ERROR% Failed to build product-service
    cd ..
    pause
    exit /b 1
)
cd ..
echo %SUCCESS% product-service built successfully!

echo %SUCCESS% All Java applications built successfully!

REM Start infrastructure services
echo %INFO% Starting infrastructure services...
docker-compose -f %COMPOSE_FILE% up -d mysql redis kafka zookeeper
if %errorlevel% neq 0 (
    echo %ERROR% Failed to start infrastructure services
    pause
    exit /b 1
)

echo %INFO% Waiting for database to be ready...
timeout /t 30 /nobreak >nul

REM Wait for MySQL to be healthy
set /a attempt=1
set /a max_attempts=30

:check_mysql
docker-compose -f %COMPOSE_FILE% exec -T mysql mysqladmin ping -h localhost --silent >nul 2>&1
if %errorlevel% equ 0 (
    echo %SUCCESS% MySQL is ready!
    goto mysql_ready
)

echo %INFO% Waiting for MySQL... (attempt %attempt%/%max_attempts%)
timeout /t 5 /nobreak >nul
set /a attempt+=1

if %attempt% leq %max_attempts% goto check_mysql

echo %ERROR% MySQL failed to start within expected time
pause
exit /b 1

:mysql_ready
echo %SUCCESS% Infrastructure services started!

REM Start monitoring stack
echo %INFO% Starting monitoring stack...
docker-compose -f %COMPOSE_FILE% up -d prometheus grafana loki tempo otel-collector
if %errorlevel% neq 0 (
    echo %ERROR% Failed to start monitoring services
    pause
    exit /b 1
)

echo %INFO% Waiting for monitoring services to be ready...
timeout /t 15 /nobreak >nul
echo %SUCCESS% Monitoring stack started!

REM Start application services
echo %INFO% Starting application services...
docker-compose -f %COMPOSE_FILE% up -d product-service
if %errorlevel% neq 0 (
    echo %ERROR% Failed to start product-service
    pause
    exit /b 1
)

timeout /t 10 /nobreak >nul

echo %INFO% Starting nginx load balancer...
docker-compose -f %COMPOSE_FILE% up -d nginx
if %errorlevel% neq 0 (
    echo %ERROR% Failed to start nginx
    pause
    exit /b 1
)

echo %SUCCESS% Application services started!

echo %INFO% Waiting for all services to stabilize...
timeout /t 30 /nobreak >nul

REM Verify deployment
echo %INFO% Verifying deployment...

curl -f -s http://localhost:8081/actuator/health >nul 2>&1
if %errorlevel% equ 0 (
    echo %SUCCESS% Product Service is healthy!
) else (
    echo %ERROR% Product Service is not responding!
)

curl -f -s http://localhost:9090/-/healthy >nul 2>&1
if %errorlevel% equ 0 (
    echo %SUCCESS% Prometheus is healthy!
) else (
    echo %ERROR% Prometheus is not responding!
)

curl -f -s http://localhost:3000/api/health >nul 2>&1
if %errorlevel% equ 0 (
    echo %SUCCESS% Grafana is healthy!
) else (
    echo %ERROR% Grafana is not responding!
)

curl -f -s http://localhost:80/health >nul 2>&1
if %errorlevel% equ 0 (
    echo %SUCCESS% Nginx is healthy!
) else (
    echo %ERROR% Nginx is not responding!
)

echo.
echo 🎉 AutoHealX deployment completed!
echo.
echo 📊 Service URLs:
echo   • Product Service API: http://localhost:8081/api/v1/products
echo   • API Gateway (Nginx): http://localhost:80/api/v1/products
echo   • Grafana Dashboard: http://localhost:3000 (admin/admin123)
echo   • Prometheus: http://localhost:9090
echo   • Loki: http://localhost:3100
echo   • Tempo: http://localhost:3200
echo.
echo 🔍 Health Checks:
echo   • Product Service: http://localhost:8081/actuator/health
echo   • Prometheus: http://localhost:9090/-/healthy
echo   • Grafana: http://localhost:3000/api/health
echo.
echo 📝 Sample API Calls:
echo   • Get all products: curl http://localhost:80/api/v1/products
echo   • Get product by ID: curl http://localhost:80/api/v1/products/1
echo.
echo 🤖 AI Health Monitor:
echo   • Install Python deps: pip install aiohttp scikit-learn numpy requests
echo   • Run: python monitoring/ai-health-monitor.py
echo.
echo 📋 Logs:
echo   • View logs: docker-compose logs -f [service-name]
echo   • All services: docker-compose logs -f
echo.

echo Press any key to continue...
pause >nul