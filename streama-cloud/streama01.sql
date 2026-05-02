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

 Date: 18/04/2026 15:47:57
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

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
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '分类信息' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of category_info
-- ----------------------------
INSERT INTO `category_info` VALUES (3, 'code', '编程', 0, '', 3);
INSERT INTO `category_info` VALUES (4, 'game', '游戏', 0, 'cover/202603/h0rrr5PFqHdhwh2Hq0XMTzYHl9iT5M.png', 2);
INSERT INTO `category_info` VALUES (6, 'codelanguage', 'java', 3, '', 1);
INSERT INTO `category_info` VALUES (7, 'song', 'song', 0, '', 5);
INSERT INTO `category_info` VALUES (8, 'anime', '动漫', 0, 'cover/202603/0PRmc2ITc6RcFAlcQx7NYTae7WFZlf.png', 6);
INSERT INTO `category_info` VALUES (9, 'live', '生活', 0, 'cover/202603/wJZLGfL2Ie2Pnn48bzv3Hczn6JAcf0.png', 7);
INSERT INTO `category_info` VALUES (11, 'mi', '原神', 4, 'cover/202603/LDOW5yofmeRHTX4xWk9HNaSinOSJhx.png', 1);

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
-- Records of undo_log
-- ----------------------------

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
) ENGINE = InnoDB AUTO_INCREMENT = 40 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户行为' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of user_action
-- ----------------------------
INSERT INTO `user_action` VALUES (21, 'uVCqA6teRT', 'uVCqA6teRT', 0, 2, 1, '9874085469', '2026-03-22 00:49:00');
INSERT INTO `user_action` VALUES (22, 'uVCqA6teRT', 'uVCqA6teRT', 0, 2, 1, '9976639498', '2026-03-22 00:53:32');
INSERT INTO `user_action` VALUES (23, 'uVCqA6teRT', 'uVCqA6teRT', 0, 3, 1, '9976639498', '2026-03-22 00:53:35');
INSERT INTO `user_action` VALUES (24, 'uVCqA6teRT', 'uVCqA6teRT', 0, 3, 1, '9874085469', '2026-03-22 01:59:34');
INSERT INTO `user_action` VALUES (25, 'uVCqA6teRT', 'uVCqA6teRT', 0, 4, 1, '9976639498', '2026-03-22 02:01:50');
INSERT INTO `user_action` VALUES (26, 'BvvusWNd8J', 'BvvusWNd8J', 25, 0, 1, '9874085469', '2026-03-24 01:08:37');
INSERT INTO `user_action` VALUES (28, 'BvvusWNd8J', 'BvvusWNd8J', 24, 1, 1, '9874085469', '2026-03-24 01:08:50');
INSERT INTO `user_action` VALUES (31, 'BvvusWNd8J', 'BvvusWNd8J', 0, 3, 1, '9976639498', '2026-03-26 12:08:54');
INSERT INTO `user_action` VALUES (32, 'o8imKGG90K', 'o8imKGG90K', 0, 3, 1, '9976639498', '2026-03-26 12:09:09');
INSERT INTO `user_action` VALUES (33, 'NhXZfG8EGJ', 'NhXZfG8EGJ', 0, 3, 1, '9976639498', '2026-03-28 23:12:54');
INSERT INTO `user_action` VALUES (34, 'NhXZfG8EGJ', 'NhXZfG8EGJ', 28, 0, 1, '9874085469', '2026-03-30 19:58:06');
INSERT INTO `user_action` VALUES (35, 'I8nADnxswI', 'I8nADnxswI', 0, 2, 1, '9976639498', '2026-04-02 20:01:02');
INSERT INTO `user_action` VALUES (36, 'I8nADnxswI', 'I8nADnxswI', 0, 3, 1, '9976639498', '2026-04-02 20:01:02');
INSERT INTO `user_action` VALUES (37, 'vuVLXyezPv', 'vuVLXyezPv', 0, 3, 1, '9874085469', '2026-04-04 12:30:45');

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
-- Records of user_focus
-- ----------------------------
INSERT INTO `user_focus` VALUES ('9874085469', '9976639498', '2026-03-30 21:07:14');
INSERT INTO `user_focus` VALUES ('9976639498', '9874085469', '2026-03-22 01:57:45');

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
-- Records of user_info
-- ----------------------------
INSERT INTO `user_info` VALUES ('9874085469', 'biean', 'jiangbing267@gmail.com', '078577e334574e6952b333ec2f73a255', 1, '2003-07-26', 'asas', '啊啊', '2026-01-29 17:38:02', '2026-04-18 15:28:52', '192.168.1.4', 1, '关注我', 11, 11, 'cover/20260401/bNdzu3itdE9odByaluPOVPCd31xdbP.png');
INSERT INTO `user_info` VALUES ('9976639498', 'sjb', '1678530981@qq.com', '216cb981a053bc960922dd55da684230', 2, NULL, NULL, NULL, '2026-03-22 00:53:10', '2026-04-02 01:31:28', '127.0.0.1', 1, NULL, 10, 9, NULL);

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
) ENGINE = InnoDB AUTO_INCREMENT = 37 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '评论' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of video_comment
-- ----------------------------
INSERT INTO `video_comment` VALUES (22, 0, 'BvvusWNd8J', '9874085469', '你就是我的回忆', NULL, '9976639498', NULL, 0, '2026-03-23 23:05:38', 0, 0);
INSERT INTO `video_comment` VALUES (24, 22, 'BvvusWNd8J', '9874085469', 'hahaha', NULL, '9976639498', '9976639498', 0, '2026-03-23 23:13:50', 0, 1);
INSERT INTO `video_comment` VALUES (25, 0, 'BvvusWNd8J', '9874085469', '你就说牛不牛a', NULL, '9874085469', NULL, 0, '2026-03-23 23:15:23', 1, 0);
INSERT INTO `video_comment` VALUES (26, 0, 'BvvusWNd8J', '9874085469', '可笑', NULL, '9874085469', NULL, 1, '2026-03-24 01:09:09', 0, 0);
INSERT INTO `video_comment` VALUES (27, 0, 'BvvusWNd8J', '9874085469', '恭喜发财啊', 'cover/20260324/9YhDkVzQGna0G110nxcJrZAyVpy0SU.png', '9874085469', NULL, 0, '2026-03-24 01:12:02', 0, 0);
INSERT INTO `video_comment` VALUES (28, 0, 'NhXZfG8EGJ', '9976639498', '真的好看，短却精髓', NULL, '9874085469', NULL, 0, '2026-03-30 19:58:01', 1, 0);
INSERT INTO `video_comment` VALUES (29, 0, 'vuVLXyezPv', '9976639498', '啊啊啊啊', NULL, '9874085469', NULL, 0, '2026-04-17 21:41:01', 0, 0);

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
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '视频弹幕' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of video_danmu
-- ----------------------------
INSERT INTO `video_danmu` VALUES (1, '8HJhBHlP6A', 'wGi66D9Wg3kQBbWmQO7M', '9874085469', '2026-03-24 22:34:20', 'adsas', 1, '#FFFFFF', 8);
INSERT INTO `video_danmu` VALUES (2, '8HJhBHlP6A', 'wGi66D9Wg3kQBbWmQO7M', '9874085469', '2026-03-24 22:35:32', 'dasdsadads', 1, '#FFFFFF', 63);
INSERT INTO `video_danmu` VALUES (4, '8HJhBHlP6A', 'wGi66D9Wg3kQBbWmQO7M', '9874085469', '2026-03-25 00:17:27', '太帅了吧', 1, '#CC0273', 34);
INSERT INTO `video_danmu` VALUES (5, 'I8nADnxswI', '0dKkwDn1txE1GYiCdg6Q', '9976639498', '2026-04-02 20:01:17', 'hahaha', 1, '#FFFFFF', 3);
INSERT INTO `video_danmu` VALUES (6, 'vuVLXyezPv', 'EH8J8XibY4KQJtkQ2wc2', '9874085469', '2026-04-10 17:24:45', 'dsadadsasdads', 1, '#FFFFFF', 24);

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
-- Records of video_info
-- ----------------------------
INSERT INTO `video_info` VALUES ('1vUhL5OMi4', 'cover/20260310/NR2ktoH56mZ00ejFGdoOw9iNFB4ItE.jpg', 'lol', '9874085469', '2026-03-10 20:37:06', '2026-03-10 20:37:06', 0, 4, 0, NULL, '游戏', '终末地', '', 363, 2, 0, 0, 0, 0, 0, 0, '2026-04-02 20:57:50');
INSERT INTO `video_info` VALUES ('8HJhBHlP6A', 'cover/20260324/CuEMjCU698EWCaEUSzqKUAAOgBTueH.png', '1618703307-1-192', '9874085469', '2026-03-24 15:51:13', '2026-03-24 15:51:13', 4, NULL, 0, NULL, '歌曲', 'sdsds', '0', 649, 21, 0, 4, 0, 0, 0, 0, '2026-04-17 21:47:15');
INSERT INTO `video_info` VALUES ('BvvusWNd8J', 'cover/20260309/Wdiv8GHZEDQtD1l4Gmv78YwriXbz5t.jpg', '下载2', '9874085469', '2026-03-09 22:27:46', '2026-03-09 22:27:46', 0, 4, 0, NULL, '游戏', 'aaaa', '', 286, 3, 0, 0, 4, 0, 1, 0, '2026-04-03 13:20:19');
INSERT INTO `video_info` VALUES ('l4BxkDYpn0', 'cover/20260309/eyxo9NIPtNPvIP7d66HyDFwWWsom6P.jpg', '刘德华-练习', '9874085469', '2026-03-09 13:36:19', '2026-03-09 13:36:19', 0, 7, 0, NULL, '刘德华', '歌曲', '', 286, 1, 0, 0, 0, 0, 0, 0, '2026-04-02 20:57:54');
INSERT INTO `video_info` VALUES ('qNz6BVuDTC', 'cover/20260308/35CWGhAhxOvrLFWtO7F6qr23iyzLVY.jpg', '下载', '9874085469', '2026-03-08 23:12:51', '2026-03-08 23:12:51', 0, 4, 0, NULL, 'asda', 'asdasd', '', 363, 1, 0, 0, 0, 0, 0, 0, '2026-04-02 20:57:57');
INSERT INTO `video_info` VALUES ('uVCqA6teRT', 'cover/20260321/pU4ve8W9jW3KkZXbF2UDBl8lQ0jiFb.png', '测试数据1', '9874085469', '2026-03-21 13:13:23', '2026-03-21 13:13:23', 4, NULL, 0, NULL, '终末地', '测试数据1', NULL, 483, 4, 2, 0, 0, 1, 2, 0, '2026-04-05 13:25:53');
INSERT INTO `video_info` VALUES ('vuVLXyezPv', 'cover/20260402/t8EMKs40iFXb5hbg6kay0dX0ITpFzD.jpg', '低头人生', '9976639498', '2026-04-02 20:26:48', '2026-04-02 20:29:42', 4, NULL, 0, NULL, '人生,生活', '人生', NULL, 454, 95, 0, 1, 1, 0, 1, 0, '2026-04-18 15:34:48');

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
-- Records of video_info_file
-- ----------------------------
INSERT INTO `video_info_file` VALUES ('0dKkwDn1txE1GYiCdg6Q', '9976639498', 'I8nADnxswI', '低头人生.mp4', 1, NULL, NULL, NULL);
INSERT INTO `video_info_file` VALUES ('3x8ujzTZgswQz00Z4yYD', '9874085469', 'l4BxkDYpn0', '1618703307-1-192.mp4', 1, 48061573, 'video/20260309/9874085469JMS1h63zGbPVhw0', 286);
INSERT INTO `video_info_file` VALUES ('EH8J8XibY4KQJtkQ2wc2', '9976639498', 'vuVLXyezPv', '1618703307-1-192.mp4', 1, 48061573, 'video/20260402/9976639498Hpqj1aVmTXoBkV5', 286);
INSERT INTO `video_info_file` VALUES ('ElvGseMcM99fhPjD1cyZ', '9976639498', 'vuVLXyezPv', '低头人生.mp4', 2, 61564716, 'video/20260402/9976639498D7OPQkfYqIsrnrQ', 168);
INSERT INTO `video_info_file` VALUES ('GfXQIPBhffeh1imi8k14', '9874085469', 'BvvusWNd8J', '1618703307-1-192.mp4', 1, 48061573, 'video/20260309/9874085469CY0LFp0D4idh0Vx', 286);
INSERT INTO `video_info_file` VALUES ('gXd0cnK0w5BBHaTYImoN', '9874085469', '1vUhL5OMi4', 'a356e90f-e6d9-480c-a302-f0d778f6f62a_北京市奖学金申请演讲.mp4', 1, 100259955, 'video/20260310/9874085469HGBLXptIWWLE6kg', 363);
INSERT INTO `video_info_file` VALUES ('oUHlOnAAGrNADhq7erby', '9874085469', 'uVCqA6teRT', '打瓦.mp4', 2, 24101428, 'video/20260321/98740854694XE9BgPlq0YX5Vj', 120);
INSERT INTO `video_info_file` VALUES ('sjo1rSSvrwW32lPvnery', '9874085469', '8HJhBHlP6A', 'a356e90f-e6d9-480c-a302-f0d778f6f62a_北京市奖学金申请演讲.mp4', 2, 100259955, 'video/20260324/9874085469uJrTelqnC4nRWO5', 363);
INSERT INTO `video_info_file` VALUES ('WFQh0PjrDJXZOfd7QtOt', '9874085469', 'uVCqA6teRT', 'a356e90f-e6d9-480c-a302-f0d778f6f62a_北京市奖学金申请演讲.mp4', 1, 100259955, 'video/20260321/9874085469aewKDoWbETMavQL', 363);
INSERT INTO `video_info_file` VALUES ('wGi66D9Wg3kQBbWmQO7M', '9874085469', '8HJhBHlP6A', '1618703307-1-192.mp4', 1, 48061573, 'video/20260324/9874085469KUUemGxL8uxe1jA', 286);
INSERT INTO `video_info_file` VALUES ('wPkB2UEpXtQa4ZFPx4wL', '9976639498', 'NhXZfG8EGJ', '442774934-1-208.mp4', 1, NULL, NULL, NULL);
INSERT INTO `video_info_file` VALUES ('xZpq3I3TGisSABufSEJ5', '9976639498', 'NhXZfG8EGJ', '打瓦.mp4', 2, NULL, NULL, NULL);
INSERT INTO `video_info_file` VALUES ('zFtUF4XGhiSEPVPFH4ls', '9874085469', 'qNz6BVuDTC', 'a356e90f-e6d9-480c-a302-f0d778f6f62a_北京市奖学金申请演讲.mp4', 1, 100259955, 'video/20260308/9874085469pdIhV9bTwjnjbgV', 363);

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
-- Records of video_info_file_post
-- ----------------------------
INSERT INTO `video_info_file_post` VALUES ('0dKkwDn1txE1GYiCdg6Q', 'TzqHZD6z6YMHNIt', '9976639498', 'I8nADnxswI', 1, '低头人生.mp4', NULL, NULL, 0, NULL, NULL);
INSERT INTO `video_info_file_post` VALUES ('3x8ujzTZgswQz00Z4yYD', 'JMS1h63zGbPVhw0', '9874085469', 'l4BxkDYpn0', 1, '1618703307-1-192.mp4', 48061573, 'video/20260309/9874085469JMS1h63zGbPVhw0', 0, 1, 286);
INSERT INTO `video_info_file_post` VALUES ('EH8J8XibY4KQJtkQ2wc2', 'Hpqj1aVmTXoBkV5', '9976639498', 'vuVLXyezPv', 1, '1618703307-1-192.mp4', 48061573, 'video/20260402/9976639498Hpqj1aVmTXoBkV5', 0, 1, 286);
INSERT INTO `video_info_file_post` VALUES ('ElvGseMcM99fhPjD1cyZ', 'D7OPQkfYqIsrnrQ', '9976639498', 'vuVLXyezPv', 2, '低头人生.mp4', 61564716, 'video/20260402/9976639498D7OPQkfYqIsrnrQ', 0, 1, 168);
INSERT INTO `video_info_file_post` VALUES ('GfXQIPBhffeh1imi8k14', 'CY0LFp0D4idh0Vx', '9874085469', 'BvvusWNd8J', 1, '1618703307-1-192.mp4', 48061573, 'video/20260309/9874085469CY0LFp0D4idh0Vx', 0, 1, 286);
INSERT INTO `video_info_file_post` VALUES ('gXd0cnK0w5BBHaTYImoN', 'HGBLXptIWWLE6kg', '9874085469', '1vUhL5OMi4', 1, 'a356e90f-e6d9-480c-a302-f0d778f6f62a_北京市奖学金申请演讲.mp4', 100259955, 'video/20260310/9874085469HGBLXptIWWLE6kg', 0, 1, 363);
INSERT INTO `video_info_file_post` VALUES ('oUHlOnAAGrNADhq7erby', '4XE9BgPlq0YX5Vj', '9874085469', 'uVCqA6teRT', 2, '打瓦.mp4', 24101428, 'video/20260321/98740854694XE9BgPlq0YX5Vj', 0, 1, 120);
INSERT INTO `video_info_file_post` VALUES ('sjo1rSSvrwW32lPvnery', 'uJrTelqnC4nRWO5', '9874085469', '8HJhBHlP6A', 2, 'a356e90f-e6d9-480c-a302-f0d778f6f62a_北京市奖学金申请演讲.mp4', 100259955, 'video/20260324/9874085469uJrTelqnC4nRWO5', 0, 1, 363);
INSERT INTO `video_info_file_post` VALUES ('WFQh0PjrDJXZOfd7QtOt', 'aewKDoWbETMavQL', '9874085469', 'uVCqA6teRT', 1, 'a356e90f-e6d9-480c-a302-f0d778f6f62a_北京市奖学金申请演讲.mp4', 100259955, 'video/20260321/9874085469aewKDoWbETMavQL', 0, 1, 363);
INSERT INTO `video_info_file_post` VALUES ('wGi66D9Wg3kQBbWmQO7M', 'KUUemGxL8uxe1jA', '9874085469', '8HJhBHlP6A', 1, '1618703307-1-192.mp4', 48061573, 'video/20260324/9874085469KUUemGxL8uxe1jA', 0, 1, 286);
INSERT INTO `video_info_file_post` VALUES ('wPkB2UEpXtQa4ZFPx4wL', 'U2wMtAXK6NvKScL', '9976639498', 'NhXZfG8EGJ', 1, '442774934-1-208.mp4', NULL, NULL, 0, NULL, NULL);
INSERT INTO `video_info_file_post` VALUES ('xZpq3I3TGisSABufSEJ5', 'zXo31oPrgiBIk1J', '9976639498', 'NhXZfG8EGJ', 2, '打瓦.mp4', NULL, NULL, 0, NULL, NULL);
INSERT INTO `video_info_file_post` VALUES ('zFtUF4XGhiSEPVPFH4ls', 'pdIhV9bTwjnjbgV', '9874085469', 'qNz6BVuDTC', 1, 'a356e90f-e6d9-480c-a302-f0d778f6f62a_北京市奖学金申请演讲.mp4', 100259955, 'video/20260308/9874085469pdIhV9bTwjnjbgV', 0, 1, 363);

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

