# AutoHealX Build and Run Script for Windows PowerShell
# This script builds and deploys the entire AutoHealX platform

param(
    [switch]$CleanVolumes
)

# Colors for output
$InfoColor = "Cyan"
$SuccessColor = "Green"
$ErrorColor = "Red"
$WarningColor = "Yellow"

# Configuration
$ProjectName = "AutoHealX"
$ComposeFile = "docker-compose.yml"

function Write-Info($message) {
    Write-Host "[INFO] $message" -ForegroundColor $InfoColor
}

function Write-Success($message) {
    Write-Host "[SUCCESS] $message" -ForegroundColor $SuccessColor
}

function Write-Error($message) {
    Write-Host "[ERROR] $message" -ForegroundColor $ErrorColor
}

function Write-Warning($message) {
    Write-Host "[WARNING] $message" -ForegroundColor $WarningColor
}

function Test-Command($command) {
    try {
        Get-Command $command -ErrorAction Stop | Out-Null
        return $true
    }
    catch {
        return $false
    }
}

function Wait-ForService($url, $serviceName, $maxAttempts = 30) {
    Write-Info "Waiting for $serviceName to be ready..."
    
    for ($i = 1; $i -le $maxAttempts; $i++) {
        try {
            $response = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
            if ($response.StatusCode -eq 200) {
                Write-Success "$serviceName is ready!"
                return $true
            }
        }
        catch {
            Write-Info "Waiting for $serviceName... (attempt $i/$maxAttempts)"
            Start-Sleep -Seconds 5
        }
    }
    
    Write-Error "$serviceName failed to start within expected time"
    return $false
}

# Main script
Write-Host "🚀 Starting $ProjectName deployment..." -ForegroundColor Yellow
Write-Host ""

# Check prerequisites
Write-Info "Checking prerequisites..."

$missingTools = @()

if (-not (Test-Command "docker")) {
    $missingTools += "docker"
}

if (-not (Test-Command "docker-compose")) {
    $missingTools += "docker-compose"
}

if (-not (Test-Command "java")) {
    $missingTools += "java"
}

if (-not (Test-Command "mvn")) {
    $missingTools += "mvn"
}

if ($missingTools.Count -gt 0) {
    Write-Error "Missing required tools: $($missingTools -join ', ')"
    Write-Error "Please install the missing tools:"
    Write-Host "  • Docker Desktop: https://docker.com/products/docker-desktop"
    Write-Host "  • Java 17+: https://adoptium.net/"
    Write-Host "  • Maven: https://maven.apache.org/download.cgi"
    exit 1
}

# Check Docker daemon
try {
    docker info | Out-Null
    Write-Success "All prerequisites are met!"
}
catch {
    Write-Error "Docker daemon is not running. Please start Docker Desktop and try again."
    exit 1
}

# Clean up existing containers
Write-Info "Cleaning up existing containers and networks..."
if ($CleanVolumes) {
    docker-compose -f $ComposeFile down -v --remove-orphans 2>$null
}
else {
    docker-compose -f $ComposeFile down --remove-orphans 2>$null
}

# Create necessary directories
Write-Info "Creating necessary directories..."
$directories = @(
    "logs",
    "monitoring\grafana\dashboards",
    "monitoring\grafana\provisioning\datasources",
    "monitoring\grafana\provisioning\dashboards",
    "nginx\ssl"
)

foreach ($dir in $directories) {
    if (-not (Test-Path $dir)) {
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
    }
}

# Create Grafana configuration
Write-Info "Creating Grafana configuration..."

$datasourcesYml = @"
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
"@

$dashboardsYml = @"
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
"@

$datasourcesYml | Out-File -FilePath "monitoring\grafana\provisioning\datasources\datasources.yml" -Encoding UTF8
$dashboardsYml | Out-File -FilePath "monitoring\grafana\provisioning\dashboards\dashboards.yml" -Encoding UTF8

# Build Java applications
Write-Info "Building Java applications..."

# Build shared library
Write-Info "Building shared library..."
Set-Location "shared-lib"
$result = & mvn clean install -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to build shared library"
    Set-Location ".."
    exit 1
}
Set-Location ".."
Write-Success "shared-lib built successfully!"

# Build product service
Write-Info "Building product-service..."
Set-Location "product-service"
$result = & mvn clean package -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to build product-service"
    Set-Location ".."
    exit 1
}
Set-Location ".."
Write-Success "product-service built successfully!"

Write-Success "All Java applications built successfully!"

# Start infrastructure services
Write-Info "Starting infrastructure services..."
$result = & docker-compose -f $ComposeFile up -d mysql redis kafka zookeeper
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to start infrastructure services"
    exit 1
}

Write-Info "Waiting for database to be ready..."
Start-Sleep -Seconds 30

# Wait for MySQL to be healthy
$attempt = 1
$maxAttempts = 30

