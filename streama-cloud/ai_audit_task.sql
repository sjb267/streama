/*
 Navicat Premium Dump SQL

 Source Server         : streama
 Source Server Type    : MySQL
 Source Server Version : 80044 (8.0.44)
 Source Host           : localhost:3306
 Source Schema         : streama

 Target Server Type    : MySQL
 Target Server Version : 80044 (8.0.44)
 File Encoding         : 65001

 Date: 19/04/2026 20:10:48
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ai_audit_task
-- ----------------------------
DROP TABLE IF EXISTS `ai_audit_task`;
CREATE TABLE `ai_audit_task`  (
  `task_id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增任务ID',
  `request_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '幂等请求ID',
  `video_id` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '视频ID',
  `audit_version` int NOT NULL DEFAULT 1 COMMENT '审核版本(同video递增)',
  `source_type` tinyint(1) NOT NULL COMMENT '来源类型 1:新增投稿 2:编辑重审',
  `task_status` tinyint(1) NOT NULL DEFAULT 0 COMMENT '任务状态 0:待处理 1:处理中 2:完成 3:失败 4:取消',
  `ai_decision` tinyint(1) NULL DEFAULT NULL COMMENT 'AI建议 1:通过 2:驳回 3:人工复核',
  `ai_risk_level` tinyint(1) NULL DEFAULT NULL COMMENT '风险等级 1:低 2:中 3:高',
  `ai_summary` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '视频级审核摘要',
  `model_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '模型名称',
  `model_version` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '模型版本',
  `trigger_time` datetime NOT NULL COMMENT '触发时间',
  `completed_at` datetime NULL DEFAULT NULL COMMENT '完成时间',
  `retry_count` int NOT NULL DEFAULT 0 COMMENT '重试次数',
  `last_error` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '最后错误信息',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  `updated_at` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`task_id`) USING BTREE,
  UNIQUE INDEX `idx_key_request_id`(`request_id` ASC) USING BTREE,
  UNIQUE INDEX `idx_key_video_audit_version`(`video_id` ASC, `audit_version` ASC) USING BTREE,
  INDEX `idx_video_status`(`video_id` ASC, `task_status` ASC) USING BTREE,
  INDEX `idx_trigger_time`(`trigger_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI审核任务主表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for ai_audit_task_item
-- ----------------------------
DROP TABLE IF EXISTS `ai_audit_task_item`;
CREATE TABLE `ai_audit_task_item`  (
  `item_id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增明细ID',
  `task_id` bigint NOT NULL COMMENT '任务ID',
  `video_id` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '视频ID',
  `file_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '分P文件ID',
  `file_index` int NOT NULL COMMENT '分P序号',
  `upload_id` varchar(15) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '上传ID',
  `file_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '文件名',
  `file_path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '文件路径',
  `duration` int NULL DEFAULT NULL COMMENT '分P时长(秒)',
  `update_type` tinyint(1) NOT NULL DEFAULT 1 COMMENT '更新类型 0:无更新 1:有更新',
  `item_status` tinyint(1) NOT NULL DEFAULT 0 COMMENT '明细状态 0:待处理 1:处理中 2:完成 3:失败',
  `item_decision` tinyint(1) NULL DEFAULT NULL COMMENT 'AI建议 1:通过 2:驳回 3:人工复核',
  `risk_score` decimal(5, 2) NULL DEFAULT NULL COMMENT '风险分',
  `risk_tags_json` json NULL COMMENT '命中标签(JSON)',
  `hit_segments_json` json NULL COMMENT '命中片段(JSON)',
  `item_reason` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '分P审核说明',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  `updated_at` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`item_id`) USING BTREE,
  UNIQUE INDEX `idx_key_task_file`(`task_id` ASC, `file_id` ASC) USING BTREE,
  INDEX `idx_task_id`(`task_id` ASC) USING BTREE,
  INDEX `idx_video_id`(`video_id` ASC) USING BTREE,
  INDEX `idx_video_file_index`(`video_id` ASC, `file_index` ASC) USING BTREE,
  INDEX `idx_item_decision`(`item_decision` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI审核任务分P明细表' ROW_FORMAT = DYNAMIC;

SET FOREIGN_KEY_CHECKS = 1;
