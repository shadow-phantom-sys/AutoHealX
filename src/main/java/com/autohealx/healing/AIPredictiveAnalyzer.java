package com.autohealx.healing;

import com.autohealx.healing.models.PredictionResult;
import com.autohealx.healing.models.SystemHealth;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIPredictiveAnalyzer {

    // Simple ML-like prediction logic (in a real system, this would use actual ML models)
    
    public PredictionResult predictFailures(SystemHealth systemHealth) {
        log.debug("Analyzing system health for failure prediction");
        
        Map<String, Double> riskScores = calculateRiskScores(systemHealth);
        List<String> riskFactors = identifyRiskFactors(riskScores);
        double overallConfidence = calculateOverallConfidence(riskScores);
        
        LocalDateTime predictedFailureTime = null;
        String recommendedAction = null;
        
        if (overallConfidence > 0.8) {
            predictedFailureTime = LocalDateTime.now().plusMinutes(calculateTimeToFailure(riskScores));
            recommendedAction = generateRecommendedAction(riskFactors);
        }
        
        return PredictionResult.builder()
                .timestamp(LocalDateTime.now())
                .confidence(overallConfidence)
                .predictionType("FAILURE_PREDICTION")
                .riskFactors(riskFactors)
                .riskScores(riskScores)
                .features(extractFeatures(systemHealth))
                .predictedFailureTime(predictedFailureTime)
                .recommendedAction(recommendedAction)
                .build();
    }

    private Map<String, Double> calculateRiskScores(SystemHealth systemHealth) {
        Map<String, Double> riskScores = new HashMap<>();
        
        // Memory risk analysis
        if (systemHealth.getMemoryHealth() != null) {
            double memoryRisk = analyzeMemoryRisk(systemHealth.getMemoryHealth());
            riskScores.put("MEMORY_PRESSURE", memoryRisk);
        }
        
        // Database risk analysis
        if (systemHealth.getDatabaseHealth() != null) {
            double dbRisk = analyzeDatabaseRisk(systemHealth.getDatabaseHealth());
            riskScores.put("DATABASE_SLOW", dbRisk);
        }
        
        // Response time risk analysis
        if (systemHealth.getResponseTimeHealth() != null) {
            double responseRisk = analyzeResponseTimeRisk(systemHealth.getResponseTimeHealth());
            riskScores.put("HIGH_RESPONSE_TIME", responseRisk);
        }
        
        // CPU risk analysis
        double cpuRisk = analyzeCpuRisk(systemHealth.getCpuUsage());
        riskScores.put("CPU_PRESSURE", cpuRisk);
        
        // Disk risk analysis
        double diskRisk = analyzeDiskRisk(systemHealth.getDiskUsage());
        riskScores.put("DISK_PRESSURE", diskRisk);
        
        return riskScores;
    }

    private double analyzeMemoryRisk(com.autohealx.healing.models.MemoryHealth memoryHealth) {
        double risk = 0.0;
        
        // Memory usage risk
        if (memoryHealth.getMemoryUsagePercentage() > 0.9) {
            risk += 0.8;
        } else if (memoryHealth.getMemoryUsagePercentage() > 0.8) {
            risk += 0.5;
        } else if (memoryHealth.getMemoryUsagePercentage() > 0.7) {
            risk += 0.3;
        }
        
        // GC pressure risk
        if (memoryHealth.getGcPressure() > 0.2) {
            risk += 0.7;
        } else if (memoryHealth.getGcPressure() > 0.1) {
            risk += 0.4;
        }
        
        return Math.min(risk, 1.0);
    }

    private double analyzeDatabaseRisk(com.autohealx.healing.models.DatabaseHealth dbHealth) {
        double risk = 0.0;
        
        // Connection pool usage risk
        if (dbHealth.getConnectionPoolUsage() > 0.9) {
            risk += 0.7;
        } else if (dbHealth.getConnectionPoolUsage() > 0.8) {
            risk += 0.4;
        }
        
        // Query performance risk
        if (dbHealth.getAverageQueryTime() > 2000) {
            risk += 0.6;
        } else if (dbHealth.getAverageQueryTime() > 1000) {
            risk += 0.3;
        }
        
        // Deadlock risk
        if (dbHealth.getDeadlockCount() > 0) {
            risk += 0.5;
        }
        
        return Math.min(risk, 1.0);
    }

    private double analyzeResponseTimeRisk(com.autohealx.healing.models.ResponseTimeHealth responseHealth) {
        double risk = 0.0;
        
        // Response time risk
        if (responseHealth.getAverageResponseTime() > 5000) {
            risk += 0.8;
        } else if (responseHealth.getAverageResponseTime() > 2000) {
            risk += 0.5;
        } else if (responseHealth.getAverageResponseTime() > 1000) {
            risk += 0.3;
        }
        
        // Error rate risk
        if (responseHealth.getErrorRate() > 0.1) {
            risk += 0.9;
        } else if (responseHealth.getErrorRate() > 0.05) {
            risk += 0.6;
        } else if (responseHealth.getErrorRate() > 0.02) {
            risk += 0.3;
        }
        
        return Math.min(risk, 1.0);
    }

    private double analyzeCpuRisk(double cpuUsage) {
        if (cpuUsage > 0.95) {
            return 0.9;
        } else if (cpuUsage > 0.9) {
            return 0.7;
        } else if (cpuUsage > 0.8) {
            return 0.4;
        } else if (cpuUsage > 0.7) {
            return 0.2;
        }
        return 0.0;
    }

    private double analyzeDiskRisk(double diskUsage) {
        if (diskUsage > 0.95) {
            return 0.8;
        } else if (diskUsage > 0.9) {
            return 0.6;
        } else if (diskUsage > 0.8) {
            return 0.3;
        }
        return 0.0;
    }

    private List<String> identifyRiskFactors(Map<String, Double> riskScores) {
        return riskScores.entrySet().stream()
                .filter(entry -> entry.getValue() > 0.5) // High risk threshold
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .toList();
    }

    private double calculateOverallConfidence(Map<String, Double> riskScores) {
        if (riskScores.isEmpty()) {
            return 0.0;
        }
        
        // Calculate weighted average of risk scores
        double totalRisk = riskScores.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();
        
        double averageRisk = totalRisk / riskScores.size();
        
        // Boost confidence if multiple high-risk factors are present
        long highRiskCount = riskScores.values().stream()
                .mapToLong(score -> score > 0.7 ? 1 : 0)
                .sum();
        
        if (highRiskCount > 1) {
            averageRisk = Math.min(averageRisk * 1.2, 1.0);
        }
        
        return averageRisk;
    }

    private long calculateTimeToFailure(Map<String, Double> riskScores) {
        double maxRisk = riskScores.values().stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0.0);
        
        // Higher risk = shorter time to failure
        if (maxRisk > 0.9) {
            return 5; // 5 minutes
        } else if (maxRisk > 0.8) {
            return 15; // 15 minutes
        } else if (maxRisk > 0.7) {
            return 30; // 30 minutes
        } else {
            return 60; // 60 minutes
        }
    }

    private String generateRecommendedAction(List<String> riskFactors) {
        if (riskFactors.isEmpty()) {
            return "No immediate action required";
        }
        
        String primaryRisk = riskFactors.get(0);
        
        return switch (primaryRisk) {
            case "MEMORY_PRESSURE" -> "Clear caches and trigger garbage collection";
            case "DATABASE_SLOW" -> "Optimize database queries and check connection pool";
            case "HIGH_RESPONSE_TIME" -> "Scale application resources and optimize performance";
            case "CPU_PRESSURE" -> "Scale CPU resources or reduce load";
            case "DISK_PRESSURE" -> "Clean up disk space and archive old data";
            default -> "Monitor system closely and prepare for manual intervention";
        };
    }

    private Map<String, Object> extractFeatures(SystemHealth systemHealth) {
        Map<String, Object> features = new HashMap<>();
        
        features.put("timestamp", systemHealth.getTimestamp());
        features.put("cpuUsage", systemHealth.getCpuUsage());
        features.put("diskUsage", systemHealth.getDiskUsage());
        features.put("activeConnections", systemHealth.getActiveConnections());
        features.put("requestsPerSecond", systemHealth.getRequestsPerSecond());
        
        if (systemHealth.getMemoryHealth() != null) {
            features.put("memoryUsage", systemHealth.getMemoryHealth().getMemoryUsagePercentage());
            features.put("gcPressure", systemHealth.getMemoryHealth().getGcPressure());
        }
        
        if (systemHealth.getDatabaseHealth() != null) {
            features.put("dbConnectionPoolUsage", systemHealth.getDatabaseHealth().getConnectionPoolUsage());
            features.put("avgQueryTime", systemHealth.getDatabaseHealth().getAverageQueryTime());
        }
        
        if (systemHealth.getResponseTimeHealth() != null) {
            features.put("avgResponseTime", systemHealth.getResponseTimeHealth().getAverageResponseTime());
            features.put("errorRate", systemHealth.getResponseTimeHealth().getErrorRate());
        }
        
        return features;
    }
}