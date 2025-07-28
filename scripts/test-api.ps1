# AutoHealX API Test Script for Windows PowerShell
# This script tests the Product Service API to verify the system is working correctly

# Colors for output
$TestColor = "Cyan"
$PassColor = "Green"
$FailColor = "Red"
$InfoColor = "Yellow"

# Configuration
$BaseUrl = "http://localhost:80/api/v1"
$ProductServiceUrl = "http://localhost:8081/api/v1"

function Write-Test($message) {
    Write-Host "[TEST] $message" -ForegroundColor $TestColor
}

function Write-Pass($message) {
    Write-Host "[PASS] $message" -ForegroundColor $PassColor
}

function Write-Fail($message) {
    Write-Host "[FAIL] $message" -ForegroundColor $FailColor
}

function Write-Info($message) {
    Write-Host "[INFO] $message" -ForegroundColor $InfoColor
}

function Test-ServiceAvailability($url, $serviceName) {
    Write-Test "Checking $serviceName availability..."
    
    try {
        $healthUrl = "$url/health"
        $actuatorUrl = "$url/actuator/health"
        
        try {
            $response = Invoke-WebRequest -Uri $healthUrl -UseBasicParsing -TimeoutSec 5
            if ($response.StatusCode -eq 200) {
                Write-Pass "$serviceName is available"
                return $true
            }
        }
        catch {
            try {
                $response = Invoke-WebRequest -Uri $actuatorUrl -UseBasicParsing -TimeoutSec 5
                if ($response.StatusCode -eq 200) {
                    Write-Pass "$serviceName is available"
                    return $true
                }
            }
            catch {
                Write-Fail "$serviceName is not available"
                return $false
            }
        }
    }
    catch {
        Write-Fail "$serviceName is not available"
        return $false
    }
}

function Test-ApiEndpoint($method, $endpoint, $data, $expectedStatus, $description) {
    Write-Test $description
    
    try {
        $uri = "$BaseUrl$endpoint"
        $headers = @{}
        
        if ($data) {
            $headers["Content-Type"] = "application/json"
        }
        
        $params = @{
            Uri = $uri
            Method = $method
            UseBasicParsing = $true
            TimeoutSec = 10
        }
        
        if ($data) {
            $params.Body = $data
            $params.Headers = $headers
        }
        
        $response = Invoke-WebRequest @params
        
        if ($response.StatusCode -eq $expectedStatus) {
            Write-Pass "Status: $($response.StatusCode)"
            if ($response.Content -and $response.Content -ne "null") {
                try {
                    $jsonContent = $response.Content | ConvertFrom-Json
                    Write-Host "Response: $($jsonContent | ConvertTo-Json -Compress)" -ForegroundColor Gray
                }
                catch {
                    Write-Host "Response: $($response.Content)" -ForegroundColor Gray
                }
            }
            Write-Host ""
            return $true, $response.Content
        }
        else {
            Write-Fail "Expected: $expectedStatus, Got: $($response.StatusCode)"
            Write-Host "Response: $($response.Content)" -ForegroundColor Gray
            Write-Host ""
            return $false, $null
        }
    }
    catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        if ($statusCode -eq $expectedStatus) {
            Write-Pass "Status: $statusCode"
            Write-Host ""
            return $true, $null
        }
        else {
            Write-Fail "Expected: $expectedStatus, Got: $statusCode"
            if ($_.Exception.Response) {
                $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
                $responseBody = $reader.ReadToEnd()
                Write-Host "Response: $responseBody" -ForegroundColor Gray
            }
            Write-Host ""
            return $false, $null
        }
    }
}

function Extract-Id($jsonContent) {
    try {
        $obj = $jsonContent | ConvertFrom-Json
        return $obj.id
    }
    catch {
        return $null
    }
}

