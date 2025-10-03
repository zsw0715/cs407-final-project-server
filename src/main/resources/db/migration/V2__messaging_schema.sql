-- V2__messaging_schema.sql

-- ──────────────────────────────────────────────────────────────────────────────
-- Conversations Table
-- type: 1=one-to-one, 2=group
-- ──────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS conversations (
    conv_id      BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '会话 ID',
    conv_type    TINYINT NOT NULL COMMENT '会话类型：1=单聊 2=群聊',
    title        VARCHAR(128) NULL COMMENT '会话标题；群聊时使用；单聊可空',
    creator_id   BIGINT NULL COMMENT '会话创建者用户ID',
    last_msg_id  BIGINT NULL COMMENT '最后一条消息ID（稍后用 ALTER 补外键）',
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '会话创建时间',
    updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '会话更新时间',

    INDEX idx_conv_type (conv_type),
    INDEX idx_update_at (updated_at),
    CONSTRAINT fk_conv_creator FOREIGN KEY (creator_id)
        REFERENCES `user`(userid) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话表';

-- ──────────────────────────────────────────────────────────────────────────────
-- single_conv_index：单聊唯一 (min(uid), max(uid)) → conv_id
-- ──────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS single_conv_index (
    user_min_id BIGINT NOT NULL COMMENT '用户ID较小者',
    user_max_id BIGINT NOT NULL COMMENT '用户ID较大者',
    conv_id     BIGINT NOT NULL COMMENT '单聊会话ID',
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '索引创建时间',

    PRIMARY KEY (user_min_id, user_max_id),
    UNIQUE KEY uniq_conv_id (conv_id),
    CONSTRAINT fk_single_conv_user_min FOREIGN KEY (user_min_id)
        REFERENCES `user`(userid) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_single_conv_user_max FOREIGN KEY (user_max_id)
        REFERENCES `user`(userid) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_single_conv_conv FOREIGN KEY (conv_id)
        REFERENCES conversations(conv_id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='单聊唯一索引表';

-- ──────────────────────────────────────────────────────────────────────────────
-- Conversation Members
-- role: 0=成员, 1=管理员, 2=群主
-- ──────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS conversation_members (
    conv_id     BIGINT NOT NULL COMMENT '会话 ID',
    userid      BIGINT NOT NULL COMMENT '用户 ID',
    conv_role   TINYINT NOT NULL DEFAULT 0 COMMENT '会话角色：0=成员,1=管理员,2=群主',
    joined_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',

    PRIMARY KEY (conv_id, userid),
    INDEX idx_member_user (userid),
    CONSTRAINT fk_cm_conv FOREIGN KEY (conv_id)
        REFERENCES conversations(conv_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_cm_user FOREIGN KEY (userid)
        REFERENCES `user`(userid) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话成员';

-- ──────────────────────────────────────────────────────────────────────────────
-- Messages：统一文本/媒体/位置/系统
-- msg_type: 0=TEXT, 1=IMAGE, 2=VIDEO, 3=LOCATION, 4=STICKER, 5=SYSTEM
-- ──────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS messages (
    msg_id           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '消息 ID',
    conv_id          BIGINT NOT NULL COMMENT '会话 ID',
    sender_id        BIGINT NOT NULL COMMENT '发送者ID',
    msg_type         TINYINT NOT NULL COMMENT '0=文本,1=图片,2=视频,3=位置,4=贴图,5=系统',
    client_msg_id    VARCHAR(64) NULL COMMENT '客户端消息ID（幂等）',

    -- 文本内容（文本/系统类用；媒体类可为空）
    content_text     TEXT NULL COMMENT '文本内容',

    -- 位置（任何消息可携带）
    loc_lat          DECIMAL(10,7) NULL,
    loc_lng          DECIMAL(10,7) NULL,
    loc_name         VARCHAR(128) NULL,
    loc_accuracy_m   FLOAT NULL,
    geohash          VARCHAR(12) NULL,

    -- 引用/回复
    reply_to_msg_id  BIGINT NULL,

    -- 单媒体字段
    media_url        VARCHAR(512) NULL,
    media_thumb_url  VARCHAR(512) NULL,
    media_meta_json  JSON NULL COMMENT '例: {"w":1080,"h":1920,"durationMs":0,"size":123456,"mime":"image/jpeg"}',

    -- 生命周期
    msg_status       TINYINT NOT NULL DEFAULT 0 COMMENT '0=正常,1=撤回,2=已删',
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    edited_at        TIMESTAMP NULL,
    deleted_at       TIMESTAMP NULL,

    INDEX idx_conv_time (conv_id, created_at),
    INDEX idx_sender_time (sender_id, created_at),
    UNIQUE KEY uk_conv_client (conv_id, client_msg_id),

    CONSTRAINT fk_msg_conv FOREIGN KEY (conv_id)
        REFERENCES conversations(conv_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_msg_sender FOREIGN KEY (sender_id)
        REFERENCES `user`(userid) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_msg_reply FOREIGN KEY (reply_to_msg_id)
        REFERENCES messages(msg_id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';

-- ──────────────────────────────────────────────────────────────────────────────
-- Message Attachments（可选：多图/多视频/文件）
-- kind: 1=image, 2=video, 3=file
-- ──────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS message_attachments (
    att_id      BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '附件ID',
    msg_id      BIGINT NOT NULL COMMENT '所属消息ID',
    kind        TINYINT NOT NULL COMMENT '1=image,2=video,3=file',
    url         VARCHAR(512) NOT NULL COMMENT '资源URL',
    thumb_url   VARCHAR(512) NULL COMMENT '缩略图URL',
    meta_json   JSON NULL COMMENT '元信息：{"w":...,"h":...,"durationMs":...,"size":...,"mime":"..."}',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号，从0开始',

    INDEX idx_att_msg (msg_id),
    UNIQUE KEY uk_msg_sort (msg_id, sort_no),
    CONSTRAINT fk_att_msg FOREIGN KEY (msg_id)
        REFERENCES messages(msg_id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息附件表';

-- ──────────────────────────────────────────────────────────────────────────────
-- Message Receipts：投递/已读回执（每条消息×每个接收者一行）
-- ──────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS message_receipts (
    msg_id        BIGINT NOT NULL COMMENT '消息ID',
    userid        BIGINT NOT NULL COMMENT '接收者用户ID',
    delivered_at  TIMESTAMP NULL COMMENT '送达时间',

    PRIMARY KEY (msg_id, userid),
    INDEX idx_receipt_user (userid),
    CONSTRAINT fk_rcp_msg FOREIGN KEY (msg_id)
        REFERENCES messages(msg_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_rcp_user FOREIGN KEY (userid)
        REFERENCES `user`(userid) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息回执表';

-- ──────────────────────────────────────────────────────────────────────────────
-- 最后补上 conversations.last_msg_id → messages(msg_id) 外键（此时 messages 已存在）
-- ──────────────────────────────────────────────────────────────────────────────
ALTER TABLE conversations
    ADD CONSTRAINT fk_conv_last_msg
    FOREIGN KEY (last_msg_id) REFERENCES messages(msg_id)
    ON DELETE SET NULL ON UPDATE CASCADE;