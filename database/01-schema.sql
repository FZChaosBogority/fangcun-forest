-- 方寸森林基础框架数据库草案，不是课程原始 DDL。
-- 仅用于新建的空库；不含 DROP/TRUNCATE，不在应用启动时自动执行。
-- MySQL 8.0.16+；先执行本文件，再执行 02-seed.sql。
SET NAMES utf8mb4;
CREATE DATABASE IF NOT EXISTS fangcun_forest
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE fangcun_forest;

CREATE TABLE store_user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(50) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    email VARCHAR(120) NULL,
    phone VARCHAR(20) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_username (username),
    CONSTRAINT chk_user_role CHECK (role IN ('CUSTOMER', 'ADMIN')),
    CONSTRAINT chk_user_status CHECK (status IN ('ENABLED', 'DISABLED'))
) ENGINE=InnoDB;

CREATE TABLE category (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(40) NOT NULL,
    parent_id BIGINT NULL,
    parent_key BIGINT GENERATED ALWAYS AS (COALESCE(parent_id, 0)) STORED,
    name VARCHAR(80) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_category_code (code),
    UNIQUE KEY uk_category_sibling_name (parent_key, name),
    KEY idx_category_parent (parent_id),
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES category (id),
    CONSTRAINT chk_category_sort CHECK (sort_order >= 0)
) ENGINE=InnoDB;

CREATE TABLE product (
    id BIGINT NOT NULL AUTO_INCREMENT,
    sku VARCHAR(40) NOT NULL,
    category_id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    summary VARCHAR(500) NULL,
    description TEXT NULL,
    cover_image VARCHAR(255) NOT NULL DEFAULT '/images/terrarium.svg',
    price DECIMAL(12,2) NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    sort_order INT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_sku (sku),
    KEY idx_product_category_status (category_id, status),
    KEY idx_product_display (status, sort_order, id),
    KEY idx_product_created (status, created_at, id),
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES category (id),
    CONSTRAINT chk_product_price CHECK (price > 0),
    CONSTRAINT chk_product_stock CHECK (stock >= 0),
    CONSTRAINT chk_product_sort CHECK (sort_order >= 0),
    CONSTRAINT chk_product_version CHECK (version >= 0),
    CONSTRAINT chk_product_status CHECK (status IN ('DRAFT', 'ON_SALE'))
) ENGINE=InnoDB;

CREATE TABLE product_image (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    image_path VARCHAR(255) NOT NULL,
    alt_text VARCHAR(120) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_image_product_sort (product_id, sort_order, id),
    CONSTRAINT fk_image_product FOREIGN KEY (product_id) REFERENCES product (id),
    CONSTRAINT chk_image_sort CHECK (sort_order >= 0)
) ENGINE=InnoDB;

CREATE TABLE cart_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_cart_user_product (user_id, product_id),
    CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES store_user (id),
    CONSTRAINT fk_cart_product FOREIGN KEY (product_id) REFERENCES product (id),
    CONSTRAINT chk_cart_quantity CHECK (quantity BETWEEN 1 AND 99),
    CONSTRAINT chk_cart_version CHECK (version >= 0)
) ENGINE=InnoDB;

CREATE TABLE sales_order (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_no VARCHAR(40) NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_PAYMENT',
    total_amount DECIMAL(12,2) NOT NULL,
    receiver_name VARCHAR(50) NOT NULL,
    receiver_phone VARCHAR(20) NOT NULL,
    receiver_address VARCHAR(255) NOT NULL,
    payment_method VARCHAR(20) NULL,
    payment_no VARCHAR(64) NULL,
    paid_at DATETIME NULL,
    cancelled_at DATETIME NULL,
    shipped_at DATETIME NULL,
    completed_at DATETIME NULL,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    UNIQUE KEY uk_order_payment_no (payment_no),
    KEY idx_order_user_created (user_id, created_at, id),
    KEY idx_order_status_created (status, created_at, id),
    CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES store_user (id),
    CONSTRAINT chk_order_total CHECK (total_amount > 0),
    CONSTRAINT chk_order_version CHECK (version >= 0),
    CONSTRAINT chk_order_status CHECK (status IN
        ('PENDING_PAYMENT', 'PAID', 'SHIPPED', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_order_payment CHECK (
        (status IN ('PENDING_PAYMENT', 'CANCELLED')
            AND payment_method IS NULL AND payment_no IS NULL AND paid_at IS NULL)
        OR
        (status IN ('PAID', 'SHIPPED', 'COMPLETED')
            AND payment_method IS NOT NULL AND payment_method = 'MOCK'
            AND payment_no IS NOT NULL AND paid_at IS NOT NULL)
    )
) ENGINE=InnoDB;

CREATE TABLE sales_order_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    sku_snapshot VARCHAR(40) NOT NULL,
    product_name_snapshot VARCHAR(120) NOT NULL,
    cover_image_snapshot VARCHAR(255) NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    quantity INT NOT NULL,
    line_amount DECIMAL(12,2) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_item_product (order_id, product_id),
    KEY idx_order_item_product (product_id),
    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES sales_order (id),
    CONSTRAINT fk_order_item_product FOREIGN KEY (product_id) REFERENCES product (id),
    CONSTRAINT chk_order_item_price CHECK (unit_price > 0),
    CONSTRAINT chk_order_item_quantity CHECK (quantity BETWEEN 1 AND 99),
    CONSTRAINT chk_order_item_amount CHECK (line_amount = unit_price * quantity)
) ENGINE=InnoDB;
