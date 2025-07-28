package com.autohealx.healing;

import com.autohealx.healing.models.PredictionResult;
import com.autohealx.healing.models.SystemHealth;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetricsCollector {

    private final MeterRegistry meterRegistry;
    
    // Counters for healing activities
    private final Counter successfulHealingCounter;
    private final Counter failedHealingCounter;
    private final Counter proactiveHealingCounter;
    private final Counter healthCheckCounter;
    
    // Gauges for system health
    private final AtomicLong lastHealthCheckTime = new AtomicLong();
    private final AtomicLong activeIssuesCount = new AtomicLong();
    private final Map<String, AtomicLong> healingAttemptsByType = new ConcurrentHashMap<>();
    
    // Timers for performance tracking
    private final Timer healthCheckTimer;
    private final Timer healingTimer;

    public MetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Initialize counters
        this.successfulHealingCounter = Counter.builder("autohealx.healing.successful")
                .description("Number of successful healing attempts")
                .register(meterRegistry);
                
        this.failedHealingCounter = Counter.builder("autohealx.healing.failed")
                .description("Number of failed healing attempts")
                .register(meterRegistry);
                
        this.proactiveHealingCounter = Counter.builder("autohealx.healing.proactive")
                .description("Number of proactive healing actions")
                .register(meterRegistry);
                
        this.healthCheckCounter = Counter.builder("autohealx.healthcheck.total")
                .description("Total number of health checks performed")
                .register(meterRegistry);
        
        // Initialize gauges
        Gauge.builder("autohealx.healthcheck.last.timestamp")
                .description("Timestamp of last health check")
                .register(meterRegistry, lastHealthCheckTime, AtomicLong::get);
                
        Gauge.builder("autohealx.issues.active.count")
                .description("Number of active health issues")
                .register(meterRegistry, activeIssuesCount, AtomicLong::get);
        
        // Initialize timers
        this.healthCheckTimer = Timer.builder("autohealx.healthcheck.duration")
                .description("Duration of health check operations")
                .register(meterRegistry);
                
        this.healingTimer = Timer.builder("autohealx.healing.duration")
                .description("Duration of healing operations")
                .register(meterRegistry);
    }

    public void recordHealthMetrics(SystemHealth systemHealth) {
        log.debug("Recording health metrics");
        
        healthCheckCounter.increment();
        lastHealthCheckTime.set(System.currentTimeMillis());
        
        // Record system-level metrics
        recordGauge("autohealx.system.cpu.usage", systemHealth.getCpuUsage());
        recordGauge("autohealx.system.disk.usage", systemHealth.getDiskUsage());
        recordGauge("autohealx.system.connections.active", systemHealth.getActiveConnections());
        recordGauge("autohealx.system.requests.per.second", systemHealth.getRequestsPerSecond());
        
        // Record memory metrics
        if (systemHealth.getMemoryHealth() != null) {
            recordGauge("autohealx.memory.usage.percentage", 
                    systemHealth.getMemoryHealth().getMemoryUsagePercentage());
            recordGauge("autohealx.memory.gc.pressure", 
                    systemHealth.getMemoryHealth().getGcPressure());
            recordGauge("autohealx.memory.heap.used", 
                    systemHealth.getMemoryHealth().getHeapUsed());
            recordGauge("autohealx.memory.heap.max", 
                    systemHealth.getMemoryHealth().getHeapMax());
        }
        
        // Record database metrics
        if (systemHealth.getDatabaseHealth() != null) {
            recordGauge("autohealx.database.connections.active", 
                    systemHealth.getDatabaseHealth().getActiveConnections());
            recordGauge("autohealx.database.pool.usage", 
                    systemHealth.getDatabaseHealth().getConnectionPoolUsage());
            recordGauge("autohealx.database.query.time.average", 
                    systemHealth.getDatabaseHealth().getAverageQueryTime());
            recordGauge("autohealx.database.deadlocks.count", 
                    systemHealth.getDatabaseHealth().getDeadlockCount());
        }
        
        // Record response time metrics
        if (systemHealth.getResponseTimeHealth() != null) {
            recordGauge("autohealx.response.time.average", 
                    systemHealth.getResponseTimeHealth().getAverageResponseTime());
            recordGauge("autohealx.response.time.p95", 
                    systemHealth.getResponseTimeHealth().getP95ResponseTime());
            recordGauge("autohealx.response.time.p99", 
                    systemHealth.getResponseTimeHealth().getP99ResponseTime());
            recordGauge("autohealx.response.error.rate", 
                    systemHealth.getResponseTimeHealth().getErrorRate());
            recordGauge("autohealx.response.throughput", 
                    systemHealth.getResponseTimeHealth().getThroughput());
        }
        
        // Record overall health status
        recordGauge("autohealx.health.status", mapHealthStatusToNumeric(systemHealth.getOverallStatus()));
    }

    public void recordSuccessfulHealing(String issueType) {
        log.debug("Recording successful healing for issue type: {}", issueType);
        
        successfulHealingCounter.increment("issue_type", issueType);
        
        // Track healing attempts by type
        healingAttemptsByType.computeIfAbsent(issueType, k -> 
                Gauge.builder("autohealx.healing.attempts")
                        .description("Number of healing attempts by type")
                        .tag("issue_type", issueType)
                        .register(meterRegistry, new AtomicLong())
        ).incrementAndGet();
    }

    public void recordHealingFailure(String issueType, String reason) {
        log.debug("Recording healing failure for issue type: {}, reason: {}", issueType, reason);
        
        failedHealingCounter.increment("issue_type", issueType, "reason", reason);
        
        // Track healing attempts by type
        healingAttemptsByType.computeIfAbsent(issueType, k -> 
                Gauge.builder("autohealx.healing.attempts")
                        .description("Number of healing attempts by type")
                        .tag("issue_type", issueType)
                        .register(meterRegistry, new AtomicLong())
        ).incrementAndGet();
    }

    public void recordProactiveHealing(String riskFactor, double confidence) {
        log.debug("Recording proactive healing for risk factor: {}, confidence: {}", riskFactor, confidence);
        
        proactiveHealingCounter.increment("risk_factor", riskFactor);
        recordGauge("autohealx.prediction.confidence", confidence, "risk_factor", riskFactor);
    }

    public void recordPredictionMetrics(PredictionResult prediction) {
        log.debug("Recording prediction metrics with confidence: {}", prediction.getConfidence());
        
        recordGauge("autohealx.prediction.overall.confidence", prediction.getConfidence());
        
        // Record risk scores
        if (prediction.getRiskScores() != null) {
            prediction.getRiskScores().forEach((riskFactor, score) -> 
                    recordGauge("autohealx.prediction.risk.score", score, "risk_factor", riskFactor));
        }
        
        // Record number of risk factors
        if (prediction.getRiskFactors() != null) {
            recordGauge("autohealx.prediction.risk.factors.count", prediction.getRiskFactors().size());
        }
        
        // Record if immediate action is required
        recordGauge("autohealx.prediction.immediate.action.required", 
                prediction.requiresImmediateAction() ? 1.0 : 0.0);
    }

    public void recordActiveIssuesCount(int count) {
        activeIssuesCount.set(count);
    }

    public Timer.Sample startHealthCheckTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopHealthCheckTimer(Timer.Sample sample) {
        sample.stop(healthCheckTimer);
    }

    public Timer.Sample startHealingTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopHealingTimer(Timer.Sample sample, String issueType) {
        sample.stop(Timer.builder("autohealx.healing.duration")
                .tag("issue_type", issueType)
                .register(meterRegistry));
    }

    private void recordGauge(String name, double value, String... tags) {
        try {
            Gauge.builder(name)
                    .tags(tags)
                    .register(meterRegistry, () -> value);
        } catch (Exception e) {
            log.warn("Failed to record gauge metric: {}", name, e);
        }
    }

    private double mapHealthStatusToNumeric(com.autohealx.model.enums.HealthStatus status) {
        return switch (status) {
            case HEALTHY -> 1.0;
            case WARNING -> 0.5;
            case CRITICAL -> 0.2;
            case DOWN -> 0.0;
            case RECOVERING -> 0.3;
            case UNKNOWN -> -1.0;
        };
    }

    public Map<String, Object> getMetricsSummary() {
        Map<String, Object> summary = new ConcurrentHashMap<>();
        
        summary.put("successfulHealings", successfulHealingCounter.count());
        summary.put("failedHealings", failedHealingCounter.count());
        summary.put("proactiveHealings", proactiveHealingCounter.count());
        summary.put("totalHealthChecks", healthCheckCounter.count());
        summary.put("lastHealthCheckTime", LocalDateTime.now());
        summary.put("activeIssues", activeIssuesCount.get());
        
        return summary;
    }
}