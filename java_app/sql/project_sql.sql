
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for chapter
-- ----------------------------
DROP TABLE IF EXISTS `chapter`;
CREATE TABLE `chapter`  (
                            `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                            `novel_id` bigint(0) NOT NULL COMMENT '所属小说ID',
                            `chapter_no` int(0) NOT NULL COMMENT '章节序号（从1开始）',
                            `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '章节标题',
                            `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '章节正文内容（最终发布版本）',
                            `meta` json NULL COMMENT '章节结构化元信息（人物/事件/伏笔/摘要等）',
                            `approved_from_draft_id` bigint(0) NOT NULL COMMENT '来源草稿ID（chapter_draft.id）',
                            `approved_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '审查通过并发布的时间',
                            `version` int(0) NOT NULL DEFAULT 1 COMMENT '章节版本号（用于乐观锁/发布迭代）',
                            PRIMARY KEY (`id`) USING BTREE,
                            UNIQUE INDEX `uk_novel_chapter`(`novel_id`, `chapter_no`) USING BTREE,
                            INDEX `idx_novel_id`(`novel_id`) USING BTREE,
                            CONSTRAINT `fk_chapter_novel` FOREIGN KEY (`novel_id`) REFERENCES `novel` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '正式章节表（仅存审查通过内容）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for chapter_draft
-- ----------------------------
DROP TABLE IF EXISTS `chapter_draft`;
CREATE TABLE `chapter_draft`  (
                                  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '草稿ID',
                                  `novel_id` bigint(0) NOT NULL COMMENT '所属小说ID',
                                  `chapter_no` int(0) NOT NULL COMMENT '章节序号',
                                  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '草稿章节标题',
                                  `draft_content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '草稿正文内容',
                                  `draft_meta` json NULL COMMENT '草稿阶段结构化信息（模型抽取/分析结果）',
                                  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '草稿状态（DRAFT/SUBMITTED/REVIEWING/REJECTED/APPROVED）',
                                  `created_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人（用户ID/系统标识）',
                                  `lock_version` int(0) NOT NULL DEFAULT 0 COMMENT '乐观锁版本号（并发控制）',
                                  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
                                  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
                                  PRIMARY KEY (`id`) USING BTREE,
                                  INDEX `idx_novel_chapter`(`novel_id`, `chapter_no`) USING BTREE,
                                  INDEX `idx_status`(`status`) USING BTREE,
                                  CONSTRAINT `fk_draft_novel` FOREIGN KEY (`novel_id`) REFERENCES `novel` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 17 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '章节草稿表（写作/修改/待审阶段）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for chapter_review
-- ----------------------------
DROP TABLE IF EXISTS `chapter_review`;
CREATE TABLE `chapter_review`  (
                                   `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '审查记录ID',
                                   `draft_id` bigint(0) NOT NULL COMMENT '被审查的草稿ID',
                                   `decision` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '审查结论（APPROVED/REJECTED）',
                                   `score_json` json NOT NULL COMMENT '审查评分（连贯性/风格一致性/剧情一致性等）',
                                   `issues_json` json NULL COMMENT '问题列表（问题类型/严重度/位置/修改建议）',
                                   `evidence_json` json NULL COMMENT '审查证据（Chroma命中片段/GraphRAG发现）',
                                   `reviewer_version` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '审查模型或规则版本标识',
                                   `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '审查时间',
                                   PRIMARY KEY (`id`) USING BTREE,
                                   INDEX `idx_draft_id`(`draft_id`) USING BTREE,
                                   CONSTRAINT `fk_review_draft` FOREIGN KEY (`draft_id`) REFERENCES `chapter_draft` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 14 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '章节审查记录表（审查历史与证据）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for novel
-- ----------------------------
DROP TABLE IF EXISTS `novel`;
CREATE TABLE `novel`  (
                          `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                          `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '小说标题',
                          `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '小说简介/总体设定',
                          `volumes` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '卷落细纲',
                          `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
                          `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
                          PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '小说主表（作品维度的元数据）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for outbox_event
-- ----------------------------
DROP TABLE IF EXISTS `outbox_event`;
CREATE TABLE `outbox_event`  (
                                 `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '事件ID',
                                 `aggregate_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '聚合根类型（如CHAPTER）',
                                 `aggregate_id` bigint(0) NOT NULL COMMENT '聚合根ID（如chapter.id）',
                                 `event_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '事件类型（如CHAPTER_APPROVED）',
                                 `payload_json` json NOT NULL COMMENT '事件负载（推送Python所需数据）',
                                 `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '事件状态（NEW/SENT/FAILED）',
                                 `retry_count` int(0) NOT NULL DEFAULT 0 COMMENT '重试次数',
                                 `next_retry_at` datetime(0) NULL DEFAULT NULL COMMENT '下次重试时间',
                                 `error_message` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '失败原因记录',
                                 `idempotency_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '幂等键（防止重复消费）',
                                 `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
                                 `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
                                 PRIMARY KEY (`id`) USING BTREE,
                                 UNIQUE INDEX `uk_idempotency`(`idempotency_key`) USING BTREE,
                                 INDEX `idx_status_retry`(`status`, `next_retry_at`) USING BTREE,
                                 INDEX `idx_aggregate`(`aggregate_type`, `aggregate_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'Outbox事件表（可靠异步通知GraphRAG）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for summary
-- ----------------------------
DROP TABLE IF EXISTS `summary`;
CREATE TABLE `summary`  (
                            `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                            `novel_id` bigint(0) NOT NULL COMMENT '所属小说ID',
                            `end_chapter_no` int(0) NOT NULL COMMENT '截止章节号',
                            `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '章节正文内容（最终发布版本）',
                            PRIMARY KEY (`id`) USING BTREE,
                            INDEX `idx_novel_id`(`novel_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '总结' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;


ALTER TABLE `xsun_novel_factory`.`summary`
    ADD COLUMN `start_chapter_no` int(0) NOT NULL COMMENT '开始章节号' AFTER `novel_id`;