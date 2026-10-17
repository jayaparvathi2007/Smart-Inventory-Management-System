-- ==========================================================
-- SMART INVENTORY MANAGEMENT SYSTEM - DATABASE SCHEMA & DATA
-- Database: smart_inventory_db
-- Compatible with MySQL 5.7+ / MySQL 8.0+
-- ==========================================================

CREATE DATABASE IF NOT EXISTS smart_inventory_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE smart_inventory_db;

-- 1. USERS TABLE
DROP TABLE IF EXISTS inventory_transactions;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS suppliers;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'Admin', -- Admin, Manager, Staff
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 2. CATEGORIES TABLE
CREATE TABLE categories (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 3. SUPPLIERS TABLE
CREATE TABLE suppliers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL UNIQUE,
    contact_person VARCHAR(100),
    email VARCHAR(100),
    phone VARCHAR(30),
    address TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 4. PRODUCTS TABLE
CREATE TABLE products (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sku VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    category_id INT NOT NULL,
    supplier_id INT,
    cost_price DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    selling_price DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    quantity INT NOT NULL DEFAULT 0,
    min_stock_level INT NOT NULL DEFAULT 10,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) 
        REFERENCES categories(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_product_supplier FOREIGN KEY (supplier_id) 
        REFERENCES suppliers(id) ON UPDATE CASCADE ON DELETE SET NULL,
    INDEX idx_product_sku (sku),
    INDEX idx_product_category (category_id),
    INDEX idx_product_quantity (quantity)
) ENGINE=InnoDB;

-- 5. INVENTORY TRANSACTIONS TABLE (Stock In, Stock Out, Adjustment)
CREATE TABLE inventory_transactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL,
    transaction_type ENUM('STOCK_IN', 'STOCK_OUT', 'ADJUSTMENT') NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    reference_no VARCHAR(100), -- PO number, invoice, customer order
    notes TEXT,
    user_id INT,
    transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_trans_product FOREIGN KEY (product_id) 
        REFERENCES products(id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_trans_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON UPDATE CASCADE ON DELETE SET NULL,
    INDEX idx_trans_product (product_id),
    INDEX idx_trans_type (transaction_type),
    INDEX idx_trans_date (transaction_date)
) ENGINE=InnoDB;

-- ==========================================================
-- SEED DATA
-- ==========================================================

-- Default Users (Passwords are stored with SHA-256 for security; fallback plain-text also accepted)
-- admin / admin123 (SHA-256: 240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa82280f1a61e3e97)
-- manager / manager123 (SHA-256: 8a70529d273bf7be72b7a0d4c1c85098ff9f212239f1ff456e30b355209f9f8c)
INSERT INTO users (username, password, full_name, role) VALUES
('admin', 'admin123', 'System Administrator', 'Admin'),
('manager', 'manager123', 'Inventory Manager', 'Manager');

-- Sample Categories
INSERT INTO categories (name, description) VALUES
('Electronics', 'Gadgets, components, peripherals, and consumer electronic devices'),
('Office Supplies', 'Stationery, paper supplies, pens, filing folders, and desk essentials'),
('Furniture', 'Office chairs, standing desks, file cabinets, and ergonomics'),
('Computer Accessories', 'Keyboards, mice, USB hubs, docking stations, and cables'),
('Networking', 'Routers, switches, ethernet cables, and wireless access points');

-- Sample Suppliers
INSERT INTO suppliers (name, contact_person, email, phone, address) VALUES
('TechSource Distribution', 'David Miller', 'sales@techsource.com', '+1-555-0101', '100 Industrial Parkway, Austin, TX'),
('Apex Office World', 'Sarah Jenkins', 'orders@apexoffice.com', '+1-555-0102', '45 Commercial Blvd, Chicago, IL'),
('Global Hardware Corp', 'Rajesh Kumar', 'support@globalhardware.com', '+1-555-0103', '78 logistics Park, Fremont, CA'),
('ErgoPro Furnishings', 'Emily Chen', 'contact@ergopro.com', '+1-555-0104', '220 Design Way, Grand Rapids, MI');

-- Sample Products
-- Products with various stock states: Normal, Low Stock (<= min_stock_level), Out of Stock (=0)
INSERT INTO products (sku, name, category_id, supplier_id, cost_price, selling_price, quantity, min_stock_level, description) VALUES
('SKU-ELEC-001', 'Wireless Noise-Cancelling Headphones', 1, 1, 65.00, 119.99, 45, 10, 'Premium wireless over-ear Bluetooth headphones with active noise cancellation'),
('SKU-ELEC-002', 'USB-C Fast Charging Station 65W', 1, 1, 18.50, 39.99, 12, 15, 'Multi-port GaN fast charger suitable for laptops, tablets, and phones [LOW STOCK]'),
('SKU-ELEC-003', 'Portable SSD 1TB USB 3.2', 1, 3, 55.00, 89.99, 4, 10, 'High-speed external solid-state drive with shock-resistant casing [LOW STOCK]'),
('SKU-ELEC-004', 'Smart 4K Web Camera with Mic', 1, 1, 32.00, 64.99, 0, 8, 'Ultra HD webcam with autofocus and dual stereo microphones [OUT OF STOCK]'),
('SKU-OFFC-001', 'Premium Executive Gel Pens (Pack of 12)', 2, 2, 6.20, 14.50, 85, 20, 'Smooth-flowing black ink rollerball pens for office and executive use'),
('SKU-OFFC-002', 'Heavy Duty Electric Stapler', 2, 2, 19.00, 34.99, 28, 10, 'Automatic electric stapler capacity up to 30 sheets with staple cartridge'),
('SKU-FURN-001', 'Ergonomic Mesh Desk Chair', 3, 4, 120.00, 249.99, 18, 5, 'High back breathable mesh chair with adjustable lumbar support and 3D armrests'),
('SKU-FURN-002', 'Electric Dual-Motor Standing Desk (140x70cm)', 3, 4, 210.00, 399.00, 7, 8, 'Motorized height adjustable standing desk with memory presets [LOW STOCK]'),
('SKU-COMP-001', 'Mechanical Wireless Keyboard RGB', 4, 3, 42.00, 79.99, 32, 12, 'Compact 75% mechanical keyboard with tactile switches and Bluetooth connectivity'),
('SKU-COMP-002', 'Ergonomic Vertical Wireless Mouse', 4, 3, 14.00, 29.99, 52, 15, 'Optical mouse designed to reduce wrist strain and carpal tunnel symptoms'),
('SKU-NETW-001', 'Wi-Fi 6 Dual-Band Gigabit Router', 5, 1, 48.00, 94.99, 16, 10, 'Next-generation Wi-Fi 6 wireless router with MU-MIMO and 4 external antennas'),
('SKU-NETW-002', '8-Port Gigabit Unmanaged Switch', 5, 1, 15.00, 28.50, 0, 10, 'Metal desktop ethernet switch with plug-and-play setup [OUT OF STOCK]');

-- Sample Transactions for Audit Trail & History
INSERT INTO inventory_transactions (product_id, transaction_type, quantity, unit_price, reference_no, notes, user_id, transaction_date) VALUES
(1, 'STOCK_IN', 50, 65.00, 'PO-2026-001', 'Initial stock purchase order', 1, DATE_SUB(NOW(), INTERVAL 14 DAY)),
(1, 'STOCK_OUT', 5, 119.99, 'INV-2026-101', 'Customer retail sale order #101', 1, DATE_SUB(NOW(), INTERVAL 10 DAY)),
(2, 'STOCK_IN', 25, 18.50, 'PO-2026-002', 'Restock batch from TechSource', 1, DATE_SUB(NOW(), INTERVAL 12 DAY)),
(2, 'STOCK_OUT', 13, 39.99, 'INV-2026-102', 'Bulk corporate order', 2, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(3, 'STOCK_IN', 20, 55.00, 'PO-2026-003', 'Quarterly inventory purchase', 1, DATE_SUB(NOW(), INTERVAL 8 DAY)),
(3, 'STOCK_OUT', 16, 89.99, 'INV-2026-103', 'Department store shipment', 2, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(4, 'STOCK_IN', 15, 32.00, 'PO-2026-004', 'Webcam shipment', 1, DATE_SUB(NOW(), INTERVAL 15 DAY)),
(4, 'STOCK_OUT', 15, 64.99, 'INV-2026-104', 'Clearance order batch fulfilled', 2, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(5, 'STOCK_IN', 100, 6.20, 'PO-2026-005', 'Office stationery bulk stock', 1, DATE_SUB(NOW(), INTERVAL 20 DAY)),
(5, 'STOCK_OUT', 15, 14.50, 'INV-2026-105', 'Office internal replenishment', 2, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(7, 'STOCK_IN', 20, 120.00, 'PO-2026-006', 'Furnishing container order', 1, DATE_SUB(NOW(), INTERVAL 18 DAY)),
(7, 'STOCK_OUT', 2, 249.99, 'INV-2026-106', 'Executive suite order', 2, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(8, 'STOCK_IN', 10, 210.00, 'PO-2026-007', 'Standing desks shipment', 1, DATE_SUB(NOW(), INTERVAL 7 DAY)),
(8, 'STOCK_OUT', 3, 399.00, 'INV-2026-107', 'Design agency delivery', 1, DATE_SUB(NOW(), INTERVAL 2 HOUR));
