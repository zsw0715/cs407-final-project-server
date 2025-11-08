-- V5__update_map_post_schema_with_post_type.sql
-- 更新map_posts表，添加post_type字段

ALTER TABLE map_posts
ADD COLUMN post_type ENUM('ALL', 'REQUEST', 'COMMENT') NOT NULL DEFAULT 'ALL' COMMENT '帖子类型：ALL, REQUEST, COMMENT';
