-- AutoHealX Database Schema
-- Create database if not exists
CREATE DATABASE IF NOT EXISTS autohealx;
USE autohealx;

-- Create user for the application
CREATE USER IF NOT EXISTS 'autohealx'@'%' IDENTIFIED BY 'autohealx123';
GRANT ALL PRIVILEGES ON autohealx.* TO 'autohealx'@'%';
FLUSH PRIVILEGES;

-- Products table
CREATE TABLE IF NOT EXISTS products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    stock_quantity INT NOT NULL DEFAULT 0,
    category VARCHAR(100),
    image_url VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    
    INDEX idx_product_category (category),
    INDEX idx_product_active (active),
    INDEX idx_product_name (name),
    INDEX idx_product_stock (stock_quantity)
);

-- Cart items table
CREATE TABLE IF NOT EXISTS cart_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(255),
    product_price DECIMAL(10,2),
    quantity INT NOT NULL,
    cart_token VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_cart_token (cart_token),
    INDEX idx_cart_product (product_id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);

-- Orders table
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_email VARCHAR(255) NOT NULL,
    customer_name VARCHAR(255) NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    payment_id VARCHAR(255),
    shipping_address TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_order_status (status),
    INDEX idx_order_email (customer_email),
    INDEX idx_order_created (created_at)
);

-- Order items table
CREATE TABLE IF NOT EXISTS order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(255),
    unit_price DECIMAL(10,2) NOT NULL,
    quantity INT NOT NULL,
    
    INDEX idx_order_item_order (order_id),
    INDEX idx_order_item_product (product_id),
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id)
);

-- Inventory events table (for Kafka event sourcing)
CREATE TABLE IF NOT EXISTS inventory_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(255) UNIQUE NOT NULL,
    product_id BIGINT NOT NULL,
    quantity_change INT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    reason TEXT,
    order_id BIGINT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed BOOLEAN DEFAULT FALSE,
    
    INDEX idx_inventory_product (product_id),
    INDEX idx_inventory_type (event_type),
    INDEX idx_inventory_timestamp (timestamp),
    INDEX idx_inventory_processed (processed),
    FOREIGN KEY (product_id) REFERENCES products(id)
);

-- Payments table
CREATE TABLE IF NOT EXISTS payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id VARCHAR(255) UNIQUE NOT NULL,
    order_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(50),
    gateway_response TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_payment_order (order_id),
    INDEX idx_payment_status (status),
    FOREIGN KEY (order_id) REFERENCES orders(id)
);

-- Health check and metrics tables
CREATE TABLE IF NOT EXISTS health_checks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    service_name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    details JSON,
    checked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_health_service (service_name),
    INDEX idx_health_status (status),
    INDEX idx_health_time (checked_at)
);

-- Insert sample data for testing
INSERT INTO products (name, description, price, stock_quantity, category, image_url) VALUES
('iPhone 15 Pro', 'Latest Apple smartphone with advanced features', 1199.99, 50, 'Electronics', 'https://example.com/iphone15.jpg'),
('MacBook Pro 16"', 'Powerful laptop for professionals', 2499.99, 25, 'Electronics', 'https://example.com/macbook.jpg'),
('AirPods Pro', 'Wireless earbuds with noise cancellation', 249.99, 100, 'Electronics', 'https://example.com/airpods.jpg'),
('The Great Gatsby', 'Classic American novel', 12.99, 200, 'Books', 'https://example.com/gatsby.jpg'),
('1984', 'Dystopian social science fiction novel', 13.99, 150, 'Books', 'https://example.com/1984.jpg'),
('Nike Air Max', 'Comfortable running shoes', 129.99, 75, 'Clothing', 'https://example.com/nike.jpg'),
('Levi\'s Jeans', 'Classic denim jeans', 79.99, 120, 'Clothing', 'https://example.com/levis.jpg'),
('Coffee Mug', 'Ceramic coffee mug', 9.99, 300, 'Home', 'https://example.com/mug.jpg'),
('Desk Lamp', 'LED desk lamp with adjustable brightness', 45.99, 60, 'Home', 'https://example.com/lamp.jpg'),
('Wireless Mouse', 'Ergonomic wireless computer mouse', 29.99, 150, 'Electronics', 'https://example.com/mouse.jpg');

