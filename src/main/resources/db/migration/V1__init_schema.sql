-- V1__init_schema.sql
-- User table
CREATE TABLE IF NOT EXISTS user (
    userid BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'user ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT 'username',
    nickname VARCHAR(100) COMMENT 'nickname',
    age INT COMMENT 'age',
    gender ENUM('MALE', 'FEMALE') COMMENT 'gender',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'creation time',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
    INDEX idx_username (username),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='user table';