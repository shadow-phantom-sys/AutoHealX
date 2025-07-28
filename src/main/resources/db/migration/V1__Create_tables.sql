-- Create products table
CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    sku VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    price DECIMAL(12,2) NOT NULL,
    category VARCHAR(50) NOT NULL,
    stock_quantity INT NOT NULL DEFAULT 0,
    image_url VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    weight DOUBLE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    
    INDEX idx_product_name (name),
    INDEX idx_product_category (category),
    INDEX idx_product_sku (sku)
);

-- Create product tags table
CREATE TABLE product_tags (
    product_id BIGINT NOT NULL,
    tag VARCHAR(100) NOT NULL,
    
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    INDEX idx_product_tags_product_id (product_id),
    INDEX idx_product_tags_tag (tag)
);

-- Create customers table
CREATE TABLE customers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_customer_email (email),
    INDEX idx_customer_name (last_name, first_name)
);

-- Create orders table
CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_amount DECIMAL(12,2) NOT NULL,
    tax_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    shipping_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (customer_id) REFERENCES customers(id),
    INDEX idx_order_customer (customer_id),
    INDEX idx_order_number (order_number),
    INDEX idx_order_status (status),
    INDEX idx_order_created_at (created_at)
);

-- Create order items table
CREATE TABLE order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    total_price DECIMAL(12,2) NOT NULL,
    
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id),
    INDEX idx_order_items_order (order_id),
    INDEX idx_order_items_product (product_id)
);

-- Insert sample data
INSERT INTO products (name, sku, description, price, category, stock_quantity, active) VALUES
('Laptop Pro 15"', 'LAP-PRO-15', 'High-performance laptop with 15-inch display', 1299.99, 'ELECTRONICS', 50, TRUE),
('Wireless Mouse', 'MOUSE-WL-001', 'Ergonomic wireless mouse with precision tracking', 29.99, 'ELECTRONICS', 200, TRUE),
('Office Chair', 'CHAIR-OFF-001', 'Comfortable ergonomic office chair', 199.99, 'HOME_GARDEN', 25, TRUE),
('Programming Book', 'BOOK-PROG-001', 'Complete guide to modern programming', 49.99, 'BOOKS', 100, TRUE),
('Running Shoes', 'SHOES-RUN-001', 'Lightweight running shoes for athletes', 89.99, 'SPORTS', 75, TRUE);

INSERT INTO customers (email, first_name, last_name, phone, city, country) VALUES
('john.doe@example.com', 'John', 'Doe', '+1-555-0123', 'New York', 'USA'),
('jane.smith@example.com', 'Jane', 'Smith', '+1-555-0124', 'Los Angeles', 'USA'),
('bob.wilson@example.com', 'Bob', 'Wilson', '+1-555-0125', 'Chicago', 'USA');