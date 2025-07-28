package com.autohealx.product.controller;

import com.autohealx.product.service.ProductService;
import com.autohealx.shared.domain.Product;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Validated
public class ProductController {
    
    private final ProductService productService;
    
    @GetMapping("/{id}")
    @Timed(value = "product.controller.get", description = "Time taken for product GET endpoint")
    public ResponseEntity<Product> getProduct(@PathVariable @NotNull Long id) {
        log.info("GET /api/v1/products/{}", id);
        Product product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }
    
    @GetMapping
    @Timed(value = "product.controller.list", description = "Time taken for product list endpoint")
    public ResponseEntity<Page<Product>> getAllProducts(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search) {
        
        log.info("GET /api/v1/products - page: {}, size: {}, category: {}, search: {}", 
                page, size, category, search);
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Product> products;
        
        if (search != null && !search.trim().isEmpty()) {
            products = productService.searchProductsByName(search.trim(), pageable);
        } else if (category != null && !category.trim().isEmpty()) {
            products = productService.getProductsByCategory(category.trim(), pageable);
        } else {
            products = productService.getAllProducts(pageable);
        }
        
        return ResponseEntity.ok(products);
    }
    
    @PostMapping
    @Timed(value = "product.controller.create", description = "Time taken for product creation endpoint")
    public ResponseEntity<Product> createProduct(@Valid @RequestBody Product product) {
        log.info("POST /api/v1/products - Creating product: {}", product.getName());
        Product createdProduct = productService.createProduct(product);
        return new ResponseEntity<>(createdProduct, HttpStatus.CREATED);
    }
    
    @PutMapping("/{id}")
    @Timed(value = "product.controller.update", description = "Time taken for product update endpoint")
    public ResponseEntity<Product> updateProduct(
            @PathVariable @NotNull Long id, 
            @Valid @RequestBody Product product) {
        log.info("PUT /api/v1/products/{} - Updating product", id);
        Product updatedProduct = productService.updateProduct(id, product);
        return ResponseEntity.ok(updatedProduct);
    }
    
    @DeleteMapping("/{id}")
    @Timed(value = "product.controller.delete", description = "Time taken for product deletion endpoint")
    public ResponseEntity<Void> deleteProduct(@PathVariable @NotNull Long id) {
        log.info("DELETE /api/v1/products/{}", id);
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/{id}/stock/reduce")
    @Timed(value = "product.controller.stock.reduce", description = "Time taken for stock reduction endpoint")
    public ResponseEntity<Map<String, Object>> reduceStock(
            @PathVariable @NotNull Long id,
            @RequestBody @Valid StockRequest request) {
        log.info("POST /api/v1/products/{}/stock/reduce - quantity: {}", id, request.getQuantity());
        
        boolean success = productService.reduceStock(id, request.getQuantity());
        
        Map<String, Object> response = Map.of(
                "success", success,
                "productId", id,
                "quantityReduced", success ? request.getQuantity() : 0
        );
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{id}/stock/increase")
    @Timed(value = "product.controller.stock.increase", description = "Time taken for stock increase endpoint")
    public ResponseEntity<Map<String, Object>> increaseStock(
            @PathVariable @NotNull Long id,
            @RequestBody @Valid StockRequest request) {
        log.info("POST /api/v1/products/{}/stock/increase - quantity: {}", id, request.getQuantity());
        
        productService.increaseStock(id, request.getQuantity());
        
        Map<String, Object> response = Map.of(
                "success", true,
                "productId", id,
                "quantityIncreased", request.getQuantity()
        );
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}/stock/check")
    @Timed(value = "product.controller.stock.check", description = "Time taken for stock check endpoint")
    public ResponseEntity<Map<String, Object>> checkStock(
            @PathVariable @NotNull Long id,
            @RequestParam @Min(1) Integer quantity) {
        log.info("GET /api/v1/products/{}/stock/check - required quantity: {}", id, quantity);
        
        boolean available = productService.checkStockAvailability(id, quantity);
        
        Map<String, Object> response = Map.of(
                "productId", id,
                "requiredQuantity", quantity,
                "available", available
        );
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/low-stock")
    @Timed(value = "product.controller.lowstock", description = "Time taken for low stock endpoint")
    public ResponseEntity<List<Product>> getLowStockProducts(
            @RequestParam(defaultValue = "10") @Min(0) Integer threshold) {
        log.info("GET /api/v1/products/low-stock - threshold: {}", threshold);
        
        List<Product> lowStockProducts = productService.getLowStockProducts(threshold);
        return ResponseEntity.ok(lowStockProducts);
    }
    
    @GetMapping("/categories")
    @Timed(value = "product.controller.categories", description = "Time taken for categories endpoint")
    public ResponseEntity<List<String>> getCategories() {
        log.info("GET /api/v1/products/categories");
        List<String> categories = productService.getAllCategories();
        return ResponseEntity.ok(categories);
    }
    
    // Inner class for stock request
    @Validated
    public static class StockRequest {
        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        private Integer quantity;
        
        public Integer getQuantity() {
            return quantity;
        }
        
        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }
}