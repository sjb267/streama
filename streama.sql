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

 Date: 29/05/2026 15:13:57
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
) ENGINE = InnoDB AUTO_INCREMENT = 28 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI审核任务主表' ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB AUTO_INCREMENT = 30 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI审核任务分P明细表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for category_info
-- ----------------------------
DROP TABLE IF EXISTS `category_info`;
CREATE TABLE `category_info`  (
  `category_id` int NOT NULL AUTO_INCREMENT COMMENT '自增分类ID',
  `category_code` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `category_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `p_category_id` int NOT NULL COMMENT '父级分类ID',
  `icon` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `sort` tinyint NOT NULL COMMENT '排序号',
  PRIMARY KEY (`category_id`) USING BTREE,
  UNIQUE INDEX `idx_key_category_code`(`category_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '分类信息' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for undo_log
-- ----------------------------
DROP TABLE IF EXISTS `undo_log`;
CREATE TABLE `undo_log`  (
  `branch_id` bigint NOT NULL COMMENT 'branch transaction id',
  `xid` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'global transaction id',
  `context` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'undo_log context,such as serialization',
  `rollback_info` longblob NOT NULL COMMENT 'rollback info',
  `log_status` int NOT NULL COMMENT '0:normal status,1:defense status',
  `log_created` datetime(6) NOT NULL COMMENT 'create datetime',
  `log_modified` datetime(6) NOT NULL COMMENT 'modify datetime',
  UNIQUE INDEX `ux_undo_log`(`xid` ASC, `branch_id` ASC) USING BTREE,
  INDEX `ix_log_created`(`log_created` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AT transaction mode undo table' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user_action
-- ----------------------------
DROP TABLE IF EXISTS `user_action`;
CREATE TABLE `user_action`  (
  `action_id` int NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `video_id` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '视频ID',
  `video_user_id` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '视频用户ID',
  `comment_id` int NOT NULL DEFAULT 0 COMMENT '评论ID',
  `action_type` tinyint(1) NOT NULL COMMENT '0:评论喜欢点赞 1:讨厌评论 2:视频点赞 3:视频收藏 4:视频投币',
  `action_count` int NOT NULL COMMENT '数量',
  `user_id` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户ID',
  `action_time` datetime NOT NULL COMMENT '操作时间',
  PRIMARY KEY (`action_id`) USING BTREE,
  UNIQUE INDEX `idx_key_video_comment_type_user`(`video_id` ASC, `comment_id` ASC, `action_type` ASC, `user_id` ASC) USING BTREE,
  INDEX `idx_video_id`(`video_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_type`(`action_type` ASC) USING BTREE,
  INDEX `idx_action_time`(`action_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 52 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户行为' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for user_focus
-- ----------------------------
DROP TABLE IF EXISTS `user_focus`;
CREATE TABLE `user_focus`  (
  `user_id` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户ID',
  `focus_user_id` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户ID',
  `focus_time` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`user_id`, `focus_user_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for user_info
-- ----------------------------
DROP TABLE IF EXISTS `user_info`;
CREATE TABLE `user_info`  (
  `user_id` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户id',
  `nick_name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '昵称',
  `email` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '邮箱',
  `password` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '密码',
  `sex` tinyint(1) NULL DEFAULT NULL COMMENT '0:女 1:男 2:未知',
  `birthday` date NULL DEFAULT NULL COMMENT '出生日期',
  `school` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '学校',
  `person_introduction` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '个人简介',
  `join_time` datetime NOT NULL COMMENT '加入时间',
  `last_login_time` datetime NULL DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` varchar(15) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '最后登录IP',
  `status` tinyint NOT NULL COMMENT '0:禁用 1:正常',
  `notice_info` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '通知公告',
  `total_coin_count` int NOT NULL COMMENT '硬币总数量',
  `current_coin_count` int NOT NULL COMMENT '当前硬币数',
  `avatar` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '头像',
  PRIMARY KEY (`user_id`) USING BTREE,
  UNIQUE INDEX `idx_key_email`(`email` ASC) USING BTREE,
  UNIQUE INDEX `idx_nick_name`(`nick_name` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户信息\r\n' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for video_comment
-- ----------------------------
DROP TABLE IF EXISTS `video_comment`;
CREATE TABLE `video_comment`  (
  `comment_id` int NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `p_comment_id` int NOT NULL COMMENT '父级评论ID',
  `video_id` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '视频ID',
  `video_user_id` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '视频用户ID',
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '回复内容',
  `img_path` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '图片',
  `user_id` varchar(15) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户ID',
  `reply_user_id` varchar(15) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '回复人ID',
  `top_type` tinyint NULL DEFAULT 0 COMMENT '未置顶 1:置顶',
  `post_time` datetime NOT NULL COMMENT '发布时间',
  `like_count` int NULL DEFAULT 0 COMMENT '喜欢数量',
  `hate_count` int NULL DEFAULT 0 COMMENT '讨厌数量',
  PRIMARY KEY (`comment_id`) USING BTREE,
  INDEX `idx_video_id`(`video_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_post_time`(`post_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 43 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '评论' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for video_danmu
-- ----------------------------
DROP TABLE IF EXISTS `video_danmu`;
CREATE TABLE `video_danmu`  (
  `danmu_id` int NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `video_id` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '视频ID',
  `file_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '唯一ID',
  `user_id` varchar(15) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户ID',
  `post_time` datetime NULL DEFAULT NULL COMMENT '发布时间',
  `text` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '内容',
  `mode` tinyint(1) NULL DEFAULT NULL COMMENT '展示位置',
  `color` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '颜色',
  `time` int NULL DEFAULT NULL COMMENT '展示时间',
  PRIMARY KEY (`danmu_id`) USING BTREE,
  INDEX `idx_file_id`(`file_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 15 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '视频弹幕' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for video_info
-- ----------------------------
DROP TABLE IF EXISTS `video_info`;
CREATE TABLE `video_info`  (
  `video_id` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `video_cover` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `video_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `user_id` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `last_update_time` datetime NOT NULL COMMENT '最后更新时间',
  `p_category_id` int NOT NULL COMMENT '父级分类ID',
  `category_id` int NULL DEFAULT NULL COMMENT '分类ID',
  `post_type` tinyint NOT NULL COMMENT '0:自制作 1:转载',
  `origin_info` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `tags` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `introduction` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `interaction` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `duration` int NULL DEFAULT 0 COMMENT '持续时间（秒）',
  `play_count` int NULL DEFAULT 0 COMMENT '播放数量',
  `like_count` int NULL DEFAULT 0 COMMENT '点赞数量',
  `danmu_count` int NULL DEFAULT 0 COMMENT '弹幕数量',
  `comment_count` int NULL DEFAULT 0 COMMENT '评论数量',
  `coin_count` int NULL DEFAULT 0 COMMENT '投币数量',
  `collect_count` int NULL DEFAULT 0 COMMENT '收藏数量',
  `recommend_type` tinyint(1) NULL DEFAULT 0 COMMENT '是否推荐 0:未推荐 1:已推荐',
  `last_play_time` datetime NULL DEFAULT NULL COMMENT '最后播放时间',
  PRIMARY KEY (`video_id`) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_category_id`(`category_id` ASC) USING BTREE,
  INDEX `idx_pcategory_id`(`p_category_id` ASC) USING BTREE,
  INDEX `idx_recommend_type`(`recommend_type` ASC) USING BTREE,
  INDEX `idx_last_update_time`(`last_play_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '视频信息' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for video_info_file
-- ----------------------------
DROP TABLE IF EXISTS `video_info_file`;
CREATE TABLE `video_info_file`  (
  `file_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `user_id` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `video_id` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `file_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `file_index` int NOT NULL COMMENT '文件索引',
  `file_size` bigint NULL DEFAULT NULL COMMENT '文件大小',
  `file_path` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `duration` int NULL DEFAULT NULL COMMENT '持续时间（秒）',
  PRIMARY KEY (`file_id`) USING BTREE,
  INDEX `idx_video_id`(`video_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '视频文件信息' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for video_info_file_post
-- ----------------------------
DROP TABLE IF EXISTS `video_info_file_post`;
CREATE TABLE `video_info_file_post`  (
  `file_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `upload_id` varchar(15) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `user_id` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `video_id` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `file_index` int NOT NULL COMMENT '文件索引',
  `file_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `file_size` bigint NULL DEFAULT NULL COMMENT '文件大小',
  `file_path` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `update_type` tinyint NULL DEFAULT NULL COMMENT '0:无更新 1:有更新',
  `transfer_result` tinyint NULL DEFAULT NULL COMMENT '0:转码中 1:转码成功 2:转码失败',
  `duration` int NULL DEFAULT NULL COMMENT '持续时间（秒）',
  PRIMARY KEY (`file_id`) USING BTREE,
  UNIQUE INDEX `idx_key_upload_id`(`upload_id` ASC, `user_id` ASC) USING BTREE,
  INDEX `idx_video_id`(`video_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '视频文件信息' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for video_info_post
-- ----------------------------
DROP TABLE IF EXISTS `video_info_post`;
CREATE TABLE `video_info_post`  (
  `video_id` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `video_cover` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '封面路径',
  `video_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `user_id` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `last_update_time` datetime NOT NULL COMMENT '最后更新时间',
  `p_category_id` int NOT NULL COMMENT '父级分类ID',
  `category_id` int NULL DEFAULT NULL COMMENT '分类ID',
  `status` tinyint(1) NOT NULL COMMENT '0:转码中 1:转码失败 2:待审核 3:审核成功 4:审核失败',
  `post_type` tinyint NOT NULL COMMENT '0:自制作 1:转载',
  `origin_info` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `tags` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `introduction` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `interaction` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `duration` int NULL DEFAULT NULL COMMENT '持续时间（秒）',
  PRIMARY KEY (`video_id`) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_category_id`(`category_id` ASC) USING BTREE,
  INDEX `idx_pcategory_id`(`p_category_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '视频信息' ROW_FORMAT = DYNAMIC;

SET FOREIGN_KEY_CHECKS = 1;
