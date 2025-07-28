package com.autohealx.product.mapper;

import com.autohealx.product.entity.ProductEntity;
import com.autohealx.shared.domain.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {
    
    public Product toProduct(ProductEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return Product.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .price(entity.getPrice())
                .stockQuantity(entity.getStockQuantity())
                .category(entity.getCategory())
                .imageUrl(entity.getImageUrl())
                .active(entity.getActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
    
    public ProductEntity toEntity(Product product) {
        if (product == null) {
            return null;
        }
        
        return ProductEntity.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .category(product.getCategory())
                .imageUrl(product.getImageUrl())
                .active(product.isActive())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}