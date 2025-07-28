#!/bin/bash

# AutoHealX API Test Script
# This script tests the Product Service API to verify the system is working correctly

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
BASE_URL="http://localhost:80/api/v1"
PRODUCT_SERVICE_URL="http://localhost:8081/api/v1"

# Function to print colored output
print_test() {
    echo -e "${BLUE}[TEST]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[PASS]${NC} $1"
}

print_error() {
    echo -e "${RED}[FAIL]${NC} $1"
}

print_info() {
    echo -e "${YELLOW}[INFO]${NC} $1"
}

# Function to check if service is available
check_service() {
    local url=$1
    local service_name=$2
    
    print_test "Checking $service_name availability..."
    
    if curl -f -s "$url/health" >/dev/null 2>&1 || curl -f -s "$url/actuator/health" >/dev/null 2>&1; then
        print_success "$service_name is available"
        return 0
    else
        print_error "$service_name is not available"
        return 1
    fi
}

# Function to test API endpoint
test_api() {
    local method=$1
    local endpoint=$2
    local data=$3
    local expected_status=$4
    local description=$5
    
    print_test "$description"
    
    local curl_cmd="curl -s -w '%{http_code}' -X $method"
    
    if [ -n "$data" ]; then
        curl_cmd="$curl_cmd -H 'Content-Type: application/json' -d '$data'"
    fi
    
    curl_cmd="$curl_cmd $BASE_URL$endpoint"
    
    local response=$(eval $curl_cmd)
    local status_code="${response: -3}"
    local body="${response%???}"
    
    if [ "$status_code" = "$expected_status" ]; then
        print_success "Status: $status_code"
        if [ -n "$body" ] && [ "$body" != "null" ]; then
            echo "Response: $body" | jq . 2>/dev/null || echo "Response: $body"
        fi
        echo ""
        return 0
    else
        print_error "Expected: $expected_status, Got: $status_code"
        if [ -n "$body" ]; then
            echo "Response: $body"
        fi
        echo ""
        return 1
    fi
}

# Function to extract ID from JSON response
extract_id() {
    echo "$1" | jq -r '.id' 2>/dev/null || echo ""
}

