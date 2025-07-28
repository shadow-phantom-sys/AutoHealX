package com.autohealx.controller;

import com.autohealx.healing.SelfHealingEngine;
import com.autohealx.healing.models.SystemHealth;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
@Slf4j
public class HealthController {

    private final SelfHealingEngine selfHealingEngine;

    @GetMapping("/status")
    public ResponseEntity<SystemHealth> getSystemHealth() {
        log.debug("Getting system health status");
        
        SystemHealth health = selfHealingEngine.getCurrentHealth();
        return ResponseEntity.ok(health);
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getHealingStatistics() {
        log.debug("Getting healing statistics");
        
        Map<String, Object> stats = selfHealingEngine.getHealingStatistics();
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/trigger-check")
    public ResponseEntity<String> triggerHealthCheck() {
        log.info("Manual health check triggered");
        
        try {
            selfHealingEngine.performHealthCheck();
            return ResponseEntity.ok("Health check triggered successfully");
        } catch (Exception e) {
            log.error("Failed to trigger health check", e);
            return ResponseEntity.internalServerError()
                    .body("Failed to trigger health check: " + e.getMessage());
        }
    }

    @GetMapping("/detailed")
    public ResponseEntity<Map<String, Object>> getDetailedHealth() {
        log.debug("Getting detailed health information");
        
        SystemHealth health = selfHealingEngine.getCurrentHealth();
        Map<String, Object> stats = selfHealingEngine.getHealingStatistics();
        
        Map<String, Object> detailed = Map.of(
                "systemHealth", health,
                "healingStatistics", stats,
                "timestamp", System.currentTimeMillis()
        );
        
        return ResponseEntity.ok(detailed);
    }
}