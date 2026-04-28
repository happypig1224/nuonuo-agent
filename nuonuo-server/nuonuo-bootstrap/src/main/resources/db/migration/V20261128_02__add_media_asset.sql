CREATE TABLE IF NOT EXISTS media_asset (
    id BIGINT NOT NULL COMMENT '主键ID',
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    session_id VARCHAR(64) NOT NULL COMMENT '会话ID',
    media_type VARCHAR(16) NOT NULL COMMENT '媒体类型: IMAGE / AUDIO',
    media_tag VARCHAR(32) DEFAULT NULL COMMENT '场景标签',
    storage_url VARCHAR(512) NOT NULL COMMENT '对象存储或本地访问URL',
    file_size BIGINT NOT NULL DEFAULT 0 COMMENT '文件字节大小',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_media_user_session (user_id, session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='多模态原始素材持久化表';
