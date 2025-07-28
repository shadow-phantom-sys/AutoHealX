# 🪟 AutoHealX Windows Setup Guide

This guide provides step-by-step instructions for setting up and running AutoHealX on Windows.

## 🚀 Quick Start for Windows Users

### Option 1: PowerShell (Recommended)

1. **Open PowerShell as Administrator**
2. **Navigate to your project directory:**
   ```powershell
   cd "C:\Users\amark\OneDrive\Desktop\autohealXapp\AutoHealX"
   ```
3. **Run the PowerShell script:**
   ```powershell
   .\scripts\build-and-run.ps1
   ```

### Option 2: Command Prompt (Batch)

1. **Open Command Prompt as Administrator**
2. **Navigate to your project directory:**
   ```cmd
   cd "C:\Users\amark\OneDrive\Desktop\autohealXapp\AutoHealX"
   ```
3. **Run the batch script:**
   ```cmd
   scripts\build-and-run.bat
   ```

### Option 3: WSL2 (Linux Experience)

1. **Install WSL2** (if not already installed):
   ```powershell
   wsl --install
   ```
2. **Restart your computer** when prompted
3. **Open Ubuntu (WSL2)** and navigate to your project:
   ```bash
   cd /mnt/c/Users/amark/OneDrive/Desktop/autohealXapp/AutoHealX
   ```
4. **Run the bash script:**
   ```bash
   ./scripts/build-and-run.sh
   ```

## 📋 Prerequisites Installation

### 1. Install Docker Desktop

1. **Download Docker Desktop** from https://docker.com/products/docker-desktop
2. **Run the installer** and follow the setup wizard
3. **Start Docker Desktop** and wait for it to be ready
4. **Verify installation** in PowerShell:
   ```powershell
   docker --version
   docker-compose --version
   ```

### 2. Install Java 17

1. **Download Java 17** from https://adoptium.net/
2. **Run the installer** and follow the setup wizard
3. **Verify installation** in PowerShell:
   ```powershell
   java -version
   ```

### 3. Install Maven

**Option A: Using Chocolatey (Recommended)**
```powershell
# Install Chocolatey if not already installed
Set-ExecutionPolicy Bypass -Scope Process -Force; [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072; iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))

# Install Maven
choco install maven
```

**Option B: Manual Installation**
1. **Download Maven** from https://maven.apache.org/download.cgi
2. **Extract to a folder** (e.g., `C:\Program Files\Apache\maven`)
3. **Add to PATH** in System Environment Variables:
   - Add `C:\Program Files\Apache\maven\bin` to PATH
4. **Verify installation**:
   ```powershell
   mvn -version
   ```

### 4. Install Python (for AI Monitor)

1. **Download Python 3.8+** from https://python.org/downloads/
2. **Run the installer** and check "Add Python to PATH"
3. **Verify installation**:
   ```powershell
   python --version
   pip --version
   ```

### 5. Install Git (Optional but Recommended)

1. **Download Git for Windows** from https://git-scm.com/download/win
2. **Run the installer** with default settings
3. **Verify installation**:
   ```powershell
   git --version
   ```

## 🔧 Step-by-Step Manual Setup

If you prefer to run each step manually:

### Step 1: Verify Prerequisites

```powershell
# Check all required tools
docker --version
docker-compose --version
java -version
mvn -version
python --version
```

### Step 2: Start Docker Desktop

1. **Launch Docker Desktop** from Start Menu
2. **Wait for it to start** (system tray icon turns green)
3. **Verify Docker is running**:
   ```powershell
   docker info
   ```

### Step 3: Build Java Applications

```powershell
# Navigate to project directory
cd "C:\Users\amark\OneDrive\Desktop\autohealXapp\AutoHealX"

# Build shared library
cd shared-lib
mvn clean install -DskipTests
cd ..

# Build product service
cd product-service
mvn clean package -DskipTests
cd ..
```

### Step 4: Start Infrastructure Services

