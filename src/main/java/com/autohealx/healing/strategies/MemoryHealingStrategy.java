package com.autohealx.healing.strategies;

import com.autohealx.healing.models.HealthIssue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

@Service
@Slf4j
public class MemoryHealingStrategy {

    private final CacheManager cacheManager;
    private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

    public MemoryHealingStrategy(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public boolean heal(HealthIssue issue) {
        log.info("Attempting to heal memory issue: {}", issue.getType());
        
        switch (issue.getType()) {
            case "HIGH_MEMORY_USAGE":
                return freeMemory();
            case "HIGH_GC_PRESSURE":
                return reduceGcPressure();
            case "MEMORY_LEAK":
                return handleMemoryLeak();
            case "OUT_OF_MEMORY":
                return handleOutOfMemory();
            default:
                log.warn("Unknown memory issue type: {}", issue.getType());
                return false;
        }
    }

    private boolean freeMemory() {
        try {
            log.info("Attempting to free memory...");
            
            long beforeHeap = memoryMXBean.getHeapMemoryUsage().getUsed();
            
            // Clear caches
            clearCaches();
            
            // Suggest garbage collection
            System.gc();
            
            // Wait a moment for GC to complete
            Thread.sleep(1000);
            
            long afterHeap = memoryMXBean.getHeapMemoryUsage().getUsed();
            long freedMemory = beforeHeap - afterHeap;
            
            log.info("Memory freed: {} bytes", freedMemory);
            
            // Check if memory usage improved
            double currentUsage = (double) afterHeap / memoryMXBean.getHeapMemoryUsage().getMax();
            if (currentUsage < 0.8) { // Less than 80%
                log.info("Memory healing successful, usage now: {:.2f}%", currentUsage * 100);
                return true;
            } else {
                log.warn("Memory healing had limited effect, usage still: {:.2f}%", currentUsage * 100);
                return false;
            }
            
        } catch (Exception e) {
            log.error("Failed to free memory", e);
            return false;
        }
    }

    private boolean reduceGcPressure() {
        try {
            log.info("Attempting to reduce GC pressure...");
            
            // Clear caches to reduce object count
            clearCaches();
            
            // Force a full GC cycle
            System.gc();
            
            // In a real implementation, you might also:
            // - Tune GC parameters dynamically
            // - Reduce object allocation rates
            // - Clear weak references
            
            log.info("GC pressure reduction completed");
            return true;
            
        } catch (Exception e) {
            log.error("Failed to reduce GC pressure", e);
            return false;
        }
    }

    private boolean handleMemoryLeak() {
        try {
            log.info("Attempting to handle memory leak...");
            
            // Clear all caches
            clearCaches();
            
            // Force garbage collection
            System.gc();
            
            // In a real implementation, you might:
            // - Dump heap for analysis
            // - Clear ThreadLocal variables
            // - Reset connection pools
            // - Clear static collections
            
            log.info("Memory leak handling completed");
            return true;
            
        } catch (Exception e) {
            log.error("Failed to handle memory leak", e);
            return false;
        }
    }

    private boolean handleOutOfMemory() {
        try {
            log.error("Handling out of memory condition...");
            
            // Emergency memory cleanup
            clearCaches();
            System.gc();
            
            // In a real implementation, you might:
            // - Scale up the application
            // - Restart the application
            // - Enable emergency mode with reduced functionality
            
            log.info("Out of memory handling completed");
            return true;
            
        } catch (Exception e) {
            log.error("Failed to handle out of memory", e);
            return false;
        }
    }

    private void clearCaches() {
        if (cacheManager != null) {
            cacheManager.getCacheNames().forEach(cacheName -> {
                try {
                    cacheManager.getCache(cacheName).clear();
                    log.debug("Cleared cache: {}", cacheName);
                } catch (Exception e) {
                    log.warn("Failed to clear cache: {}", cacheName, e);
                }
            });
            log.info("All caches cleared");
        }
    }
}