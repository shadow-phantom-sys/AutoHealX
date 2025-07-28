package com.autohealx.shared.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryEvent {
    
    private String eventId;
    
    private Long productId;
    
    private Integer quantityChange;
    
    private EventType eventType;
    
    private String reason;
    
    private Long orderId;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    
    public enum EventType {
        STOCK_RESERVED,
        STOCK_RELEASED,
        STOCK_REPLENISHED,
        STOCK_ADJUSTED
    }
    
    public static InventoryEvent stockReserved(Long productId, Integer quantity, Long orderId) {
        return InventoryEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .productId(productId)
                .quantityChange(-quantity)
                .eventType(EventType.STOCK_RESERVED)
                .reason("Stock reserved for order")
                .orderId(orderId)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static InventoryEvent stockReleased(Long productId, Integer quantity, Long orderId) {
        return InventoryEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .productId(productId)
                .quantityChange(quantity)
                .eventType(EventType.STOCK_RELEASED)
                .reason("Stock released from cancelled order")
                .orderId(orderId)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static InventoryEvent stockReplenished(Long productId, Integer quantity) {
        return InventoryEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .productId(productId)
                .quantityChange(quantity)
                .eventType(EventType.STOCK_REPLENISHED)
                .reason("Stock replenishment")
                .timestamp(LocalDateTime.now())
                .build();
    }
}