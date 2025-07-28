-- Create AI predictions table
CREATE TABLE ai_predictions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    prediction_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    prediction_type VARCHAR(50) NOT NULL,
    confidence DOUBLE NOT NULL,
    risk_factors JSON,
    risk_scores JSON,
    features JSON,
    predicted_failure_time TIMESTAMP,
    recommended_action TEXT,
    
    INDEX idx_predictions_timestamp (prediction_timestamp),
    INDEX idx_predictions_type (prediction_type),
    INDEX idx_predictions_confidence (confidence)
);

-- Create chaos experiments table
CREATE TABLE chaos_experiments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    experiment_name VARCHAR(100) NOT NULL,
    experiment_type VARCHAR(50) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'RUNNING',
    parameters JSON,
    results JSON,
    impact_assessment JSON,
    
    INDEX idx_chaos_experiments_name (experiment_name),
    INDEX idx_chaos_experiments_type (experiment_type),
    INDEX idx_chaos_experiments_status (status)
);

-- Create application metrics table
CREATE TABLE application_metrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    metric_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metric_category VARCHAR(50) NOT NULL,
    metric_name VARCHAR(100) NOT NULL,
    metric_value DOUBLE NOT NULL,
    unit VARCHAR(20),
    tags JSON,
    
    INDEX idx_app_metrics_timestamp (metric_timestamp),
    INDEX idx_app_metrics_category (metric_category),
    INDEX idx_app_metrics_name (metric_name)
);

-- Create performance baselines table
CREATE TABLE performance_baselines (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    baseline_name VARCHAR(100) NOT NULL UNIQUE,
    metric_type VARCHAR(50) NOT NULL,
    baseline_value DOUBLE NOT NULL,
    threshold_warning DOUBLE,
    threshold_critical DOUBLE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_baselines_name (baseline_name),
    INDEX idx_baselines_type (metric_type)
);

-- Insert default performance baselines
INSERT INTO performance_baselines (baseline_name, metric_type, baseline_value, threshold_warning, threshold_critical) VALUES
('cpu_usage_baseline', 'CPU', 0.3, 0.7, 0.9),
('memory_usage_baseline', 'MEMORY', 0.4, 0.8, 0.95),
('response_time_baseline', 'RESPONSE_TIME', 200.0, 1000.0, 2000.0),
('error_rate_baseline', 'ERROR_RATE', 0.001, 0.01, 0.05),
('disk_usage_baseline', 'DISK', 0.3, 0.8, 0.95);