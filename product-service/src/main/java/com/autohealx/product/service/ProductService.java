package com.autohealx.product.service;

import com.autohealx.product.entity.ProductEntity;
import com.autohealx.product.mapper.ProductMapper;
import com.autohealx.product.repository.ProductRepository;
import com.autohealx.shared.domain.Product;
import com.autohealx.shared.exception.BusinessException;
import com.autohealx.shared.exception.ResourceNotFoundException;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {
    
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    
    @Cacheable(value = "products", key = "#id")
    @Timed(value = "product.get", description = "Time taken to get a product")
    public Product getProductById(Long id) {
        log.debug("Fetching product with id: {}", id);
        
        ProductEntity entity = productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        
        return productMapper.toProduct(entity);
    }
    
    @Timed(value = "product.search", description = "Time taken to search products")
    public Page<Product> getAllProducts(Pageable pageable) {
        log.debug("Fetching all active products with pagination: {}", pageable);
        
        return productRepository.findByActiveTrue(pageable)
                .map(productMapper::toProduct);
    }
    
    @Timed(value = "product.search.category", description = "Time taken to search products by category")
    public Page<Product> getProductsByCategory(String category, Pageable pageable) {
        log.debug("Fetching products by category: {} with pagination: {}", category, pageable);
        
        return productRepository.findByCategoryAndActiveTrue(category, pageable)
                .map(productMapper::toProduct);
    }
    
    @Timed(value = "product.search.name", description = "Time taken to search products by name")
    public Page<Product> searchProductsByName(String name, Pageable pageable) {
        log.debug("Searching products by name: {} with pagination: {}", name, pageable);
        
        return productRepository.findByNameContainingIgnoreCaseAndActiveTrue(name, pageable)
                .map(productMapper::toProduct);
    }
    
    @Transactional
    @CacheEvict(value = "products", key = "#result.id")
    @Counted(value = "product.created", description = "Number of products created")
    @Timed(value = "product.create", description = "Time taken to create a product")
    public Product createProduct(Product product) {
        log.info("Creating new product: {}", product.getName());
        
        ProductEntity entity = productMapper.toEntity(product);
        entity.setId(null); // Ensure it's a new entity
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        
        ProductEntity savedEntity = productRepository.save(entity);
        
        log.info("Product created successfully with id: {}", savedEntity.getId());
        return productMapper.toProduct(savedEntity);
    }
    
    @Transactional
    @CacheEvict(value = "products", key = "#id")
    @Timed(value = "product.update", description = "Time taken to update a product")
    public Product updateProduct(Long id, Product product) {
        log.info("Updating product with id: {}", id);
        
        ProductEntity existingEntity = productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        
        // Update fields
        existingEntity.setName(product.getName());
        existingEntity.setDescription(product.getDescription());
        existingEntity.setPrice(product.getPrice());
        existingEntity.setStockQuantity(product.getStockQuantity());
        existingEntity.setCategory(product.getCategory());
        existingEntity.setImageUrl(product.getImageUrl());
        existingEntity.setUpdatedAt(LocalDateTime.now());
        
        ProductEntity savedEntity = productRepository.save(existingEntity);
        
        log.info("Product updated successfully with id: {}", savedEntity.getId());
        return productMapper.toProduct(savedEntity);
    }
    
    @Transactional
    @CacheEvict(value = "products", key = "#id")
    @Timed(value = "product.delete", description = "Time taken to delete a product")
    public void deleteProduct(Long id) {
        log.info("Soft deleting product with id: {}", id);
        
        ProductEntity entity = productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        
        entity.setActive(false);
        entity.setUpdatedAt(LocalDateTime.now());
        productRepository.save(entity);
        
        log.info("Product soft deleted successfully with id: {}", id);
    }
    
    @Transactional
    @CacheEvict(value = "products", key = "#productId")
    @Timed(value = "product.stock.reduce", description = "Time taken to reduce product stock")
    public boolean reduceStock(Long productId, Integer quantity) {
        log.debug("Reducing stock for product {} by quantity {}", productId, quantity);
        
        int updatedRows = productRepository.reduceStock(productId, quantity);
        
        if (updatedRows == 0) {
            log.warn("Failed to reduce stock for product {} - insufficient stock or product not found", productId);
            return false;
        }
        
        log.info("Stock reduced successfully for product {} by quantity {}", productId, quantity);
        return true;
    }
    
    @Transactional
    @CacheEvict(value = "products", key = "#productId")
    @Timed(value = "product.stock.increase", description = "Time taken to increase product stock")
    public void increaseStock(Long productId, Integer quantity) {
        log.debug("Increasing stock for product {} by quantity {}", productId, quantity);
        
        int updatedRows = productRepository.increaseStock(productId, quantity);
        
        if (updatedRows == 0) {
            throw new ResourceNotFoundException("Product", "id", productId);
        }
        
        log.info("Stock increased successfully for product {} by quantity {}", productId, quantity);
    }
    
    @Timed(value = "product.stock.check", description = "Time taken to check product stock")
    public boolean checkStockAvailability(Long productId, Integer requiredQuantity) {
        log.debug("Checking stock availability for product {} with required quantity {}", productId, requiredQuantity);
        
        Product product = getProductById(productId);
        boolean available = product.canFulfillQuantity(requiredQuantity);
        
        log.debug("Stock availability for product {}: {}", productId, available);
        return available;
    }
    
    @Timed(value = "product.lowstock", description = "Time taken to get low stock products")
    public List<Product> getLowStockProducts(Integer threshold) {
        log.debug("Fetching products with stock below threshold: {}", threshold);
        
        List<ProductEntity> entities = productRepository.findLowStockProducts(threshold);
        
        return entities.stream()
                .map(productMapper::toProduct)
                .collect(Collectors.toList());
    }
    
    public List<String> getAllCategories() {
        log.debug("Fetching all product categories");
        return productRepository.findAllCategories();
    }
}