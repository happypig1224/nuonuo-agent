ALTER TABLE `user_account`
    ADD COLUMN `password_hash` VARCHAR(128) NOT NULL DEFAULT '' COMMENT '密码哈希' AFTER `nickname`;
