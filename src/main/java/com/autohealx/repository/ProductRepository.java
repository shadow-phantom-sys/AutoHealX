package com.autohealx.repository;

import com.autohealx.model.Product;
import com.autohealx.model.enums.ProductCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);
    
    boolean existsBySku(String sku);
    
    Page<Product> findByActiveTrue(Pageable pageable);
    
    Page<Product> findByActiveFalse(Pageable pageable);
    
    Page<Product> findByCategoryAndActiveTrue(ProductCategory category, Pageable pageable);
    
    Page<Product> findByStockQuantityLessThanAndActiveTrue(int stockQuantity, Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.active = true AND p.name LIKE %:name%")
    Page<Product> findByNameContainingAndActiveTrue(@Param("name") String name, Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.active = true AND p.price BETWEEN :minPrice AND :maxPrice")
    Page<Product> findByPriceBetweenAndActiveTrue(@Param("minPrice") double minPrice, 
                                                  @Param("maxPrice") double maxPrice, 
                                                  Pageable pageable);
    
    @Query("SELECT COUNT(p) FROM Product p WHERE p.active = true AND p.stockQuantity <= 10")
    long countLowStockProducts();
    
    @Query("SELECT COUNT(p) FROM Product p WHERE p.active = true AND p.stockQuantity = 0")
    long countOutOfStockProducts();
}