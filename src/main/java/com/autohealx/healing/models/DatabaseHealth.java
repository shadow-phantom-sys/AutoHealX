package com.autohealx.healing.models;

import com.autohealx.model.enums.HealthStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseHealth {
    
    private HealthStatus status;
    private LocalDateTime lastChecked;
    
    private int activeConnections;
    private int maxConnections;
    private double connectionPoolUsage;
    
    private long averageQueryTime;
    private long slowQueryCount;
    private long deadlockCount;
    
    private boolean isConnected;
    private String lastError;
    
    public boolean isConnectionPoolHealthy() {
        return connectionPoolUsage < 0.8; // Less than 80% usage
    }
    
    public boolean hasSlowQueries() {
        return averageQueryTime > 1000; // More than 1 second
    }
    
    public boolean hasDeadlocks() {
        return deadlockCount > 0;
    }
}