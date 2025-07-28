package com.autohealx.healing;

import com.autohealx.healing.indicators.CacheHealthIndicator;
import com.autohealx.healing.indicators.CustomHealthIndicator;
import com.autohealx.healing.indicators.DatabaseHealthIndicator;
import com.autohealx.healing.indicators.ExternalApiHealthIndicator;
import com.autohealx.healing.models.*;
import com.autohealx.model.enums.HealthStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.stereotype.Service;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class HealthMonitor {

    private final DatabaseHealthIndicator databaseHealthIndicator;
    private final CacheHealthIndicator cacheHealthIndicator;
    private final ExternalApiHealthIndicator externalApiHealthIndicator;
    private final CustomHealthIndicator customHealthIndicator;

    private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    private final OperatingSystemMXBean osMXBean = ManagementFactory.getOperatingSystemMXBean();
    private final List<GarbageCollectorMXBean> gcMXBeans = ManagementFactory.getGarbageCollectorMXBeans();

    public SystemHealth getCurrentSystemHealth() {
        LocalDateTime now = LocalDateTime.now();
        
        DatabaseHealth databaseHealth = getDatabaseHealth();
        MemoryHealth memoryHealth = getMemoryHealth();
        ResponseTimeHealth responseTimeHealth = getResponseTimeHealth();
        
        HealthStatus overallStatus = determineOverallStatus(databaseHealth, memoryHealth, responseTimeHealth);
        
        return SystemHealth.builder()
                .overallStatus(overallStatus)
                .timestamp(now)
                .databaseHealth(databaseHealth)
                .memoryHealth(memoryHealth)
                .responseTimeHealth(responseTimeHealth)
                .cpuUsage(getCpuUsage())
                .diskUsage(getDiskUsage())
                .activeConnections(getActiveConnections())
                .requestsPerSecond(getRequestsPerSecond())
                .customMetrics(getCustomMetrics())
                .build();
    }

    public List<HealthIssue> detectIssues(SystemHealth systemHealth) {
        List<HealthIssue> issues = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // Check database issues
        if (systemHealth.getDatabaseHealth().getStatus() != HealthStatus.HEALTHY) {
            issues.add(HealthIssue.builder()
                    .type("DATABASE_UNHEALTHY")
                    .category("DATABASE")
                    .severity(systemHealth.getDatabaseHealth().getStatus() == HealthStatus.CRITICAL ? "CRITICAL" : "WARNING")
                    .description("Database health is degraded")
                    .detectedAt(now)
                    .context(Map.of("status", systemHealth.getDatabaseHealth().getStatus()))
                    .build());
        }

        // Check memory issues
        if (systemHealth.getMemoryHealth().isMemoryPressureHigh()) {
            issues.add(HealthIssue.builder()
                    .type("HIGH_MEMORY_USAGE")
                    .category("MEMORY")
                    .severity("WARNING")
                    .description("Memory usage is above 85%")
                    .detectedAt(now)
                    .context(Map.of("usage", systemHealth.getMemoryHealth().getMemoryUsagePercentage()))
                    .build());
        }

        // Check GC pressure
        if (systemHealth.getMemoryHealth().isGcPressureHigh()) {
            issues.add(HealthIssue.builder()
                    .type("HIGH_GC_PRESSURE")
                    .category("MEMORY")
                    .severity("CRITICAL")
                    .description("Garbage collection pressure is too high")
                    .detectedAt(now)
                    .context(Map.of("gcPressure", systemHealth.getMemoryHealth().getGcPressure()))
                    .build());
        }

        // Check response time issues
        if (systemHealth.getResponseTimeHealth().isResponseTimeSlow()) {
            issues.add(HealthIssue.builder()
                    .type("SLOW_RESPONSE_TIME")
                    .category("APPLICATION")
                    .severity("WARNING")
                    .description("Average response time is above threshold")
                    .detectedAt(now)
                    .context(Map.of("avgResponseTime", systemHealth.getResponseTimeHealth().getAverageResponseTime()))
                    .build());
        }

        // Check error rate
        if (systemHealth.getResponseTimeHealth().hasHighErrorRate()) {
            issues.add(HealthIssue.builder()
                    .type("HIGH_ERROR_RATE")
                    .category("APPLICATION")
                    .severity("CRITICAL")
                    .description("Error rate is above 5%")
                    .detectedAt(now)
                    .context(Map.of("errorRate", systemHealth.getResponseTimeHealth().getErrorRate()))
                    .build());
        }

        // Check CPU usage
        if (systemHealth.getCpuUsage() > 0.9) {
            issues.add(HealthIssue.builder()
                    .type("HIGH_CPU_USAGE")
                    .category("SYSTEM")
                    .severity("CRITICAL")
                    .description("CPU usage is above 90%")
                    .detectedAt(now)
                    .context(Map.of("cpuUsage", systemHealth.getCpuUsage()))
                    .build());
        }

        return issues;
    }

    private DatabaseHealth getDatabaseHealth() {
        Health dbHealth = databaseHealthIndicator.health();
        
        return DatabaseHealth.builder()
                .status(convertToHealthStatus(dbHealth.getStatus()))
                .lastChecked(LocalDateTime.now())
                .isConnected(dbHealth.getStatus().getCode().equals("UP"))
                .activeConnections(getActiveDbConnections())
                .maxConnections(getMaxDbConnections())
                .connectionPoolUsage(getConnectionPoolUsage())
                .averageQueryTime(getAverageQueryTime())
                .slowQueryCount(getSlowQueryCount())
                .deadlockCount(getDeadlockCount())
                .build();
    }

    private MemoryHealth getMemoryHealth() {
        long heapUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryMXBean.getHeapMemoryUsage().getMax();
        long nonHeapUsed = memoryMXBean.getNonHeapMemoryUsage().getUsed();
        long nonHeapMax = memoryMXBean.getNonHeapMemoryUsage().getMax();
        
        double memoryUsage = (double) heapUsed / heapMax;
        
        long totalGcTime = gcMXBeans.stream()
                .mapToLong(GarbageCollectorMXBean::getCollectionTime)
                .sum();
        
        long totalGcCount = gcMXBeans.stream()
                .mapToLong(GarbageCollectorMXBean::getCollectionCount)
                .sum();
        
        // Calculate GC pressure (simplified)
        double gcPressure = totalGcTime > 0 ? (double) totalGcTime / (System.currentTimeMillis() - ManagementFactory.getRuntimeMXBean().getStartTime()) : 0;
        
        HealthStatus status = HealthStatus.HEALTHY;
        if (memoryUsage > 0.9 || gcPressure > 0.1) {
            status = HealthStatus.CRITICAL;
        } else if (memoryUsage > 0.8 || gcPressure > 0.05) {
            status = HealthStatus.WARNING;
        }
        
        return MemoryHealth.builder()
                .status(status)
                .lastChecked(LocalDateTime.now())
                .usedMemory(heapUsed)
                .maxMemory(heapMax)
                .memoryUsagePercentage(memoryUsage)
                .gcCount(totalGcCount)
                .gcTime(totalGcTime)
                .gcPressure(gcPressure)
                .heapUsed(heapUsed)
                .heapMax(heapMax)
                .nonHeapUsed(nonHeapUsed)
                .nonHeapMax(nonHeapMax)
                .build();
    }

    private ResponseTimeHealth getResponseTimeHealth() {
        // These would typically come from metrics collectors like Micrometer
        return ResponseTimeHealth.builder()
                .status(HealthStatus.HEALTHY)
                .lastChecked(LocalDateTime.now())
                .averageResponseTime(getAverageResponseTime())
                .p95ResponseTime(getP95ResponseTime())
                .p99ResponseTime(getP99ResponseTime())
                .requestCount(getRequestCount())
                .errorCount(getErrorCount())
                .errorRate(getErrorRate())
                .throughput(getThroughput())
                .build();
    }

    private HealthStatus determineOverallStatus(DatabaseHealth dbHealth, MemoryHealth memHealth, ResponseTimeHealth respHealth) {
        if (dbHealth.getStatus() == HealthStatus.DOWN || 
            memHealth.getStatus() == HealthStatus.CRITICAL ||
            respHealth.getStatus() == HealthStatus.CRITICAL) {
            return HealthStatus.CRITICAL;
        }
        
        if (dbHealth.getStatus() == HealthStatus.WARNING || 
            memHealth.getStatus() == HealthStatus.WARNING ||
            respHealth.getStatus() == HealthStatus.WARNING) {
            return HealthStatus.WARNING;
        }
        
        return HealthStatus.HEALTHY;
    }

    private HealthStatus convertToHealthStatus(org.springframework.boot.actuator.health.Status status) {
        if (status.getCode().equals("UP")) return HealthStatus.HEALTHY;
        if (status.getCode().equals("DOWN")) return HealthStatus.DOWN;
        if (status.getCode().equals("OUT_OF_SERVICE")) return HealthStatus.CRITICAL;
        return HealthStatus.UNKNOWN;
    }

    // Placeholder methods - these would be implemented with actual metrics collection
    private double getCpuUsage() {
        if (osMXBean instanceof com.sun.management.OperatingSystemMXBean) {
            return ((com.sun.management.OperatingSystemMXBean) osMXBean).getProcessCpuLoad();
        }
        return 0.0;
    }

    private double getDiskUsage() { return 0.5; } // Placeholder
    private long getActiveConnections() { return 10; } // Placeholder
    private long getRequestsPerSecond() { return 100; } // Placeholder
    private int getActiveDbConnections() { return 5; } // Placeholder
    private int getMaxDbConnections() { return 20; } // Placeholder
    private double getConnectionPoolUsage() { return 0.25; } // Placeholder
    private long getAverageQueryTime() { return 50; } // Placeholder
    private long getSlowQueryCount() { return 0; } // Placeholder
    private long getDeadlockCount() { return 0; } // Placeholder
    private double getAverageResponseTime() { return 150.0; } // Placeholder
    private double getP95ResponseTime() { return 300.0; } // Placeholder
    private double getP99ResponseTime() { return 500.0; } // Placeholder
    private long getRequestCount() { return 1000; } // Placeholder
    private long getErrorCount() { return 5; } // Placeholder
    private double getErrorRate() { return 0.005; } // Placeholder
    private double getThroughput() { return 50.0; } // Placeholder

    private Map<String, Object> getCustomMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("jvm.threads.live", ManagementFactory.getThreadMXBean().getThreadCount());
        metrics.put("jvm.threads.daemon", ManagementFactory.getThreadMXBean().getDaemonThreadCount());
        metrics.put("system.load.average.1m", osMXBean.getSystemLoadAverage());
        return metrics;
    }
}