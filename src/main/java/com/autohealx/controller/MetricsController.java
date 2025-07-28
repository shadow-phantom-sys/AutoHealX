package com.autohealx.controller;

import com.autohealx.healing.MetricsCollector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/metrics")
@RequiredArgsConstructor
@Slf4j
public class MetricsController {

    private final MetricsCollector metricsCollector;

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getMetricsSummary() {
        log.debug("Getting metrics summary");
        
        Map<String, Object> summary = metricsCollector.getMetricsSummary();
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/healing")
    public ResponseEntity<Map<String, Object>> getHealingMetrics() {
        log.debug("Getting healing metrics");
        
        Map<String, Object> metrics = Map.of(
                "successfulHealings", metricsCollector.getMetricsSummary().get("successfulHealings"),
                "failedHealings", metricsCollector.getMetricsSummary().get("failedHealings"),
                "proactiveHealings", metricsCollector.getMetricsSummary().get("proactiveHealings")
        );
        
        return ResponseEntity.ok(metrics);
    }
}