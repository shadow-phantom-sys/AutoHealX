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
public class ResponseTimeHealth {
    
    private HealthStatus status;
    private LocalDateTime lastChecked;
    
    private double averageResponseTime;
    private double p95ResponseTime;
    private double p99ResponseTime;
    
    private long requestCount;
    private long errorCount;
    private double errorRate;
    
    private double throughput; // requests per second
    
    public boolean isResponseTimeSlow() {
        return averageResponseTime > 2000; // More than 2 seconds
    }
    
    public boolean hasHighErrorRate() {
        return errorRate > 0.05; // More than 5% error rate
    }
    
    public boolean isPerformanceDegraded() {
        return isResponseTimeSlow() || hasHighErrorRate();
    }
}