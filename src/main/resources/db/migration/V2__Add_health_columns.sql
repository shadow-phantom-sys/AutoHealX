-- Create health metrics table
CREATE TABLE health_metrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metric_name VARCHAR(100) NOT NULL,
    metric_value DOUBLE NOT NULL,
    metric_type VARCHAR(50) NOT NULL,
    tags JSON,
    
    INDEX idx_health_metrics_timestamp (timestamp),
    INDEX idx_health_metrics_name (metric_name),
    INDEX idx_health_metrics_type (metric_type)
);

-- Create healing events table
CREATE TABLE healing_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    issue_type VARCHAR(100) NOT NULL,
    issue_category VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    description TEXT,
    healing_action VARCHAR(100),
    healing_result VARCHAR(20) NOT NULL,
    duration_ms BIGINT,
    context JSON,
    
    INDEX idx_healing_events_timestamp (event_timestamp),
    INDEX idx_healing_events_type (issue_type),
    INDEX idx_healing_events_result (healing_result)
);

-- Create system health snapshots table
CREATE TABLE system_health_snapshots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    snapshot_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    overall_status VARCHAR(20) NOT NULL,
    cpu_usage DOUBLE,
    memory_usage DOUBLE,
    disk_usage DOUBLE,
    active_connections BIGINT,
    requests_per_second DOUBLE,
    error_rate DOUBLE,
    average_response_time DOUBLE,
    health_data JSON,
    
    INDEX idx_health_snapshots_timestamp (snapshot_timestamp),
    INDEX idx_health_snapshots_status (overall_status)
);