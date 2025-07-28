package com.autohealx.healing;

import com.autohealx.healing.models.HealthIssue;
import com.autohealx.healing.models.PredictionResult;
import com.autohealx.healing.models.SystemHealth;
import com.autohealx.healing.strategies.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class SelfHealingEngine {

    private final HealthMonitor healthMonitor;
    private final AIPredictiveAnalyzer aiAnalyzer;
    private final AutoRepairService autoRepairService;
    private final MetricsCollector metricsCollector;
    
    private final DatabaseHealingStrategy databaseHealingStrategy;
    private final MemoryHealingStrategy memoryHealingStrategy;
    private final NetworkHealingStrategy networkHealingStrategy;
    private final ApplicationHealingStrategy applicationHealingStrategy;

    private final Map<String, LocalDateTime> lastHealingAttempts = new ConcurrentHashMap<>();
    private final Map<String, Integer> healingAttemptCounts = new ConcurrentHashMap<>();
    
    private static final int MAX_HEALING_ATTEMPTS = 3;
    private static final long HEALING_COOLDOWN_MINUTES = 5;

    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void performHealthCheck() {
        log.debug("Starting scheduled health check");
        
        try {
            SystemHealth systemHealth = healthMonitor.getCurrentSystemHealth();
            metricsCollector.recordHealthMetrics(systemHealth);
            
            // Check for immediate issues
            List<HealthIssue> currentIssues = healthMonitor.detectIssues(systemHealth);
            
            if (!currentIssues.isEmpty()) {
                log.warn("Detected {} health issues", currentIssues.size());
                handleHealthIssues(currentIssues);
            }
            
            // Predictive analysis
            performPredictiveAnalysis(systemHealth);
            
        } catch (Exception e) {
            log.error("Error during health check", e);
            metricsCollector.recordHealingFailure("health_check_error", e.getMessage());
        }
    }

    @Async
    public CompletableFuture<Void> handleHealthIssues(List<HealthIssue> issues) {
        for (HealthIssue issue : issues) {
            try {
                if (shouldAttemptHealing(issue)) {
                    log.info("Attempting to heal issue: {}", issue.getType());
                    
                    boolean healingResult = attemptHealing(issue);
                    
                    if (healingResult) {
                        log.info("Successfully healed issue: {}", issue.getType());
                        resetHealingAttempts(issue.getType());
                        metricsCollector.recordSuccessfulHealing(issue.getType());
                    } else {
                        log.warn("Failed to heal issue: {}", issue.getType());
                        incrementHealingAttempts(issue.getType());
                        metricsCollector.recordHealingFailure(issue.getType(), "healing_failed");
                    }
                } else {
                    log.debug("Skipping healing for issue: {} (cooldown or max attempts)", issue.getType());
                }
            } catch (Exception e) {
                log.error("Error healing issue: {}", issue.getType(), e);
                metricsCollector.recordHealingFailure(issue.getType(), e.getMessage());
            }
        }
        
        return CompletableFuture.completedFuture(null);
    }

    private void performPredictiveAnalysis(SystemHealth systemHealth) {
        try {
            PredictionResult prediction = aiAnalyzer.predictFailures(systemHealth);
            
            if (prediction.hasHighRiskPredictions()) {
                log.warn("AI detected high risk predictions: {}", prediction.getRiskFactors());
                
                // Proactive healing based on predictions
                for (String riskFactor : prediction.getRiskFactors()) {
                    performProactiveHealing(riskFactor, prediction.getConfidence());
                }
            }
            
            metricsCollector.recordPredictionMetrics(prediction);
            
        } catch (Exception e) {
            log.error("Error during predictive analysis", e);
        }
    }

    private boolean attemptHealing(HealthIssue issue) {
        switch (issue.getCategory()) {
            case "DATABASE":
                return databaseHealingStrategy.heal(issue);
            case "MEMORY":
                return memoryHealingStrategy.heal(issue);
            case "NETWORK":
                return networkHealingStrategy.heal(issue);
            case "APPLICATION":
                return applicationHealingStrategy.heal(issue);
            default:
                log.warn("No healing strategy found for issue category: {}", issue.getCategory());
                return false;
        }
    }

    private void performProactiveHealing(String riskFactor, double confidence) {
        if (confidence > 0.8) { // High confidence threshold
            log.info("Performing proactive healing for risk factor: {} (confidence: {})", 
                    riskFactor, confidence);
            
            switch (riskFactor) {
                case "MEMORY_PRESSURE":
                    autoRepairService.performGarbageCollection();
                    autoRepairService.clearCaches();
                    break;
                case "DATABASE_SLOW":
                    autoRepairService.optimizeDatabaseConnections();
                    break;
                case "HIGH_RESPONSE_TIME":
                    autoRepairService.scaleResources();
                    break;
                default:
                    log.debug("No proactive action defined for risk factor: {}", riskFactor);
            }
            
            metricsCollector.recordProactiveHealing(riskFactor, confidence);
        }
    }

    private boolean shouldAttemptHealing(HealthIssue issue) {
        String issueType = issue.getType();
        
        // Check max attempts
        int attempts = healingAttemptCounts.getOrDefault(issueType, 0);
        if (attempts >= MAX_HEALING_ATTEMPTS) {
            log.warn("Max healing attempts reached for issue: {}", issueType);
            return false;
        }
        
        // Check cooldown
        LocalDateTime lastAttempt = lastHealingAttempts.get(issueType);
        if (lastAttempt != null) {
            LocalDateTime cooldownEnd = lastAttempt.plusMinutes(HEALING_COOLDOWN_MINUTES);
            if (LocalDateTime.now().isBefore(cooldownEnd)) {
                log.debug("Healing cooldown active for issue: {}", issueType);
                return false;
            }
        }
        
        return true;
    }

    private void incrementHealingAttempts(String issueType) {
        healingAttemptCounts.merge(issueType, 1, Integer::sum);
        lastHealingAttempts.put(issueType, LocalDateTime.now());
    }

    private void resetHealingAttempts(String issueType) {
        healingAttemptCounts.remove(issueType);
        lastHealingAttempts.remove(issueType);
    }

    public SystemHealth getCurrentHealth() {
        return healthMonitor.getCurrentSystemHealth();
    }

    public Map<String, Object> getHealingStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("totalHealingAttempts", healingAttemptCounts.values().stream().mapToInt(Integer::intValue).sum());
        stats.put("activeIssues", healingAttemptCounts.size());
        stats.put("lastHealthCheck", LocalDateTime.now());
        return stats;
    }
}