-- ----------------------------
-- Records of video_info_post
-- ----------------------------
INSERT INTO `video_info_post` VALUES ('1vUhL5OMi4', 'cover/20260310/NR2ktoH56mZ00ejFGdoOw9iNFB4ItE.jpg', 'lol', '9874085469', '2026-03-10 20:37:06', '2026-03-10 20:37:06', 0, 4, 3, 0, NULL, '游戏', '终末地', '', 363);
INSERT INTO `video_info_post` VALUES ('8HJhBHlP6A', 'cover/20260324/CuEMjCU698EWCaEUSzqKUAAOgBTueH.png', '1618703307-1-192', '9874085469', '2026-03-24 15:51:13', '2026-03-24 15:51:13', 4, NULL, 3, 0, NULL, '歌曲', 'sdsds', '0', 649);
INSERT INTO `video_info_post` VALUES ('BvvusWNd8J', 'cover/20260309/Wdiv8GHZEDQtD1l4Gmv78YwriXbz5t.jpg', '下载2', '9874085469', '2026-03-09 22:27:46', '2026-03-09 22:27:46', 0, 4, 3, 0, NULL, '游戏', 'aaaa', '', 286);
INSERT INTO `video_info_post` VALUES ('l4BxkDYpn0', 'cover/20260309/eyxo9NIPtNPvIP7d66HyDFwWWsom6P.jpg', '刘德华-练习', '9874085469', '2026-03-09 13:36:19', '2026-03-09 13:36:19', 0, 7, 3, 0, NULL, '刘德华', '歌曲', '', 286);
INSERT INTO `video_info_post` VALUES ('qNz6BVuDTC', 'cover/20260308/35CWGhAhxOvrLFWtO7F6qr23iyzLVY.jpg', '下载', '9874085469', '2026-03-08 23:12:51', '2026-03-08 23:12:51', 0, 4, 3, 0, NULL, 'asda', 'asdasd', '', 363);
INSERT INTO `video_info_post` VALUES ('uVCqA6teRT', 'cover/20260321/pU4ve8W9jW3KkZXbF2UDBl8lQ0jiFb.png', '测试数据1', '9874085469', '2026-03-21 13:13:23', '2026-03-21 13:13:23', 4, NULL, 3, 0, NULL, '终末地', '测试数据1', NULL, 483);
INSERT INTO `video_info_post` VALUES ('vuVLXyezPv', 'cover/20260402/t8EMKs40iFXb5hbg6kay0dX0ITpFzD.jpg', '低头人生', '9976639498', '2026-04-02 20:26:48', '2026-04-02 20:29:42', 4, NULL, 3, 0, NULL, '人生,生活', '人生', NULL, 454);

SET FOREIGN_KEY_CHECKS = 1;