# Main test suite
function Run-Tests {
    Write-Host "🧪 AutoHealX API Test Suite" -ForegroundColor Yellow
    Write-Host "============================" -ForegroundColor Yellow
    Write-Host ""
    
    # Check service availability
    $servicesOk = $true
    
    if (-not (Test-ServiceAvailability "http://localhost:80" "Nginx Load Balancer")) {
        $servicesOk = $false
    }
    
    if (-not (Test-ServiceAvailability $ProductServiceUrl "Product Service")) {
        $servicesOk = $false
    }
    
    if (-not $servicesOk) {
        Write-Fail "Some services are not available. Please check your deployment."
        return
    }
    
    Write-Host ""
    Write-Info "Starting API tests..."
    Write-Host ""
    
    $failedTests = 0
    $totalTests = 0
    
    # Test 1: Get all products
    $totalTests++
    $result, $content = Test-ApiEndpoint "GET" "/products" $null 200 "Get all products"
    if (-not $result) { $failedTests++ }
    
    # Test 2: Get specific product
    $totalTests++
    $result, $content = Test-ApiEndpoint "GET" "/products/1" $null 200 "Get product by ID"
    if (-not $result) { $failedTests++ }
    
    # Test 3: Get non-existent product
    $totalTests++
    $result, $content = Test-ApiEndpoint "GET" "/products/99999" $null 404 "Get non-existent product (should fail)"
    if (-not $result) { $failedTests++ }
    
    # Test 4: Create new product
    $newProductData = @{
        name = "API Test Product"
        description = "Created by API test script"
        price = 199.99
        stockQuantity = 50
        category = "Test"
        imageUrl = "https://example.com/test.jpg"
    } | ConvertTo-Json
    
    $totalTests++
    Write-Test "Create new product"
    
    try {
        $response = Invoke-WebRequest -Uri "$BaseUrl/products" -Method POST -Body $newProductData -ContentType "application/json" -UseBasicParsing -TimeoutSec 10
        
        if ($response.StatusCode -eq 201) {
            Write-Pass "Status: 201"
            $productId = Extract-Id $response.Content
            Write-Host "Created product ID: $productId" -ForegroundColor Gray
            Write-Host "Response: $($response.Content)" -ForegroundColor Gray
            Write-Host ""
            
            if ($productId) {
                # Test 5: Update the created product
                $updateData = @{
                    name = "Updated API Test Product"
                    description = "Updated by API test script"
                    price = 299.99
                    stockQuantity = 75
                    category = "Test Updated"
                    imageUrl = "https://example.com/test-updated.jpg"
                } | ConvertTo-Json
                
                $totalTests++
                $result, $content = Test-ApiEndpoint "PUT" "/products/$productId" $updateData 200 "Update created product"
                if (-not $result) { $failedTests++ }
                
                # Test 6: Reduce stock
                $stockRequest = @{ quantity = 10 } | ConvertTo-Json
                $totalTests++
                $result, $content = Test-ApiEndpoint "POST" "/products/$productId/stock/reduce" $stockRequest 200 "Reduce product stock"
                if (-not $result) { $failedTests++ }
                
                # Test 7: Increase stock
                $stockRequest = @{ quantity = 5 } | ConvertTo-Json
                $totalTests++
                $result, $content = Test-ApiEndpoint "POST" "/products/$productId/stock/increase" $stockRequest 200 "Increase product stock"
                if (-not $result) { $failedTests++ }
                
                # Test 8: Check stock availability
                $totalTests++
                $result, $content = Test-ApiEndpoint "GET" "/products/$productId/stock/check?quantity=20" $null 200 "Check stock availability"
                if (-not $result) { $failedTests++ }
                
                # Test 9: Delete the created product
                $totalTests++
                $result, $content = Test-ApiEndpoint "DELETE" "/products/$productId" $null 204 "Delete created product"
                if (-not $result) { $failedTests++ }
            }
        }
        else {
            Write-Fail "Expected: 201, Got: $($response.StatusCode)"
            $failedTests++
        }
    }
    catch {
        Write-Fail "Failed to create product: $($_.Exception.Message)"
        $failedTests++
    }
    
    # Test 10: Get product categories
    $totalTests++
    $result, $content = Test-ApiEndpoint "GET" "/products/categories" $null 200 "Get product categories"
    if (-not $result) { $failedTests++ }
    
    # Test 11: Get low stock products
    $totalTests++
    $result, $content = Test-ApiEndpoint "GET" "/products/low-stock?threshold=10" $null 200 "Get low stock products"
    if (-not $result) { $failedTests++ }
    
    # Test 12: Search products by name
    $totalTests++
    $result, $content = Test-ApiEndpoint "GET" "/products?search=iPhone" $null 200 "Search products by name"
    if (-not $result) { $failedTests++ }
    
    # Test 13: Filter products by category
    $totalTests++
    $result, $content = Test-ApiEndpoint "GET" "/products?category=Electronics" $null 200 "Filter products by category"
    if (-not $result) { $failedTests++ }
    
    # Test 14: Test pagination
    $totalTests++
    $result, $content = Test-ApiEndpoint "GET" "/products?page=0&size=5" $null 200 "Test pagination"
    if (-not $result) { $failedTests++ }
    
    # Test 15: Invalid data validation
    $invalidData = @{
        name = ""
        price = -10
        stockQuantity = -5
    } | ConvertTo-Json
    
    $totalTests++
    $result, $content = Test-ApiEndpoint "POST" "/products" $invalidData 400 "Test validation (should fail)"
    if (-not $result) { $failedTests++ }
    
    # Summary
    Write-Host ""
    Write-Host "🏁 Test Results Summary" -ForegroundColor Yellow
    Write-Host "======================" -ForegroundColor Yellow
    Write-Host "Total tests: $totalTests"
    Write-Host "Passed: $($totalTests - $failedTests)" -ForegroundColor Green
    Write-Host "Failed: $failedTests" -ForegroundColor $(if ($failedTests -eq 0) { "Green" } else { "Red" })
    Write-Host ""
    
    if ($failedTests -eq 0) {
        Write-Host "All tests passed! 🎉" -ForegroundColor Green
        Write-Host ""
        Write-Host "✅ Your AutoHealX system is working correctly!" -ForegroundColor Green
        Write-Host ""
        Write-Host "📊 Next steps:" -ForegroundColor Yellow
        Write-Host "  • Open Grafana: http://localhost:3000 (admin/admin123)"
        Write-Host "  • View metrics: http://localhost:9090"
        Write-Host "  • Run AI monitor: python monitoring/ai-health-monitor.py"
        Write-Host ""
    }
    else {
        Write-Host "Some tests failed. Please check your deployment." -ForegroundColor Red
        Write-Host ""
        Write-Host "🔧 Debugging tips:" -ForegroundColor Yellow
        Write-Host "  • Check logs: docker-compose logs product-service"
        Write-Host "  • Verify health: curl http://localhost:8081/actuator/health"
        Write-Host "  • Check database: docker-compose exec mysql mysqladmin ping"
        Write-Host ""
    }
}

