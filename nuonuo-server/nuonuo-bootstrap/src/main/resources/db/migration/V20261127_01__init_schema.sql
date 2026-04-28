-- ============================================================================
-- 小糯智肤 (nuonuo-agent) 初始化建表脚本
-- 规范依据: 《Java 后端开发规范手册》1.1 & 《阿里巴巴 Java 开发手册》1.3.0
-- 数据库版本: MySQL 8.4 LTS
-- ============================================================================

-- 1. 用户基础信息表 (所属模块: user)
CREATE TABLE `user_account` (
    `id` BIGINT NOT NULL COMMENT '雪花算法主键ID',
    `user_id` VARCHAR(64) NOT NULL COMMENT '用户业务唯一标识',
    `nickname` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '用户昵称',
    `is_deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标识: 0(未删除), 1(已删除)',
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户基础信息表';

-- 2. 用户画像表 (所属模块: memory)
CREATE TABLE `user_profile_memory` (
    `id` BIGINT NOT NULL COMMENT '雪花算法主键ID',
    `user_id` VARCHAR(64) NOT NULL COMMENT '所属用户业务ID',
    `category` VARCHAR(32) NOT NULL COMMENT '画像类别: SKIN_TYPE(肤质), ALLERGY(禁忌过敏), PREFERENCE(偏好)',
    `profile_key` VARCHAR(64) NOT NULL COMMENT '画像项标识: 如 alcohol, retinol, skin_base',
    `profile_value` VARCHAR(255) NOT NULL COMMENT '画像值: 如 重度接触性泛红, 干性敏感肌',
    `confidence` DECIMAL(3, 2) NOT NULL DEFAULT 1.00 COMMENT '置信度 (0.00 ~ 1.00)',
    `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE(生效), DEPRECATED(已废弃), USER_DELETED(用户手动删除墓碑)',
    `version` INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '版本号(用于冲突更新版本迭代与乐观锁条件更新)',
    `source_session_id` VARCHAR(64) DEFAULT NULL COMMENT '来源会话ID',
    `is_deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标识: 0(未删除), 1(已删除)',
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_key_version` (`user_id`, `profile_key`, `version`),
    KEY `idx_user_status` (`user_id`, `status`),
    KEY `idx_user_key_status` (`user_id`, `profile_key`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户画像表';

-- 3. 成分标准字典表 (所属模块: ingredient)
CREATE TABLE `ingredient_dict` (
    `id` BIGINT NOT NULL COMMENT '雪花算法主键ID',
    `inci_name_cn` VARCHAR(128) NOT NULL COMMENT '中文标准名称',
    `inci_name_en` VARCHAR(128) NOT NULL DEFAULT '' COMMENT '英文标准名称',
    `alias_names` JSON DEFAULT NULL COMMENT '常见别名/商品名列表 (JSON 数组)',
    `category` VARCHAR(32) NOT NULL DEFAULT 'OTHER' COMMENT '化学大类: HIGH_ACID(强酸), RETINOIDS(A醇类), METAL_PEPTIDE(金属多肽), PURE_VC(原型维C), NIACINAMIDE(烟酰胺), GENTLE_HYDRATOR(温和保湿)',
    `ph_min` DECIMAL(3, 1) DEFAULT NULL COMMENT '适宜起效最低pH',
    `ph_max` DECIMAL(3, 1) DEFAULT NULL COMMENT '适宜起效最高pH',
    `irritancy_level` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '刺激度等级 (0:温和, 1:轻度, 2:强刺激)',
    `is_preservative` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否为防腐剂: 0(否), 1(是)',
    `is_sensitizer` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否为常见致敏原: 0(否), 1(是)',
    `is_deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标识: 0(未删除), 1(已删除)',
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_inci_name_cn` (`inci_name_cn`),
    KEY `idx_inci_name_en` (`inci_name_en`(64)),
    KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='成分标准字典表';

-- 4. 成分配伍禁忌规则表 (所属模块: ingredient，写入层强制按字典序规范化 A < B)
CREATE TABLE `ingredient_conflict_rule` (
    `id` BIGINT NOT NULL COMMENT '雪花算法主键ID',
    `rule_level` VARCHAR(16) NOT NULL DEFAULT 'SINGLE' COMMENT '规则级别: SINGLE(单体精准对), CATEGORY(大类理化冲突)',
    `ingredient_a` VARCHAR(128) DEFAULT NULL COMMENT '源成分名称 (字典序较小者，rule_level=SINGLE 时有效)',
    `ingredient_b` VARCHAR(128) DEFAULT NULL COMMENT '冲突成分名称 (字典序较大者，rule_level=SINGLE 时有效)',
    `category_a` VARCHAR(32) DEFAULT NULL COMMENT '源成分大类 (字典序较小者，rule_level=CATEGORY 时有效)',
    `category_b` VARCHAR(32) DEFAULT NULL COMMENT '冲突成分大类 (字典序较大者，rule_level=CATEGORY 时有效)',
    `conflict_level` VARCHAR(16) NOT NULL COMMENT '冲突等级: STRICT_FORBIDDEN(严禁同用), CAUTION(谨慎搭配)',
    `risk_mechanism` VARCHAR(255) NOT NULL COMMENT '风险机理',
    `handling_advice` VARCHAR(255) NOT NULL COMMENT '安全处置建议',
    `is_deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标识: 0(未删除), 1(已删除)',
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_single_pair` (`rule_level`, `ingredient_a`(64), `ingredient_b`(64)),
    UNIQUE KEY `uk_category_pair` (`rule_level`, `category_a`, `category_b`),
    KEY `idx_ingredient_a_b` (`ingredient_a`(64), `ingredient_b`(64)),
    KEY `idx_category_a_b` (`category_a`, `category_b`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='成分配伍禁忌规则表';

-- 5. 未收录成分组合审计日志表 (所属模块: ingredient)
CREATE TABLE `ingredient_audit_log` (
    `id` BIGINT NOT NULL COMMENT '雪花算法主键ID',
    `session_id` VARCHAR(64) NOT NULL COMMENT '触发会话ID',
    `user_id` VARCHAR(64) NOT NULL COMMENT '用户业务ID',
    `ingredient_names` JSON NOT NULL COMMENT '提取到的多元活性成分列表 (JSON 数组)',
    `category_tags` JSON DEFAULT NULL COMMENT '解析出的大类标签 (JSON 数组)',
    `audit_status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING(待评估), REVIEWED(已评估), RULE_CREATED(已入库), IGNORED(无需干预)',
    `suggested_mechanism` TEXT DEFAULT NULL COMMENT '离线药理模型推导评估结论',
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_audit_status_gmt_create` (`audit_status`, `gmt_create`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='未收录成分审计与规则自扩充日志表';

-- 6. 会话索引表 (所属模块: chat)
CREATE TABLE `chat_session` (
    `id` BIGINT NOT NULL COMMENT '雪花算法主键ID',
    `session_id` VARCHAR(64) NOT NULL COMMENT '会话业务唯一ID',
    `user_id` VARCHAR(64) NOT NULL COMMENT '用户业务ID',
    `title` VARCHAR(128) NOT NULL DEFAULT '新护肤咨询' COMMENT '会话标题',
    `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE(活跃), CLOSED(已完结), ARCHIVED(已归档)',
    `total_messages` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '消息总数',
    `archive_oss_path` VARCHAR(255) DEFAULT NULL COMMENT '冷存储OSS归档对象Key',
    `version` INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    `is_deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标识: 0(未删除), 1(已删除)',
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_session_id` (`session_id`),
    KEY `idx_user_id_gmt_create` (`user_id`, `gmt_create`),
    KEY `idx_status_gmt_create` (`status`, `gmt_create`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会话索引表';

-- 7. 历史对话消息表 (所属模块: chat)
CREATE TABLE `chat_message` (
    `id` BIGINT NOT NULL COMMENT '雪花算法主键ID',
    `message_id` VARCHAR(64) NOT NULL COMMENT '消息业务唯一ID',
    `session_id` VARCHAR(64) NOT NULL COMMENT '所属会话ID',
    `user_id` VARCHAR(64) NOT NULL COMMENT '用户业务ID',
    `role` VARCHAR(16) NOT NULL COMMENT '角色: user, assistant, system, tool',
    `content` TEXT NOT NULL COMMENT '消息正文',
    `structured_card` JSON DEFAULT NULL COMMENT '结构化卡片载荷 (如早晚护肤流程 JSON)',
    `media_object_keys` JSON DEFAULT NULL COMMENT '关联多模态素材OSS持久化对象Key列表 (JSON 数组)',
    `token_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '当条消息消耗Token估算值',
    `is_deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标识: 0(未删除), 1(已删除)',
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_message_id` (`message_id`),
    KEY `idx_session_id_gmt_create` (`session_id`, `gmt_create`),
    KEY `idx_user_id_gmt_create` (`user_id`, `gmt_create`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='历史对话消息表';

-- 8. 多模态素材元数据表 (所属模块: media)
CREATE TABLE `media_object` (
    `id` BIGINT NOT NULL COMMENT '雪花算法主键ID',
    `object_key` VARCHAR(255) NOT NULL COMMENT 'OSS 持久化对象Key',
    `user_id` VARCHAR(64) NOT NULL COMMENT '归属用户业务ID',
    `session_id` VARCHAR(64) DEFAULT NULL COMMENT '绑定会话ID',
    `mime_type` VARCHAR(64) NOT NULL COMMENT '媒体MIME类型 (如 image/jpeg, audio/wav)',
    `file_size` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '文件字节大小',
    `upload_status` VARCHAR(16) NOT NULL DEFAULT 'PRESIGNED' COMMENT '状态: PRESIGNED(已签发), UPLOADED(已上传), BOUND(已绑定消息), ORPHAN_DELETED(孤儿清理)',
    `expire_at` DATETIME DEFAULT NULL COMMENT '未绑定消息时的临时过期清理时间',
    `is_deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标识: 0(未删除), 1(已删除)',
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_object_key` (`object_key`(128)),
    KEY `idx_user_id_gmt_create` (`user_id`, `gmt_create`),
    KEY `idx_upload_status_expire_at` (`upload_status`, `expire_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='多模态素材元数据表';
