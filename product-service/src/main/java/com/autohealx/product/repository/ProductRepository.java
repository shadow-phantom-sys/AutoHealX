package com.autohealx.product.repository;

import com.autohealx.product.entity.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {
    
    // Find active products
    Page<ProductEntity> findByActiveTrue(Pageable pageable);
    
    // Find by category
    Page<ProductEntity> findByCategoryAndActiveTrue(String category, Pageable pageable);
    
    // Search by name
    @Query("SELECT p FROM ProductEntity p WHERE p.active = true AND LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<ProductEntity> findByNameContainingIgnoreCaseAndActiveTrue(@Param("name") String name, Pageable pageable);
    
    // Find products with low stock
    @Query("SELECT p FROM ProductEntity p WHERE p.active = true AND p.stockQuantity <= :threshold")
    List<ProductEntity> findLowStockProducts(@Param("threshold") Integer threshold);
    
    // Update stock quantity with optimistic locking
    @Modifying
    @Query("UPDATE ProductEntity p SET p.stockQuantity = p.stockQuantity - :quantity, p.updatedAt = CURRENT_TIMESTAMP WHERE p.id = :productId AND p.stockQuantity >= :quantity")
    int reduceStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);
    
    @Modifying
    @Query("UPDATE ProductEntity p SET p.stockQuantity = p.stockQuantity + :quantity, p.updatedAt = CURRENT_TIMESTAMP WHERE p.id = :productId")
    int increaseStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);
    
    // Find by ID and active
    Optional<ProductEntity> findByIdAndActiveTrue(Long id);
    
    // Get all categories
    @Query("SELECT DISTINCT p.category FROM ProductEntity p WHERE p.active = true AND p.category IS NOT NULL")
    List<String> findAllCategories();
}