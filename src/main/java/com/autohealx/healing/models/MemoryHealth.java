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
public class MemoryHealth {
    
    private HealthStatus status;
    private LocalDateTime lastChecked;
    
    private long usedMemory;
    private long maxMemory;
    private double memoryUsagePercentage;
    
    private long gcCount;
    private long gcTime;
    private double gcPressure;
    
    private long heapUsed;
    private long heapMax;
    private long nonHeapUsed;
    private long nonHeapMax;
    
    public boolean isMemoryPressureHigh() {
        return memoryUsagePercentage > 0.85; // More than 85% usage
    }
    
    public boolean isGcPressureHigh() {
        return gcPressure > 0.1; // More than 10% time spent in GC
    }
    
    public boolean requiresMemoryCleanup() {
        return isMemoryPressureHigh() || isGcPressureHigh();
    }
}