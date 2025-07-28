package com.autohealx.healing.models;

import com.autohealx.model.enums.HealthStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemHealth {
    
    private HealthStatus overallStatus;
    private LocalDateTime timestamp;
    
    private DatabaseHealth databaseHealth;
    private MemoryHealth memoryHealth;
    private ResponseTimeHealth responseTimeHealth;
    
    private double cpuUsage;
    private double diskUsage;
    private long activeConnections;
    private long requestsPerSecond;
    
    private Map<String, Object> customMetrics;
    
    public boolean isHealthy() {
        return overallStatus == HealthStatus.HEALTHY;
    }
    
    public boolean requiresAttention() {
        return overallStatus == HealthStatus.WARNING || 
               overallStatus == HealthStatus.CRITICAL;
    }
    
    public boolean isCritical() {
        return overallStatus == HealthStatus.CRITICAL || 
               overallStatus == HealthStatus.DOWN;
    }
}