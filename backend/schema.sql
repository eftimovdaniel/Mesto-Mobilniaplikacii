-- Izvrshi vo MySQL (na pr. mysql -u root -p < schema.sql)
CREATE DATABASE IF NOT EXISTS mesto CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE mesto;

CREATE TABLE IF NOT EXISTS companies (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address TEXT NOT NULL,
    latitude DECIMAL(10, 7) NOT NULL,
    longitude DECIMAL(10, 7) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(64) NOT NULL,
    website VARCHAR(512) NOT NULL,
    categories JSON NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
