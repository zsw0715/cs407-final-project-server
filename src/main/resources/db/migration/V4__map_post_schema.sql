-- V4__map_post_schema.sql（简化版）
-- Map Post功能：地图帖子表

-- ══════════════════════════════════════════════════════════════════════════════
-- Map Posts 表：地图上的社交帖子
-- ══════════════════════════════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS map_posts (
    map_post_id      BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '地图帖子ID',
    conv_id          BIGINT NOT NULL UNIQUE COMMENT '关联的评论区conversation_id',
    creator_id       BIGINT NOT NULL COMMENT '创建者用户ID（冗余字段，避免JOIN）',
    
    -- 内容信息
    title            VARCHAR(128) NOT NULL COMMENT '帖子标题',
    description      TEXT NULL COMMENT '帖子描述/正文',
    media_json       JSON NULL COMMENT '媒体URL数组，例: ["url1","url2"]',
    
    -- 位置信息（必需）
    loc_lat          DECIMAL(10,7) NOT NULL COMMENT '纬度',
    loc_lng          DECIMAL(10,7) NOT NULL COMMENT '经度',
    loc_name         VARCHAR(128) NULL COMMENT '位置名称，例: 三里屯SOHO',
    geohash          VARCHAR(12) NOT NULL COMMENT 'GeoHash索引，用于地理查询',
    
    -- 统计信息
    view_count       INT NOT NULL DEFAULT 0 COMMENT '浏览量（排除作者自己）',
    like_count       INT NOT NULL DEFAULT 0 COMMENT '点赞数',
    comment_count    INT NOT NULL DEFAULT 0 COMMENT '评论数',
    
    -- 状态管理（简化）
    status           TINYINT NOT NULL DEFAULT 1 COMMENT '0=已删除 1=已发布',
    
    -- 时间戳
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at       TIMESTAMP NULL COMMENT '删除时间（软删除）',
    
    -- 索引
    INDEX idx_geohash (geohash),
    INDEX idx_creator (creator_id),
    INDEX idx_status_geohash (status, geohash),
    INDEX idx_created_at (created_at),
    
    -- 外键约束
    CONSTRAINT fk_mappost_conv FOREIGN KEY (conv_id)
        REFERENCES conversations(conv_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_mappost_creator FOREIGN KEY (creator_id)
        REFERENCES `user`(userid) ON DELETE CASCADE ON UPDATE CASCADE
        
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='地图帖子表';

-- ══════════════════════════════════════════════════════════════════════════════
-- 更新 conversations 表注释
-- ══════════════════════════════════════════════════════════════════════════════
ALTER TABLE conversations 
    MODIFY COLUMN conv_type TINYINT NOT NULL COMMENT '会话类型：1=单聊 2=群聊 3=地图帖子评论区';

-- ══════════════════════════════════════════════════════════════════════════════
-- Map Post Likes 表：点赞记录
-- ══════════════════════════════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS map_post_likes (
    map_post_id      BIGINT NOT NULL COMMENT '地图帖子ID',
    userid           BIGINT NOT NULL COMMENT '点赞用户ID',
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    
    PRIMARY KEY (map_post_id, userid),
    INDEX idx_user (userid),
    
    CONSTRAINT fk_like_mappost FOREIGN KEY (map_post_id)
        REFERENCES map_posts(map_post_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_like_user FOREIGN KEY (userid)
        REFERENCES `user`(userid) ON DELETE CASCADE ON UPDATE CASCADE
        
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='地图帖子点赞表';

