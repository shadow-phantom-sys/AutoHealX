package com.autohealx.healing.indicators;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CacheHealthIndicator implements HealthIndicator {

    private final RedisConnectionFactory redisConnectionFactory;

    @Override
    public Health health() {
        try {
            RedisConnection connection = redisConnectionFactory.getConnection();
            
            if (connection != null) {
                // Test Redis with a ping
                String pong = connection.ping();
                connection.close();
                
                if ("PONG".equals(pong)) {
                    return Health.up()
                            .withDetail("cache", "Redis")
                            .withDetail("status", "Connected")
                            .withDetail("ping", pong)
                            .build();
                } else {
                    return Health.down()
                            .withDetail("cache", "Redis")
                            .withDetail("status", "Ping failed")
                            .withDetail("response", pong)
                            .build();
                }
            } else {
                return Health.down()
                        .withDetail("cache", "Redis")
                        .withDetail("status", "No connection")
                        .build();
            }
        } catch (Exception e) {
            log.error("Cache health check failed", e);
            return Health.down()
                    .withDetail("cache", "Redis")
                    .withDetail("status", "Connection failed")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}