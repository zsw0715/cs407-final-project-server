-- V1__init_schema.sql
-- User table
CREATE TABLE IF NOT EXISTS user (
    -- user basic info
    userid BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password_hash VARCHAR(255) NOT NULL COMMENT '加密后的密码',
    nickname VARCHAR(100) COMMENT '昵称',
    email VARCHAR(100) UNIQUE COMMENT '电子邮件地址',
    gender ENUM('MALE', 'FEMALE') COMMENT '性别',
    age INT COMMENT '年龄',
    birthdate DATE COMMENT '出生日期',
    avatar_url VARCHAR(255) COMMENT '头像URL',
    account_status ENUM('ACTIVE', 'SUSPENDED', 'DELETED') DEFAULT 'ACTIVE' COMMENT '账号状态',

    -- Location tracking fields
    last_latitude DECIMAL(10, 8) COMMENT '最后纬度',
    last_longitude DECIMAL(10, 8) COMMENT '最后经度',
    last_location_update TIMESTAMP NULL COMMENT '最后位置更新时间',

    -- social features
    status_message VARCHAR(255) COMMENT '状态签名',
    last_online_time TIMESTAMP NULL COMMENT '最后在线时间',

    -- privacy settings
    discoverable BOOLEAN DEFAULT true COMMENT '是否可被搜索',
    privacy_level ENUM('PUBLIC', 'FRIENDS_ONLY', 'BAOBAO_ONLY', 'PRIVATE') DEFAULT 'FRIENDS_ONLY' COMMENT '隐私级别 - 全部公开，好友可见，仅情侣，完全私密',

    -- ssytem settings
    device_id VARCHAR(255) COMMENT '设备ID',

    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '账号创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '账号更新时间（例如更改密码）',

    -- Indexes
    INDEX idx_username (username),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='user table';