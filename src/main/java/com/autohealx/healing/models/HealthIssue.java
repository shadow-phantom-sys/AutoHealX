package com.autohealx.healing.models;

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
public class HealthIssue {
    
    private String type;
    private String category;
    private String severity;
    private String description;
    
    private LocalDateTime detectedAt;
    private LocalDateTime lastOccurrence;
    
    private Map<String, Object> context;
    private Map<String, Object> metrics;
    
    private boolean isRecurring;
    private int occurrenceCount;
    
    public boolean isCritical() {
        return "CRITICAL".equalsIgnoreCase(severity);
    }
    
    public boolean isWarning() {
        return "WARNING".equalsIgnoreCase(severity);
    }
    
    public boolean isRecent() {
        return detectedAt.isAfter(LocalDateTime.now().minusMinutes(5));
    }
}