-- Insert some low stock products for testing
INSERT INTO products (name, description, price, stock_quantity, category, image_url) VALUES
('Limited Edition Watch', 'Exclusive timepiece with limited availability', 599.99, 3, 'Accessories', 'https://example.com/watch.jpg'),
('Rare Book Collection', 'Collection of rare first edition books', 299.99, 2, 'Books', 'https://example.com/rarebooks.jpg'),
('Vintage Guitar', 'Classic acoustic guitar from the 1970s', 899.99, 1, 'Music', 'https://example.com/guitar.jpg');

-- Create indexes for better performance
CREATE INDEX idx_products_composite ON products(active, category, stock_quantity);
CREATE INDEX idx_orders_composite ON orders(status, created_at);
CREATE INDEX idx_inventory_composite ON inventory_events(product_id, event_type, timestamp);

-- Create a view for product inventory summary
CREATE VIEW product_inventory_summary AS
SELECT 
    p.id,
    p.name,
    p.category,
    p.stock_quantity,
    COALESCE(SUM(CASE WHEN ie.event_type = 'STOCK_RESERVED' THEN ie.quantity_change ELSE 0 END), 0) as reserved_stock,
    COALESCE(SUM(CASE WHEN ie.event_type = 'STOCK_REPLENISHED' THEN ie.quantity_change ELSE 0 END), 0) as replenished_stock,
    p.stock_quantity + COALESCE(SUM(ie.quantity_change), 0) as calculated_stock
FROM products p
LEFT JOIN inventory_events ie ON p.id = ie.product_id
WHERE p.active = TRUE
GROUP BY p.id, p.name, p.category, p.stock_quantity;

-- Create a stored procedure for stock validation
DELIMITER //
CREATE PROCEDURE ValidateAndReduceStock(
    IN p_product_id BIGINT,
    IN p_quantity INT,
    OUT p_success BOOLEAN
)
BEGIN
    DECLARE current_stock INT;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_success = FALSE;
    END;

    START TRANSACTION;
    
    SELECT stock_quantity INTO current_stock 
    FROM products 
    WHERE id = p_product_id AND active = TRUE
    FOR UPDATE;
    
    IF current_stock >= p_quantity THEN
        UPDATE products 
        SET stock_quantity = stock_quantity - p_quantity,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = p_product_id;
        
        SET p_success = TRUE;
        COMMIT;
    ELSE
        SET p_success = FALSE;
        ROLLBACK;
    END IF;
END //
DELIMITER ;

-- Create triggers for audit logging
CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    table_name VARCHAR(50) NOT NULL,
    operation VARCHAR(10) NOT NULL,
    record_id BIGINT NOT NULL,
    old_values JSON,
    new_values JSON,
    changed_by VARCHAR(100),
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_audit_table (table_name),
    INDEX idx_audit_operation (operation),
    INDEX idx_audit_time (changed_at)
);

-- Trigger for product updates
DELIMITER //
CREATE TRIGGER products_audit_trigger
AFTER UPDATE ON products
FOR EACH ROW
BEGIN
    INSERT INTO audit_log (table_name, operation, record_id, old_values, new_values)
    VALUES (
        'products',
        'UPDATE',
        NEW.id,
        JSON_OBJECT(
            'name', OLD.name,
            'price', OLD.price,
            'stock_quantity', OLD.stock_quantity,
            'active', OLD.active
        ),
        JSON_OBJECT(
            'name', NEW.name,
            'price', NEW.price,
            'stock_quantity', NEW.stock_quantity,
            'active', NEW.active
        )
    );
END //
DELIMITER ;

-- Grant necessary permissions
GRANT SELECT, INSERT, UPDATE, DELETE ON autohealx.* TO 'autohealx'@'%';
GRANT EXECUTE ON PROCEDURE autohealx.ValidateAndReduceStock TO 'autohealx'@'%';

COMMIT;