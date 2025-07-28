#!/usr/bin/env python3
"""
AutoHealX AI-Powered Health Monitor
This script demonstrates AI-driven self-healing capabilities by:
1. Monitoring service health and performance metrics
2. Predicting potential failures using simple ML models
3. Triggering automated remediation actions
4. Logging all activities for observability
"""

import asyncio
import aiohttp
import json
import logging
import time
import statistics
from datetime import datetime, timedelta
from typing import Dict, List, Optional, Tuple
from dataclasses import dataclass, asdict
import numpy as np
from sklearn.ensemble import IsolationForest
from sklearn.preprocessing import StandardScaler
import requests
import subprocess
import os

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('/tmp/ai-health-monitor.log'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger('AutoHealX-AI-Monitor')

@dataclass
class ServiceMetrics:
    """Data class for service metrics"""
    service_name: str
    timestamp: datetime
    response_time: float
    error_rate: float
    cpu_usage: float
    memory_usage: float
    request_count: int
    is_healthy: bool
    
    def to_features(self) -> List[float]:
        """Convert metrics to feature vector for ML model"""
        return [
            self.response_time,
            self.error_rate,
            self.cpu_usage,
            self.memory_usage,
            self.request_count
        ]

@dataclass
class HealthAlert:
    """Data class for health alerts"""
    service_name: str
    alert_type: str
    severity: str
    message: str
    timestamp: datetime
    metrics: ServiceMetrics
    prediction_confidence: float

class AIHealthMonitor:
    """AI-powered health monitoring and self-healing system"""
    
    def __init__(self):
        self.services = {
            'product-service': 'http://localhost:8081',
            'cart-service': 'http://localhost:8082',
            'order-service': 'http://localhost:8083',
            'payment-service': 'http://localhost:8084'
        }
        
        self.prometheus_url = 'http://localhost:9090'
        self.grafana_url = 'http://localhost:3000'
        
        # ML models for anomaly detection
        self.anomaly_detectors = {}
        self.scalers = {}
        self.metrics_history = {}
        
        # Thresholds for alerts
        self.thresholds = {
            'response_time': 2.0,  # seconds
            'error_rate': 0.05,    # 5%
            'cpu_usage': 80.0,     # percentage
            'memory_usage': 85.0,  # percentage
        }
        
        # Initialize ML models
        self._initialize_ml_models()
        
    def _initialize_ml_models(self):
        """Initialize ML models for each service"""
        for service in self.services.keys():
            self.anomaly_detectors[service] = IsolationForest(
                contamination=0.1,
                random_state=42
            )
            self.scalers[service] = StandardScaler()
            self.metrics_history[service] = []
    
    async def collect_service_metrics(self, service_name: str, base_url: str) -> Optional[ServiceMetrics]:
        """Collect metrics from a service"""
        try:
            async with aiohttp.ClientSession() as session:
                # Health check
                health_url = f"{base_url}/actuator/health"
                async with session.get(health_url, timeout=5) as response:
                    is_healthy = response.status == 200
                    health_data = await response.json() if is_healthy else {}
                
                # Metrics
                metrics_url = f"{base_url}/actuator/metrics"
                async with session.get(metrics_url, timeout=5) as response:
                    if response.status != 200:
                        return None
                    
                    metrics_data = await response.json()
                    
                    # Extract key metrics
                    response_time = await self._get_metric_value(session, base_url, 'http.server.requests')
                    error_rate = await self._calculate_error_rate(session, base_url)
                    cpu_usage = await self._get_metric_value(session, base_url, 'process.cpu.usage') * 100
                    memory_usage = await self._get_memory_usage(session, base_url)
                    request_count = await self._get_metric_value(session, base_url, 'http.server.requests', tags='count')
                    
                    return ServiceMetrics(
                        service_name=service_name,
                        timestamp=datetime.now(),
                        response_time=response_time or 0.0,
                        error_rate=error_rate or 0.0,
                        cpu_usage=cpu_usage or 0.0,
                        memory_usage=memory_usage or 0.0,
                        request_count=int(request_count or 0),
                        is_healthy=is_healthy
                    )
                    
        except Exception as e:
            logger.error(f"Failed to collect metrics for {service_name}: {e}")
            return None
    
    async def _get_metric_value(self, session: aiohttp.ClientSession, base_url: str, 
                               metric_name: str, tags: str = None) -> Optional[float]:
        """Get specific metric value from actuator endpoint"""
        try:
            url = f"{base_url}/actuator/metrics/{metric_name}"
            async with session.get(url, timeout=5) as response:
                if response.status == 200:
                    data = await response.json()
                    measurements = data.get('measurements', [])
                    if measurements:
                        return measurements[0].get('value', 0.0)
        except Exception as e:
            logger.debug(f"Failed to get metric {metric_name}: {e}")
        return None
    
    async def _calculate_error_rate(self, session: aiohttp.ClientSession, base_url: str) -> float:
        """Calculate error rate from HTTP metrics"""
        try:
            total_requests = await self._get_metric_value(session, base_url, 'http.server.requests')
            error_requests = 0
            
            # This is a simplified calculation - in reality, you'd query Prometheus
            # for more accurate error rate calculations
            for status in ['4xx', '5xx']:
                errors = await self._get_metric_value(session, base_url, f'http.server.requests.{status}')
                if errors:
                    error_requests += errors
            
            if total_requests and total_requests > 0:
                return error_requests / total_requests
            return 0.0
            
        except Exception:
            return 0.0
    
    async def _get_memory_usage(self, session: aiohttp.ClientSession, base_url: str) -> float:
        """Calculate memory usage percentage"""
        try:
            used = await self._get_metric_value(session, base_url, 'jvm.memory.used')
            max_memory = await self._get_metric_value(session, base_url, 'jvm.memory.max')
            
            if used and max_memory and max_memory > 0:
                return (used / max_memory) * 100
            return 0.0
            
        except Exception:
            return 0.0
    
    def predict_anomaly(self, service_name: str, metrics: ServiceMetrics) -> Tuple[bool, float]:
        """Use ML model to predict if metrics indicate an anomaly"""
        try:
            # Add to history
            self.metrics_history[service_name].append(metrics)
            
            # Keep only last 100 data points
            if len(self.metrics_history[service_name]) > 100:
                self.metrics_history[service_name] = self.metrics_history[service_name][-100:]
            
            # Need at least 10 data points to train
            if len(self.metrics_history[service_name]) < 10:
                return False, 0.0
            
            # Prepare training data
            features = [m.to_features() for m in self.metrics_history[service_name]]
            X = np.array(features)
            
            # Scale features
            X_scaled = self.scalers[service_name].fit_transform(X)
            
            # Train model
            self.anomaly_detectors[service_name].fit(X_scaled)
            
            # Predict on current metrics
            current_features = np.array([metrics.to_features()])
            current_scaled = self.scalers[service_name].transform(current_features)
            
            prediction = self.anomaly_detectors[service_name].predict(current_scaled)[0]
            score = self.anomaly_detectors[service_name].score_samples(current_scaled)[0]
            
            # Convert score to confidence (higher negative score = more anomalous)
            confidence = max(0, min(1, (score + 0.5) * 2))
            
            is_anomaly = prediction == -1
            return is_anomaly, confidence
            
        except Exception as e:
            logger.error(f"Anomaly prediction failed for {service_name}: {e}")
            return False, 0.0
    
    def check_thresholds(self, metrics: ServiceMetrics) -> List[HealthAlert]:
        """Check if metrics exceed predefined thresholds"""
        alerts = []
        
        checks = [
            ('response_time', metrics.response_time, 'HIGH_RESPONSE_TIME'),
            ('error_rate', metrics.error_rate, 'HIGH_ERROR_RATE'),
            ('cpu_usage', metrics.cpu_usage, 'HIGH_CPU_USAGE'),
            ('memory_usage', metrics.memory_usage, 'HIGH_MEMORY_USAGE'),
        ]
        
        for metric_name, value, alert_type in checks:
            threshold = self.thresholds[metric_name]
            if value > threshold:
                severity = 'CRITICAL' if value > threshold * 1.5 else 'WARNING'
                message = f"{metric_name} is {value:.2f}, exceeding threshold of {threshold}"
                
                alerts.append(HealthAlert(
                    service_name=metrics.service_name,
                    alert_type=alert_type,
                    severity=severity,
                    message=message,
                    timestamp=datetime.now(),
                    metrics=metrics,
                    prediction_confidence=1.0
                ))
        
        return alerts
    
    async def trigger_self_healing(self, alert: HealthAlert):
        """Trigger automated remediation based on alert type"""
        service_name = alert.service_name
        alert_type = alert.alert_type
        
        logger.info(f"Triggering self-healing for {service_name}: {alert_type}")
        
        try:
            if alert_type == 'HIGH_MEMORY_USAGE':
                await self._restart_service(service_name)
            elif alert_type == 'HIGH_CPU_USAGE':
                await self._scale_service(service_name, scale_up=True)
            elif alert_type == 'HIGH_ERROR_RATE':
                await self._rollback_service(service_name)
            elif alert_type == 'HIGH_RESPONSE_TIME':
                await self._scale_service(service_name, scale_up=True)
            else:
                logger.warning(f"No remediation action defined for {alert_type}")
                
        except Exception as e:
            logger.error(f"Self-healing action failed for {service_name}: {e}")
    
    async def _restart_service(self, service_name: str):
        """Restart a service container"""
        try:
            container_name = f"autohealx-{service_name}"
            cmd = f"docker restart {container_name}"
            result = subprocess.run(cmd, shell=True, capture_output=True, text=True)
            
            if result.returncode == 0:
                logger.info(f"Successfully restarted {service_name}")
                await self._send_notification(f"Service {service_name} restarted due to high memory usage")
            else:
                logger.error(f"Failed to restart {service_name}: {result.stderr}")
                
        except Exception as e:
            logger.error(f"Error restarting {service_name}: {e}")
    
    async def _scale_service(self, service_name: str, scale_up: bool = True):
        """Scale a service (simulated - in real K8s this would use HPA)"""
        try:
            action = "up" if scale_up else "down"
            logger.info(f"Scaling {service_name} {action} (simulated)")
            
            # In a real Kubernetes environment, this would trigger HPA or manual scaling
            # For demo purposes, we'll just log the action
            await self._send_notification(f"Service {service_name} scaled {action} due to performance issues")
            
        except Exception as e:
            logger.error(f"Error scaling {service_name}: {e}")
    
    async def _rollback_service(self, service_name: str):
        """Rollback service to previous version (simulated)"""
        try:
            logger.info(f"Rolling back {service_name} to previous version (simulated)")
            
            # In a real environment, this would trigger Argo Rollouts or similar
            await self._send_notification(f"Service {service_name} rolled back due to high error rate")
            
        except Exception as e:
            logger.error(f"Error rolling back {service_name}: {e}")
    
    async def _send_notification(self, message: str):
        """Send notification (simulated - could be Slack, email, etc.)"""
        logger.info(f"NOTIFICATION: {message}")
        
        # In a real system, you would send to Slack, email, PagerDuty, etc.
        notification_data = {
            'timestamp': datetime.now().isoformat(),
            'message': message,
            'source': 'AutoHealX-AI-Monitor'
        }
        
        # Write to file for demo purposes
        with open('/tmp/autohealx-notifications.json', 'a') as f:
            f.write(json.dumps(notification_data) + '\n')
    
    async def run_monitoring_cycle(self):
        """Run one complete monitoring cycle"""
        logger.info("Starting monitoring cycle...")
        
        for service_name, base_url in self.services.items():
            try:
                # Collect metrics
                metrics = await self.collect_service_metrics(service_name, base_url)
                
                if not metrics:
                    logger.warning(f"Could not collect metrics for {service_name}")
                    continue
                
                logger.info(f"Collected metrics for {service_name}: "
                          f"RT={metrics.response_time:.2f}s, "
                          f"ER={metrics.error_rate:.2%}, "
                          f"CPU={metrics.cpu_usage:.1f}%, "
                          f"MEM={metrics.memory_usage:.1f}%")
                
                # Check thresholds
                threshold_alerts = self.check_thresholds(metrics)
                
                # Check for anomalies using ML
                is_anomaly, confidence = self.predict_anomaly(service_name, metrics)
                
                if is_anomaly:
                    anomaly_alert = HealthAlert(
                        service_name=service_name,
                        alert_type='ANOMALY_DETECTED',
                        severity='WARNING',
                        message=f"ML model detected anomaly with confidence {confidence:.2%}",
                        timestamp=datetime.now(),
                        metrics=metrics,
                        prediction_confidence=confidence
                    )
                    threshold_alerts.append(anomaly_alert)
                
                # Process alerts
                for alert in threshold_alerts:
                    logger.warning(f"ALERT: {alert.service_name} - {alert.alert_type} - {alert.message}")
                    
                    # Trigger self-healing for critical alerts
                    if alert.severity == 'CRITICAL':
                        await self.trigger_self_healing(alert)
                
            except Exception as e:
                logger.error(f"Error monitoring {service_name}: {e}")
    
    async def run_continuous_monitoring(self, interval: int = 30):
        """Run continuous monitoring with specified interval"""
        logger.info(f"Starting continuous monitoring with {interval}s interval...")
        
        while True:
            try:
                await self.run_monitoring_cycle()
                await asyncio.sleep(interval)
            except KeyboardInterrupt:
                logger.info("Monitoring stopped by user")
                break
            except Exception as e:
                logger.error(f"Unexpected error in monitoring loop: {e}")
                await asyncio.sleep(interval)

async def main():
    """Main function"""
    print("🚀 AutoHealX AI-Powered Health Monitor Starting...")
    print("This demo shows AI-driven self-healing capabilities:")
    print("• Continuous health monitoring")
    print("• ML-based anomaly detection")
    print("• Automated remediation actions")
    print("• Comprehensive logging and notifications")
    print("\nPress Ctrl+C to stop monitoring\n")
    
    monitor = AIHealthMonitor()
    await monitor.run_continuous_monitoring(interval=30)

if __name__ == "__main__":
    asyncio.run(main())