# Run performance test
function Run-PerformanceTest {
    Write-Host "🚀 Running basic performance test..." -ForegroundColor Yellow
    Write-Host ""
    
    Write-Test "Measuring response times for 10 requests..."
    
    $totalTime = 0
    $successfulRequests = 0
    
    for ($i = 1; $i -le 10; $i++) {
        try {
            $startTime = Get-Date
            $response = Invoke-WebRequest -Uri "$BaseUrl/products" -UseBasicParsing -TimeoutSec 10
            $endTime = Get-Date
            
            if ($response.StatusCode -eq 200) {
                $responseTime = ($endTime - $startTime).TotalMilliseconds
                $totalTime += $responseTime
                $successfulRequests++
                Write-Host "Request $i`: $([math]::Round($responseTime, 0))ms"
            }
            else {
                Write-Host "Request $i`: Failed (Status: $($response.StatusCode))"
            }
        }
        catch {
            Write-Host "Request $i`: Failed (Error: $($_.Exception.Message))"
        }
    }
    
    if ($successfulRequests -gt 0) {
        $avgTime = [math]::Round($totalTime / $successfulRequests, 0)
        Write-Host ""
        Write-Pass "Average response time: ${avgTime}ms"
        Write-Pass "Success rate: $successfulRequests/10"
        
        if ($avgTime -lt 100) {
            Write-Pass "Excellent performance! 🚀"
        }
        elseif ($avgTime -lt 500) {
            Write-Pass "Good performance! 👍"
        }
        else {
            Write-Info "Performance could be improved. Consider checking system resources."
        }
    }
    else {
        Write-Fail "All requests failed!"
    }
    
    Write-Host ""
}

# Main script execution
param(
    [Parameter(Position=0)]
    [ValidateSet("test", "perf", "all")]
    [string]$Command = "test"
)

switch ($Command) {
    "test" {
        Run-Tests
    }
    "perf" {
        Run-PerformanceTest
    }
    "all" {
        Run-Tests
        Run-PerformanceTest
    }
    default {
        Write-Host "Usage: .\test-api.ps1 {test|perf|all}" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "Commands:" -ForegroundColor Yellow
        Write-Host "  test  Run API functionality tests"
        Write-Host "  perf  Run basic performance test"
        Write-Host "  all   Run both functionality and performance tests"
        Write-Host ""
    }
}