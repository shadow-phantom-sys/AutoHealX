-- Add composite indexes for better query performance

-- Products table indexes
CREATE INDEX idx_products_category_active ON products(category, active);
CREATE INDEX idx_products_price_range ON products(price, active);
CREATE INDEX idx_products_stock_low ON products(stock_quantity, active);

-- Orders table indexes  
CREATE INDEX idx_orders_customer_status ON orders(customer_id, status);
CREATE INDEX idx_orders_date_status ON orders(created_at, status);
CREATE INDEX idx_orders_total_amount ON orders(total_amount);

-- Health metrics indexes
CREATE INDEX idx_health_metrics_name_timestamp ON health_metrics(metric_name, timestamp);
CREATE INDEX idx_health_metrics_type_timestamp ON health_metrics(metric_type, timestamp);

-- Healing events indexes
CREATE INDEX idx_healing_events_type_timestamp ON healing_events(issue_type, event_timestamp);
CREATE INDEX idx_healing_events_category_result ON healing_events(issue_category, healing_result);

-- System health snapshots indexes
CREATE INDEX idx_health_snapshots_status_timestamp ON system_health_snapshots(overall_status, snapshot_timestamp);

-- AI predictions indexes
CREATE INDEX idx_predictions_type_timestamp ON ai_predictions(prediction_type, prediction_timestamp);
CREATE INDEX idx_predictions_confidence_timestamp ON ai_predictions(confidence, prediction_timestamp);

-- Application metrics indexes
CREATE INDEX idx_app_metrics_category_name_timestamp ON application_metrics(metric_category, metric_name, metric_timestamp);

-- Add foreign key constraints that were missing
ALTER TABLE healing_events ADD COLUMN system_health_snapshot_id BIGINT NULL;
ALTER TABLE healing_events ADD FOREIGN KEY (system_health_snapshot_id) REFERENCES system_health_snapshots(id);

-- Create views for common queries
CREATE VIEW recent_health_metrics AS
SELECT 
    metric_name,
    metric_value,
    metric_type,
    timestamp
FROM health_metrics 
WHERE timestamp >= DATE_SUB(NOW(), INTERVAL 1 HOUR)
ORDER BY timestamp DESC;

CREATE VIEW healing_success_rate AS
SELECT 
    issue_type,
    COUNT(*) as total_attempts,
    SUM(CASE WHEN healing_result = 'SUCCESS' THEN 1 ELSE 0 END) as successful_attempts,
    ROUND(SUM(CASE WHEN healing_result = 'SUCCESS' THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 2) as success_rate
FROM healing_events
WHERE event_timestamp >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
GROUP BY issue_type;

CREATE VIEW system_health_trend AS
SELECT 
    DATE(snapshot_timestamp) as date,
    overall_status,
    AVG(cpu_usage) as avg_cpu_usage,
    AVG(memory_usage) as avg_memory_usage,
    AVG(disk_usage) as avg_disk_usage,
    AVG(error_rate) as avg_error_rate,
    AVG(average_response_time) as avg_response_time
FROM system_health_snapshots
WHERE snapshot_timestamp >= DATE_SUB(NOW(), INTERVAL 7 DAY)
GROUP BY DATE(snapshot_timestamp), overall_status
ORDER BY date DESC;