```powershell
# Start database and messaging services
docker-compose up -d mysql redis kafka zookeeper

# Wait for services to start
Start-Sleep -Seconds 30

# Verify MySQL is ready
docker-compose exec mysql mysqladmin ping -h localhost
```

### Step 5: Start Monitoring Stack

```powershell
# Start observability services
docker-compose up -d prometheus grafana loki tempo otel-collector

# Wait for services to start
Start-Sleep -Seconds 15
```

### Step 6: Start Application Services

```powershell
# Start the product service
docker-compose up -d product-service

# Wait for service to start
Start-Sleep -Seconds 10

# Start the load balancer
docker-compose up -d nginx
```

### Step 7: Verify Deployment

```powershell
# Check all services are running
docker-compose ps

# Test the API
Invoke-WebRequest -Uri "http://localhost:80/api/v1/products" -UseBasicParsing
```

## 🧪 Testing the System

### Run Automated Tests

```powershell
# Run API tests
.\scripts\test-api.ps1

# Run performance tests
.\scripts\test-api.ps1 perf

# Run all tests
.\scripts\test-api.ps1 all
```

### Manual API Testing

```powershell
# Test with PowerShell
Invoke-WebRequest -Uri "http://localhost:80/api/v1/products" -UseBasicParsing

# Test with curl (if installed)
curl http://localhost:80/api/v1/products

# Create a new product
$productData = @{
    name = "Windows Test Product"
    description = "Created from Windows"
    price = 99.99
    stockQuantity = 50
    category = "Test"
} | ConvertTo-Json

Invoke-WebRequest -Uri "http://localhost:80/api/v1/products" -Method POST -Body $productData -ContentType "application/json" -UseBasicParsing
```

## 🤖 AI Health Monitor Setup

### Install Python Dependencies

```powershell
# Install required packages
pip install aiohttp scikit-learn numpy requests

# Or if you get permission errors
pip install --user aiohttp scikit-learn numpy requests
```

### Run the AI Monitor

```powershell
# Start the AI health monitor
python monitoring\ai-health-monitor.py
```

**Expected Output:**
```
🚀 AutoHealX AI-Powered Health Monitor Starting...
This demo shows AI-driven self-healing capabilities:
• Continuous health monitoring
• ML-based anomaly detection
• Automated remediation actions
• Comprehensive logging and notifications

Press Ctrl+C to stop monitoring

2024-01-15 10:30:00 - AutoHealX-AI-Monitor - INFO - Starting continuous monitoring...
```

## 📊 Access Monitoring Dashboards

### Open in Browser

- **Grafana**: http://localhost:3000 (admin/admin123)
- **Prometheus**: http://localhost:9090
- **Product API**: http://localhost:80/api/v1/products

### PowerShell Commands

```powershell
# Open Grafana in default browser
Start-Process "http://localhost:3000"

# Open Prometheus
Start-Process "http://localhost:9090"

# Test API endpoint
Invoke-WebRequest -Uri "http://localhost:8081/actuator/health" -UseBasicParsing
```

## 🔧 Troubleshooting Windows Issues

### Common Windows-Specific Problems

#### 1. PowerShell Execution Policy

**Problem**: `cannot be loaded because running scripts is disabled`

**Solution**:
```powershell
# Run as Administrator
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

#### 2. Docker Desktop Not Starting

**Problem**: Docker Desktop fails to start

**Solutions**:
```powershell
# Enable Hyper-V (if not enabled)
Enable-WindowsOptionalFeature -Online -FeatureName Microsoft-Hyper-V -All

# Enable WSL2 (if using WSL2 backend)
wsl --install

# Restart Docker Desktop
Stop-Process -Name "Docker Desktop" -Force
Start-Process "C:\Program Files\Docker\Docker\Docker Desktop.exe"
```

#### 3. Port Already in Use

**Problem**: `Port 8081 is already allocated`

**Solution**:
```powershell
# Find what's using the port
netstat -ano | findstr :8081

# Kill the process (replace PID with actual process ID)
taskkill /PID <PID> /F

