package com.autohealx.healing.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictionResult {
    
    private LocalDateTime timestamp;
    private double confidence;
    private String predictionType;
    
    private List<String> riskFactors;
    private Map<String, Double> riskScores;
    private Map<String, Object> features;
    
    private LocalDateTime predictedFailureTime;
    private String recommendedAction;
    
    public boolean hasHighRiskPredictions() {
        return confidence > 0.7 && !riskFactors.isEmpty();
    }
    
    public boolean requiresImmediateAction() {
        return confidence > 0.9;
    }
    
    public String getHighestRiskFactor() {
        return riskScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }
}