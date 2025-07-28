-- MySQL initialization script for VIMS application

-- Create database if not exists
CREATE DATABASE IF NOT EXISTS vims CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Use the database
USE vims;

-- 1. Create database
CREATE DATABASE IF NOT EXISTS vims CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE vims;

-- 2. users
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    oauth_provider ENUM('GOOGLE', 'LOCAL'),
    oauth_id VARCHAR(255),
    username VARCHAR(100),
    email VARCHAR(255) NOT NULL UNIQUE,
    profile_image_url VARCHAR(512),
    role ENUM('GUEST', 'GENERAL') NOT NULL,
    password_hash VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_users_email (email)
);

-- 3. courses
CREATE TABLE IF NOT EXISTS courses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    instructor_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (instructor_id) REFERENCES users(id)
);

-- 4. rooms
CREATE TABLE IF NOT EXISTS rooms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    room_code VARCHAR(20) NOT NULL UNIQUE,
    host_user_id BIGINT NOT NULL,
    course_id BIGINT,
    max_participants INT DEFAULT 10,
    password VARCHAR(255),
    is_recording_enabled BOOLEAN DEFAULT FALSE,
    scheduled_start_time TIMESTAMP,
    scheduled_end_time TIMESTAMP,
    is_open_to_everyone BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (host_user_id) REFERENCES users(id),
    FOREIGN KEY (course_id) REFERENCES courses(id)
);

-- 5. participants
CREATE TABLE IF NOT EXISTS participants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    user_id BIGINT,
    guest_name VARCHAR(100),
    joined_at TIMESTAMP,
    left_at TIMESTAMP,
    is_presenter BOOLEAN DEFAULT FALSE,
    total_duration INT DEFAULT 0,

    FOREIGN KEY (room_id) REFERENCES rooms(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 6. messages
CREATE TABLE IF NOT EXISTS messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NULL COMMENT '강의장 채팅용 (rooms.id 참조)',
    sender_id BIGINT NOT NULL COMMENT '발신자 (users.id 참조)',
    receiver_id BIGINT NULL COMMENT 'DM 수신자용 (users.id 참조)',
    message_type ENUM('CHAT', 'ANNOUNCEMENT', 'SYSTEM', 'DM') NOT NULL COMMENT '메시지 타입',
    content TEXT NOT NULL COMMENT '메시지 내용',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',

    FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE,
    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE CASCADE,

    INDEX idx_room_messages (room_id, created_at),
    INDEX idx_dm_messages (sender_id, receiver_id, created_at),
    INDEX idx_message_type (message_type, created_at),
    INDEX idx_sender_messages (sender_id, created_at),
    INDEX idx_receiver_messages (receiver_id, created_at)
);

-- 7. recordings
CREATE TABLE IF NOT EXISTS recordings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    file_name VARCHAR(255),
    file_path VARCHAR(1024),
    file_size BIGINT,
    duration INT,
    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (room_id) REFERENCES rooms(id)
);

-- 8. recording_comment
CREATE TABLE IF NOT EXISTS recording_comment (
    comment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    content VARCHAR(3000),
    recording_id BIGINT NOT NULL,
    parent_comment_id BIGINT NULL,
    writer_id BIGINT NOT NULL,
    deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (recording_id) REFERENCES recordings(id),
    FOREIGN KEY (parent_comment_id) REFERENCES recording_comment(comment_id),
    FOREIGN KEY (writer_id) REFERENCES users(id)
);

-- 9. recording_like
CREATE TABLE IF NOT EXISTS recording_like (
    recording_like_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recording_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (recording_id) REFERENCES recordings(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    
    UNIQUE KEY uk_recording_user (recording_id, user_id)
);

-- 10. recording_like_count
CREATE TABLE IF NOT EXISTS recording_like_count (
    recording_id BIGINT PRIMARY KEY,
    like_count INT DEFAULT 0,

    FOREIGN KEY (recording_id) REFERENCES recordings(id)
);

-- 11. course_students
CREATE TABLE IF NOT EXISTS course_students (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    email VARCHAR(255) NOT NULL,
    name VARCHAR(100),
    user_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (course_id) REFERENCES courses(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 12. 샘플 사용자 추가
INSERT INTO users (oauth_provider, oauth_id, username, email, profile_image_url, role, password_hash)
VALUES 
  ('GOOGLE', 'oauth123', 'Alice', 'alice@example.com', '', 'GENERAL', 'hash1'),
  ('LOCAL', 'oauth456', 'Bob', 'bob@example.com', '', 'GENERAL', 'hash2'),
  ('GOOGLE', 'oauth789', 'Charlie', 'charlie@example.com', '', 'GUEST', NULL);

-- 13. 샘플 룸 추가 (Alice, Bob이 방장)
INSERT INTO rooms (title, description, room_code, host_user_id, max_participants, is_recording_enabled, is_open_to_everyone)
VALUES
  ('Spring Boot 기초 강의', '스프링 부트 기초 개념을 다룹니다', 'ROOM001', 1, 20, TRUE, TRUE),
  ('WebRTC 실습방', 'WebRTC를 이용한 화상 채팅 실습', 'ROOM002', 2, 10, FALSE, FALSE);