do {
    try {
        $result = & docker-compose -f $ComposeFile exec -T mysql mysqladmin ping -h localhost --silent
        if ($LASTEXITCODE -eq 0) {
            Write-Success "MySQL is ready!"
            break
        }
    }
    catch {
        # Continue to next attempt
    }
    
    Write-Info "Waiting for MySQL... (attempt $attempt/$maxAttempts)"
    Start-Sleep -Seconds 5
    $attempt++
} while ($attempt -le $maxAttempts)

if ($attempt -gt $maxAttempts) {
    Write-Error "MySQL failed to start within expected time"
    exit 1
}

Write-Success "Infrastructure services started!"

# Start monitoring stack
Write-Info "Starting monitoring stack..."
$result = & docker-compose -f $ComposeFile up -d prometheus grafana loki tempo otel-collector
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to start monitoring services"
    exit 1
}

Write-Info "Waiting for monitoring services to be ready..."
Start-Sleep -Seconds 15
Write-Success "Monitoring stack started!"

# Start application services
Write-Info "Starting application services..."
$result = & docker-compose -f $ComposeFile up -d product-service
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to start product-service"
    exit 1
}

Start-Sleep -Seconds 10

Write-Info "Starting nginx load balancer..."
$result = & docker-compose -f $ComposeFile up -d nginx
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to start nginx"
    exit 1
}

Write-Success "Application services started!"

Write-Info "Waiting for all services to stabilize..."
Start-Sleep -Seconds 30

# Verify deployment
Write-Info "Verifying deployment..."

$services = @(
    @{Url = "http://localhost:8081/actuator/health"; Name = "Product Service"},
    @{Url = "http://localhost:9090/-/healthy"; Name = "Prometheus"},
    @{Url = "http://localhost:3000/api/health"; Name = "Grafana"},
    @{Url = "http://localhost:80/health"; Name = "Nginx"}
)

$failedServices = @()

foreach ($service in $services) {
    try {
        $response = Invoke-WebRequest -Uri $service.Url -UseBasicParsing -TimeoutSec 10
        if ($response.StatusCode -eq 200) {
            Write-Success "$($service.Name) is healthy!"
        }
        else {
            Write-Error "$($service.Name) returned status $($response.StatusCode)"
            $failedServices += $service.Name
        }
    }
    catch {
        Write-Error "$($service.Name) is not responding!"
        $failedServices += $service.Name
    }
}

Write-Host ""
if ($failedServices.Count -eq 0) {
    Write-Host "🎉 AutoHealX deployment completed successfully!" -ForegroundColor Green
}
else {
    Write-Warning "Some services failed health checks: $($failedServices -join ', ')"
}

Write-Host ""
Write-Host "📊 Service URLs:" -ForegroundColor Yellow
Write-Host "  • Product Service API: http://localhost:8081/api/v1/products"
Write-Host "  • API Gateway (Nginx): http://localhost:80/api/v1/products"
Write-Host "  • Grafana Dashboard: http://localhost:3000 (admin/admin123)"
Write-Host "  • Prometheus: http://localhost:9090"
Write-Host "  • Loki: http://localhost:3100"
Write-Host "  • Tempo: http://localhost:3200"
Write-Host ""
Write-Host "🔍 Health Checks:" -ForegroundColor Yellow
Write-Host "  • Product Service: http://localhost:8081/actuator/health"
Write-Host "  • Prometheus: http://localhost:9090/-/healthy"
Write-Host "  • Grafana: http://localhost:3000/api/health"
Write-Host ""
Write-Host "📝 Sample API Calls:" -ForegroundColor Yellow
Write-Host "  • Get all products: curl http://localhost:80/api/v1/products"
Write-Host "  • Get product by ID: curl http://localhost:80/api/v1/products/1"
Write-Host ""
Write-Host "🤖 AI Health Monitor:" -ForegroundColor Yellow
Write-Host "  • Install Python deps: pip install aiohttp scikit-learn numpy requests"
Write-Host "  • Run: python monitoring/ai-health-monitor.py"
Write-Host ""
Write-Host "📋 Logs:" -ForegroundColor Yellow
Write-Host "  • View logs: docker-compose logs -f [service-name]"
Write-Host "  • All services: docker-compose logs -f"
Write-Host ""

# Try to install Python dependencies
Write-Info "Attempting to install Python dependencies for AI health monitor..."
try {
    if (Test-Command "pip") {
        pip install aiohttp scikit-learn numpy requests --quiet
        Write-Success "Python dependencies installed!"
    }
    elseif (Test-Command "pip3") {
        pip3 install aiohttp scikit-learn numpy requests --quiet
        Write-Success "Python dependencies installed!"
    }
    else {
        Write-Warning "pip not found. Please install Python dependencies manually:"
        Write-Warning "pip install aiohttp scikit-learn numpy requests"
    }
}
catch {
    Write-Warning "Failed to install Python dependencies. Please install manually:"
    Write-Warning "pip install aiohttp scikit-learn numpy requests"
}

Write-Host ""
Write-Host "Press any key to continue..." -ForegroundColor Yellow
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")