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

-- Create messages table for chat system
CREATE TABLE IF NOT EXISTS messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NULL COMMENT '강의장 채팅용 (rooms.id 참조)',
    sender_id BIGINT NOT NULL COMMENT '발신자 (users.id 참조)',
    receiver_id BIGINT NULL COMMENT 'DM 수신자용 (users.id 참조)',
    message_type ENUM('CHAT', 'ANNOUNCEMENT', 'SYSTEM', 'DM') NOT NULL COMMENT '메시지 타입',
    content TEXT NOT NULL COMMENT '메시지 내용',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
    
    -- 외래키 제약조건
    FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE,
    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE CASCADE,
    
    -- 성능 최적화 인덱스
    INDEX idx_room_messages (room_id, created_at),
    INDEX idx_dm_messages (sender_id, receiver_id, created_at),
    INDEX idx_message_type (message_type, created_at),
    INDEX idx_sender_messages (sender_id, created_at),
    INDEX idx_receiver_messages (receiver_id, created_at)
);

-- Insert sample data (optional)
INSERT IGNORE INTO users (username, email, password) VALUES 
('admin', 'admin@vims.local', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iYqiSfFdEh.1ER4WS8YL/n6Fppnq'),
('user1', 'user1@vims.local', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iYqiSfFdEh.1ER4WS8YL/n6Fppnq'),
('user2', 'user2@vims.local', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iYqiSfFdEh.1ER4WS8YL/n6Fppnq');