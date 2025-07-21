-- MySQL initialization script for VIMS application

-- Create database if not exists
CREATE DATABASE IF NOT EXISTS vims CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Use the database
USE vims;

-- Create users table (example)
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Create rooms table (example)
CREATE TABLE IF NOT EXISTS rooms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_name VARCHAR(100) NOT NULL,
    room_id VARCHAR(36) NOT NULL UNIQUE,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (created_by) REFERENCES users(id)
);

-- Create participants table (example)
CREATE TABLE IF NOT EXISTS participants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    left_at TIMESTAMP NULL,
    FOREIGN KEY (room_id) REFERENCES rooms(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Insert sample data (optional)
INSERT IGNORE INTO users (username, email, password) VALUES 
('admin', 'admin@vims.local', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iYqiSfFdEh.1ER4WS8YL/n6Fppnq'),
('user1', 'user1@vims.local', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iYqiSfFdEh.1ER4WS8YL/n6Fppnq'),
('user2', 'user2@vims.local', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iYqiSfFdEh.1ER4WS8YL/n6Fppnq');