# Or change ports in docker-compose.yml
```

#### 4. Java/Maven Not Found

**Problem**: `'java' is not recognized as an internal or external command`

**Solution**:
```powershell
# Check if Java is installed
Get-Command java -ErrorAction SilentlyContinue

# Add to PATH manually (replace with your Java installation path)
$env:PATH += ";C:\Program Files\Eclipse Adoptium\jdk-17.0.1.12-hotspot\bin"

# Or install via Chocolatey
choco install openjdk17 maven
```

#### 5. Long Path Issues

**Problem**: File paths too long

**Solution**:
```powershell
# Enable long path support (as Administrator)
New-ItemProperty -Path "HKLM:\SYSTEM\CurrentControlSet\Control\FileSystem" -Name "LongPathsEnabled" -Value 1 -PropertyType DWORD -Force

# Or move project to shorter path
# Example: C:\autohealx instead of C:\Users\amark\OneDrive\Desktop\autohealXapp\AutoHealX
```

### Health Check Commands

```powershell
# Check Docker
docker info

# Check services
docker-compose ps

# Check specific service logs
docker-compose logs product-service

# Check Windows services
Get-Service | Where-Object {$_.Name -like "*docker*"}

# Check network connectivity
Test-NetConnection -ComputerName localhost -Port 8081
```

### Performance Optimization

```powershell
# Check system resources
Get-Process | Sort-Object CPU -Descending | Select-Object -First 10

# Check Docker resource usage
docker stats

# Increase Docker memory (Docker Desktop > Settings > Resources)
# Recommended: 8GB RAM, 4 CPU cores

# Clean up Docker resources
docker system prune -a
docker volume prune
```

## 🛑 Stopping the System

### Stop All Services

```powershell
# Stop all containers
docker-compose down

# Stop and remove volumes (WARNING: Data will be lost)
docker-compose down -v

# Stop Docker Desktop
Stop-Process -Name "Docker Desktop" -Force
```

### Clean Up

```powershell
# Remove all Docker resources
docker system prune -a -f

# Remove specific volumes
docker volume rm autohealx_mysql_data autohealx_redis_data
```

## 📝 Windows-Specific Tips

### 1. Use PowerShell ISE or VS Code

For better script editing and debugging:
- **PowerShell ISE**: Built into Windows
- **VS Code**: Download from https://code.visualstudio.com/

### 2. Windows Terminal (Recommended)

Install Windows Terminal for better command-line experience:
```powershell
# Install via Microsoft Store or
winget install Microsoft.WindowsTerminal
```

### 3. File Path Considerations

- Use forward slashes (`/`) or escape backslashes (`\\`) in paths
- Avoid spaces in directory names when possible
- Consider using shorter paths (e.g., `C:\autohealx`)

### 4. Antivirus Exclusions

Add Docker and project directories to antivirus exclusions:
- `C:\Program Files\Docker\`
- Your project directory
- Docker volumes location

## 🎯 Next Steps

1. **Explore the system** using the web interfaces
2. **Run load tests** to see performance
3. **Monitor the AI system** in action
4. **Simulate failures** to see self-healing
5. **Customize configurations** for your needs

## 📞 Windows-Specific Support

If you encounter Windows-specific issues:

1. **Check Windows Event Viewer** for system errors
2. **Verify Windows version compatibility** (Windows 10/11 recommended)
3. **Ensure sufficient disk space** (at least 10GB free)
4. **Check Windows Defender/Antivirus** exclusions
5. **Try running as Administrator** if permission issues occur

---

🎉 **Success!** You now have AutoHealX running on Windows with full AI-powered self-healing capabilities!

**Access your system:**
- **API**: http://localhost:80/api/v1/products
- **Grafana**: http://localhost:3000 (admin/admin123)
- **Prometheus**: http://localhost:9090

**Test your system:**
```powershell
.\scripts\test-api.ps1
python monitoring\ai-health-monitor.py
```

Happy coding on Windows! 🚀