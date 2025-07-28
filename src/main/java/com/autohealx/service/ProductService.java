package com.autohealx.service;

import com.autohealx.model.Product;
import com.autohealx.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    @Cacheable(value = "products", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<Product> getAllProducts(Pageable pageable) {
        log.debug("Fetching all products with pagination");
        return productRepository.findByActiveTrue(pageable);
    }

    @Cacheable(value = "product", key = "#id")
    public Product getProductById(Long id) {
        log.debug("Fetching product by id: {}", id);
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
    }

    @Cacheable(value = "product", key = "#sku")
    public Product getProductBySku(String sku) {
        log.debug("Fetching product by SKU: {}", sku);
        return productRepository.findBySku(sku)
                .orElseThrow(() -> new RuntimeException("Product not found with SKU: " + sku));
    }

    @Transactional
    @CacheEvict(value = {"products", "product"}, allEntries = true)
    public Product createProduct(Product product) {
        log.info("Creating new product: {}", product.getName());
        
        // Check if SKU already exists
        if (productRepository.existsBySku(product.getSku())) {
            throw new RuntimeException("Product with SKU already exists: " + product.getSku());
        }
        
        return productRepository.save(product);
    }

    @Transactional
    @CacheEvict(value = {"products", "product"}, allEntries = true)
    public Product updateProduct(Long id, Product productDetails) {
        log.info("Updating product with id: {}", id);
        
        Product product = getProductById(id);
        
        product.setName(productDetails.getName());
        product.setDescription(productDetails.getDescription());
        product.setPrice(productDetails.getPrice());
        product.setCategory(productDetails.getCategory());
        product.setStockQuantity(productDetails.getStockQuantity());
        product.setImageUrl(productDetails.getImageUrl());
        product.setActive(productDetails.getActive());
        product.setWeight(productDetails.getWeight());
        product.setTags(productDetails.getTags());
        
        return productRepository.save(product);
    }

    @Transactional
    @CacheEvict(value = {"products", "product"}, allEntries = true)
    public void deleteProduct(Long id) {
        log.info("Deleting product with id: {}", id);
        
        Product product = getProductById(id);
        product.setActive(false); // Soft delete
        productRepository.save(product);
    }

    @Transactional
    @CacheEvict(value = {"products", "product"}, key = "#id")
    public Product updateStock(Long id, int newQuantity) {
        log.info("Updating stock for product id: {} to quantity: {}", id, newQuantity);
        
        Product product = getProductById(id);
        product.setStockQuantity(newQuantity);
        
        return productRepository.save(product);
    }

    @Transactional
    @CacheEvict(value = {"products", "product"}, key = "#id")
    public Product decreaseStock(Long id, int quantity) {
        log.info("Decreasing stock for product id: {} by quantity: {}", id, quantity);
        
        Product product = getProductById(id);
        
        if (product.getStockQuantity() < quantity) {
            throw new RuntimeException("Insufficient stock for product: " + product.getName());
        }
        
        product.decreaseStock(quantity);
        return productRepository.save(product);
    }

    @Transactional
    @CacheEvict(value = {"products", "product"}, key = "#id")
    public Product increaseStock(Long id, int quantity) {
        log.info("Increasing stock for product id: {} by quantity: {}", id, quantity);
        
        Product product = getProductById(id);
        product.increaseStock(quantity);
        
        return productRepository.save(product);
    }

    public Page<Product> getLowStockProducts(Pageable pageable) {
        log.debug("Fetching low stock products");
        return productRepository.findByStockQuantityLessThanAndActiveTrue(10, pageable);
    }

    public Page<Product> getProductsByCategory(String category, Pageable pageable) {
        log.debug("Fetching products by category: {}", category);
        return productRepository.findByCategoryAndActiveTrue(
                com.autohealx.model.enums.ProductCategory.valueOf(category.toUpperCase()), 
                pageable);
    }

    public boolean isProductInStock(Long id) {
        Product product = getProductById(id);
        return product.isInStock();
    }

    public boolean isProductLowStock(Long id) {
        Product product = getProductById(id);
        return product.isLowStock();
    }
}