# Main test suite
run_tests() {
    echo "🧪 AutoHealX API Test Suite"
    echo "=========================="
    echo ""
    
    # Check service availability
    local services_ok=true
    
    if ! check_service "http://localhost:80" "Nginx Load Balancer"; then
        services_ok=false
    fi
    
    if ! check_service "$PRODUCT_SERVICE_URL" "Product Service"; then
        services_ok=false
    fi
    
    if [ "$services_ok" = false ]; then
        print_error "Some services are not available. Please check your deployment."
        exit 1
    fi
    
    echo ""
    print_info "Starting API tests..."
    echo ""
    
    local failed_tests=0
    local total_tests=0
    
    # Test 1: Get all products (should return sample data)
    ((total_tests++))
    if test_api "GET" "/products" "" "200" "Get all products"; then
        :
    else
        ((failed_tests++))
    fi
    
    # Test 2: Get specific product
    ((total_tests++))
    if test_api "GET" "/products/1" "" "200" "Get product by ID"; then
        :
    else
        ((failed_tests++))
    fi
    
    # Test 3: Get non-existent product
    ((total_tests++))
    if test_api "GET" "/products/99999" "" "404" "Get non-existent product (should fail)"; then
        :
    else
        ((failed_tests++))
    fi
    
    # Test 4: Create new product
    local new_product_data='{
        "name": "API Test Product",
        "description": "Created by API test script",
        "price": 199.99,
        "stockQuantity": 50,
        "category": "Test",
        "imageUrl": "https://example.com/test.jpg"
    }'
    
    ((total_tests++))
    local create_response=$(curl -s -X POST -H 'Content-Type: application/json' -d "$new_product_data" "$BASE_URL/products")
    local create_status=$(curl -s -w '%{http_code}' -o /dev/null -X POST -H 'Content-Type: application/json' -d "$new_product_data" "$BASE_URL/products")
    
    print_test "Create new product"
    if [ "$create_status" = "201" ]; then
        print_success "Status: 201"
        local product_id=$(extract_id "$create_response")
        echo "Created product ID: $product_id"
        echo "Response: $create_response" | jq . 2>/dev/null || echo "Response: $create_response"
        echo ""
        
        # Test 5: Update the created product
        if [ -n "$product_id" ] && [ "$product_id" != "null" ]; then
            local update_data='{
                "name": "Updated API Test Product",
                "description": "Updated by API test script",
                "price": 299.99,
                "stockQuantity": 75,
                "category": "Test Updated",
                "imageUrl": "https://example.com/test-updated.jpg"
            }'
            
            ((total_tests++))
            if test_api "PUT" "/products/$product_id" "$update_data" "200" "Update created product"; then
                :
            else
                ((failed_tests++))
            fi
            
            # Test 6: Reduce stock
            ((total_tests++))
            if test_api "POST" "/products/$product_id/stock/reduce" '{"quantity": 10}' "200" "Reduce product stock"; then
                :
            else
                ((failed_tests++))
            fi
            
            # Test 7: Increase stock
            ((total_tests++))
            if test_api "POST" "/products/$product_id/stock/increase" '{"quantity": 5}' "200" "Increase product stock"; then
                :
            else
                ((failed_tests++))
            fi
            
            # Test 8: Check stock availability
            ((total_tests++))
            if test_api "GET" "/products/$product_id/stock/check?quantity=20" "" "200" "Check stock availability"; then
                :
            else
                ((failed_tests++))
            fi
            
            # Test 9: Delete the created product
            ((total_tests++))
            if test_api "DELETE" "/products/$product_id" "" "204" "Delete created product"; then
                :
            else
                ((failed_tests++))
            fi
        fi
    else
        print_error "Expected: 201, Got: $create_status"
        ((failed_tests++))
    fi
    
    # Test 10: Get product categories
    ((total_tests++))
    if test_api "GET" "/products/categories" "" "200" "Get product categories"; then
        :
    else
        ((failed_tests++))
    fi
    
    # Test 11: Get low stock products
    ((total_tests++))
    if test_api "GET" "/products/low-stock?threshold=10" "" "200" "Get low stock products"; then
        :
    else
        ((failed_tests++))
    fi
    
    # Test 12: Search products by name
    ((total_tests++))
    if test_api "GET" "/products?search=iPhone" "" "200" "Search products by name"; then
        :
    else
        ((failed_tests++))
    fi
    
    # Test 13: Filter products by category
    ((total_tests++))
    if test_api "GET" "/products?category=Electronics" "" "200" "Filter products by category"; then
        :
    else
        ((failed_tests++))
    fi
    
    # Test 14: Test pagination
    ((total_tests++))
    if test_api "GET" "/products?page=0&size=5" "" "200" "Test pagination"; then
        :
    else
        ((failed_tests++))
    fi
    
    # Test 15: Invalid data validation
    ((total_tests++))
    local invalid_data='{"name": "", "price": -10, "stockQuantity": -5}'
    if test_api "POST" "/products" "$invalid_data" "400" "Test validation (should fail)"; then
        :
    else
        ((failed_tests++))
    fi
    
    # Summary
    echo ""
    echo "🏁 Test Results Summary"
    echo "======================"
    echo "Total tests: $total_tests"
    echo "Passed: $((total_tests - failed_tests))"
    echo "Failed: $failed_tests"
    echo ""
    
    if [ $failed_tests -eq 0 ]; then
        print_success "All tests passed! 🎉"
        echo ""
        echo "✅ Your AutoHealX system is working correctly!"
        echo ""
        echo "📊 Next steps:"
        echo "  • Open Grafana: http://localhost:3000 (admin/admin123)"
        echo "  • View metrics: http://localhost:9090"
        echo "  • Run AI monitor: python3 monitoring/ai-health-monitor.py"
        echo "  • Load test: k6 run load-test.js"
        echo ""
        return 0
    else
        print_error "Some tests failed. Please check your deployment."
        echo ""
        echo "🔧 Debugging tips:"
        echo "  • Check logs: docker-compose logs product-service"
        echo "  • Verify health: curl http://localhost:8081/actuator/health"
        echo "  • Check database: docker-compose exec mysql mysqladmin ping"
        echo ""
        return 1
    fi
}

# Performance test function
run_performance_test() {
    echo "🚀 Running basic performance test..."
    echo ""
    
    print_test "Measuring response times for 10 requests..."
    
    local total_time=0
    local successful_requests=0
    
    for i in {1..10}; do
        local start_time=$(date +%s%N)
        local status=$(curl -s -w '%{http_code}' -o /dev/null "$BASE_URL/products")
        local end_time=$(date +%s%N)
        
        if [ "$status" = "200" ]; then
            local response_time=$(( (end_time - start_time) / 1000000 ))  # Convert to milliseconds
            total_time=$((total_time + response_time))
            successful_requests=$((successful_requests + 1))
            echo "Request $i: ${response_time}ms"
        else
            echo "Request $i: Failed (Status: $status)"
        fi
    done
    
    if [ $successful_requests -gt 0 ]; then
        local avg_time=$((total_time / successful_requests))
        echo ""
        print_success "Average response time: ${avg_time}ms"
        print_success "Success rate: $successful_requests/10"
        
        if [ $avg_time -lt 100 ]; then
            print_success "Excellent performance! 🚀"
        elif [ $avg_time -lt 500 ]; then
            print_success "Good performance! 👍"
        else
            print_info "Performance could be improved. Consider checking system resources."
        fi
    else
        print_error "All requests failed!"
        return 1
    fi
    
    echo ""
}

# Main script execution
case "${1:-test}" in
    test)
        run_tests
        ;;
    perf)
        run_performance_test
        ;;
    all)
        run_tests && run_performance_test
        ;;
    *)
        echo "Usage: $0 {test|perf|all}"
        echo ""
        echo "Commands:"
        echo "  test  Run API functionality tests"
        echo "  perf  Run basic performance test"
        echo "  all   Run both functionality and performance tests"
        echo ""
        exit 1
        ;;
esac