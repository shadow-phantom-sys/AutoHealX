package com.autohealx.healing.indicators;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.io.File;
import java.lang.management.ManagementFactory;

@Component
@Slf4j
public class CustomHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        try {
            // Check disk space
            File root = new File("/");
            long totalSpace = root.getTotalSpace();
            long freeSpace = root.getFreeSpace();
            double usagePercentage = (double) (totalSpace - freeSpace) / totalSpace;
            
            // Check thread count
            int threadCount = ManagementFactory.getThreadMXBean().getThreadCount();
            
            Health.Builder healthBuilder = Health.up()
                    .withDetail("diskUsage", String.format("%.2f%%", usagePercentage * 100))
                    .withDetail("freeSpace", freeSpace)
                    .withDetail("totalSpace", totalSpace)
                    .withDetail("threadCount", threadCount);
            
            // Warning if disk usage > 90%
            if (usagePercentage > 0.9) {
                healthBuilder = Health.down()
                        .withDetail("warning", "Disk usage above 90%");
            }
            
            // Warning if too many threads
            if (threadCount > 500) {
                healthBuilder = Health.down()
                        .withDetail("warning", "Thread count above 500");
            }
            
            return healthBuilder.build();
            
        } catch (Exception e) {
            log.error("Custom health check failed", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}