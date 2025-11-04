-- V3__friend_schema.sql

-- ──────────────────────────────────────────────────────────────────────────────
 -- friend table
-- ──────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS friends (
    user_min_id  BIGINT NOT NULL COMMENT '用户ID较小者',
    user_max_id  BIGINT NOT NULL COMMENT '用户ID较大者',
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '成为好友时间',
    
    PRIMARY KEY (user_min_id, user_max_id),
    INDEX idx_user_min (user_min_id),
    INDEX idx_user_max (user_max_id),
    CONSTRAINT fk_friend_user_min FOREIGN KEY (user_min_id)
        REFERENCES `user`(userid) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_friend_user_max FOREIGN KEY (user_max_id)
        REFERENCES `user`(userid) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友关系表';

-- ──────────────────────────────────────────────────────────────────────────────
 -- friend request table
-- ──────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS friend_requests (
    req_id        BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '申请ID',
    req_sender_id  BIGINT NOT NULL COMMENT '发起申请的用户ID',
    req_receiver_id   BIGINT NOT NULL COMMENT '接收申请的用户ID',
    message       VARCHAR(255) NULL COMMENT '申请留言',
    status        TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0=待处理,1=已接受,2=已拒绝',
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '处理时间',
    
    UNIQUE KEY uk_req_pair (req_sender_id, req_receiver_id),
    INDEX idx_req_receiver_status (req_receiver_id, status),
    CONSTRAINT fk_req_sender FOREIGN KEY (req_sender_id)
        REFERENCES `user`(userid) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_req_receiver FOREIGN KEY (req_receiver_id)
        REFERENCES `user`(userid) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友申请表';



-- 1. 用户A 发送好友申请给 用户B
--    → INSERT INTO friend_requests (req_sender_id=A, req_receiver_id=B, status=0)

-- 2. 用户B 接受申请
--    → UPDATE friend_requests SET status=1
--    → INSERT INTO friends (user_min_id=min(A,B), user_max_id=max(A,B))
--    → 调用 getOrCreateSingleConv(A, B) 创建单聊（复用现有逻辑！）

-- 3. 用户B 拒绝申请
--    → UPDATE friend_requests SET status=2
