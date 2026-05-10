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

 Date: 10/05/2026 19:12:51
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
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI审核任务主表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_audit_task
-- ----------------------------
INSERT INTO `ai_audit_task` VALUES (1, '4d90cac921f542aeb89133d9d6a6c65f', 'vt0Wrs8W75', 1, 1, 3, 3, 2, 'videoDecision=manual_review; FJj6qpeACU0tj9Ymp2Db: AI processing failed, fallback to manual review: HTTPConnectionPool(host=\'127.0.0.1\', port=7072): Max retries exceeded with url: /innerApi/video/aiAuditItemProgress (Caused by NewConnectionError(\"HTTPConnection(host=\'127.0.0.1\', port=7072): Failed to establish a new connection: [WinError 10061] 由于目标计算机积极拒绝，无法连接。\"))', 'qwen-video-moderation', '1.0.0', '2026-04-20 14:28:52', '2026-04-21 04:15:40', 0, 'AI processing failed, fallback to manual review: HTTPConnectionPool(host=\'127.0.0.1\', port=7072): Max retries exceeded with url: /innerApi/video/aiAuditItemProgress (Caused by NewConnectionError(\"HTTPConnection(host=\'127.0.0.1\', port=7072): Failed to establish a new connection: [WinError 10061] 由于目标计算机积极拒绝，无法连接。\"))', '2026-04-20 14:28:52', '2026-04-21 12:31:17');
INSERT INTO `ai_audit_task` VALUES (4, 'ef2af863cce54ac89e33b85597901391', 'kLfqKWuR07', 1, 1, 1, NULL, NULL, NULL, NULL, NULL, '2026-04-21 13:37:26', NULL, 0, NULL, '2026-04-21 13:37:26', '2026-04-21 13:37:26');
INSERT INTO `ai_audit_task` VALUES (5, '922bde157e414e51a6eb26c103acc919', 'XSSgmZJFWr', 1, 1, 2, 1, 1, 'videoDecision=pass; w7oDso9Aa6ombbXmljHK: 低风险 - 建议通过; no risky segments detected.', 'qwen-video-moderation', '1.0.0', '2026-04-21 13:41:25', '2026-04-21 06:39:42', 0, NULL, '2026-04-21 13:41:25', '2026-04-21 14:39:42');
INSERT INTO `ai_audit_task` VALUES (6, '703b542e8cd348a18e6e43bc7e0236c2', 'qQDkiVveIa', 1, 1, 2, 1, 1, 'videoDecision=pass; j8NTwZr3id7ADnjDkkdk: 低风险 - 建议通过; no risky segments detected. | nDggQuSRpx7o1ltJGSom: 低风险 - 建议通过; no risky segments detected.', 'qwen-video-moderation', '1.0.0', '2026-04-21 19:40:24', '2026-04-21 11:49:05', 0, NULL, '2026-04-21 19:40:24', '2026-04-21 19:49:05');
INSERT INTO `ai_audit_task` VALUES (7, 'eb0a1240e44442b2919a3b96c407f327', 'Yr5XeOni5l', 1, 1, 2, 2, 3, 'videoDecision=reject; y9SYepiMqsdfHHMhEV21: 高风险 - 建议拦截; riskySegments=2; topRisk=暴力@25.98-37.02s', 'qwen-video-moderation', '1.0.0', '2026-04-21 19:56:41', '2026-04-21 12:00:43', 0, NULL, '2026-04-21 19:56:41', '2026-04-21 20:00:43');
INSERT INTO `ai_audit_task` VALUES (8, '933a8eea78524774b5adb9a72ca6b458', '8B64N7Jkju', 1, 1, 2, 2, 3, 'videoDecision=reject; XVtAtke4RtA1QoyV3ap4: 高风险 - 建议拦截; riskySegments=9; topRisk=暴力@0.0-6.84s; modalities=audio,audio_event,metadata_text,speech_text,vad,visual; coverage=complete', 'qwen-video-moderation', '1.0.0', '2026-04-29 18:36:10', '2026-04-29 10:52:33', 0, NULL, '2026-04-29 18:36:10', '2026-04-29 18:52:33');
INSERT INTO `ai_audit_task` VALUES (9, '696a7d9e8b8840d1aee890fdefe406d1', 'a49SrbrIlN', 1, 1, 2, 2, 3, 'videoDecision=reject; kxqc3Ed7YfiyOoanqrry: 高风险 - 建议拦截; riskySegments=1; topRisk=violence@0.0-6.84s; modalities=audio,audio_event,metadata_text,speech_text,vad,visual; coverage=complete', 'qwen-video-moderation', '1.0.0', '2026-04-29 19:18:20', '2026-04-29 11:23:53', 0, NULL, '2026-04-29 19:18:20', '2026-04-29 19:23:53');
INSERT INTO `ai_audit_task` VALUES (10, '917606ddfbe6448c88bc4d65822ea782', 'lc6beANXzy', 1, 1, 2, 2, 3, 'videoDecision=reject; UQbHGIRr2ndprPRBZSAV: 高风险 - 建议拦截; riskySegments=2; topRisk=violence@0.0-6.84s; modalities=audio,audio_event,metadata_text,speech_text,vad,visual; coverage=complete', 'qwen-video-moderation', '1.0.0', '2026-04-29 23:14:28', '2026-04-29 15:20:39', 0, NULL, '2026-04-29 23:14:28', '2026-04-29 23:20:39');
INSERT INTO `ai_audit_task` VALUES (11, '047b95bcf013436a83752b7d28a29c02', 'taQehKU5du', 1, 1, 2, 3, 2, 'videoDecision=manual_review; wYn15OnTnkyB7z6wjDS7: 中风险 - 建议人工复核; riskySegments=1; topRisk=audio_event@66.84-96.84s; modalities=audio,audio_event,metadata_text,speech_text,vad,visual; coverage=complete', 'qwen-video-moderation', '1.0.0', '2026-04-30 00:27:55', '2026-04-29 17:41:25', 0, NULL, '2026-04-30 00:27:55', '2026-04-30 01:41:25');

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
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI审核任务分P明细表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_audit_task_item
-- ----------------------------
INSERT INTO `ai_audit_task_item` VALUES (1, 1, 'vt0Wrs8W75', 'FJj6qpeACU0tj9Ymp2Db', 1, 's2lnd2p7cFwtetH', 'video_【动物丰容】5分钟快速理解！_0.mp4', 'video/20260420/9976639498s2lnd2p7cFwtetH', 381, 1, 3, 3, 0.50, '[\"processing_error\"]', '[]', 'AI processing failed, fallback to manual review: HTTPConnectionPool(host=\'127.0.0.1\', port=7072): Max retries exceeded with url: /innerApi/video/aiAuditItemProgress (Caused by NewConnectionError(\"HTTPConnection(host=\'127.0.0.1\', port=7072): Failed to establish a new connection: [WinError 10061] 由于目标计算机积极拒绝，无法连接。\"))', '2026-04-20 14:28:52', '2026-04-21 12:31:17');
INSERT INTO `ai_audit_task_item` VALUES (2, 3, 'MJOk6Hkqcy', 'FOPGnd8eBnDXOb2r7yq3', 1, 'Zbm64dOk7LqrE8g', 'video_【动物丰容】5分钟快速理解！_0.mp4', 'video/20260421/9976639498Zbm64dOk7LqrE8g', 381, 1, 0, NULL, NULL, NULL, NULL, NULL, '2026-04-21 12:44:30', '2026-04-21 12:44:30');
INSERT INTO `ai_audit_task_item` VALUES (4, 5, 'XSSgmZJFWr', 'w7oDso9Aa6ombbXmljHK', 1, 'tWnapgHvWydWcRO', 'video_【动物丰容】5分钟快速理解！_0.mp4', 'video/20260421/9976639498tWnapgHvWydWcRO', 381, 1, 2, 1, 0.10, NULL, NULL, '低风险 - 建议通过; no risky segments detected.', '2026-04-21 13:41:25', '2026-04-21 14:39:42');
INSERT INTO `ai_audit_task_item` VALUES (5, 6, 'qQDkiVveIa', 'j8NTwZr3id7ADnjDkkdk', 1, '49ejjnhaTJXAs3M', 'video_【动物丰容】5分钟快速理解！_0.mp4', 'video/20260421/997663949849ejjnhaTJXAs3M', 381, 1, 2, 1, 0.10, NULL, NULL, '低风险 - 建议通过; no risky segments detected.', '2026-04-21 19:40:24', '2026-04-21 19:49:05');
INSERT INTO `ai_audit_task_item` VALUES (6, 6, 'qQDkiVveIa', 'nDggQuSRpx7o1ltJGSom', 2, 'gTzZoz6sCfsgsoY', '低头人生.mp4', 'video/20260421/9976639498gTzZoz6sCfsgsoY', 168, 1, 2, 1, 0.00, NULL, NULL, '低风险 - 建议通过; no risky segments detected.', '2026-04-21 19:40:24', '2026-04-21 19:49:05');
INSERT INTO `ai_audit_task_item` VALUES (7, 7, 'Yr5XeOni5l', 'y9SYepiMqsdfHHMhEV21', 1, 'Zf3jZEUt5rx7F5Y', '打瓦.mp4', 'video/20260421/9976639498Zf3jZEUt5rx7F5Y', 120, 1, 2, 2, 0.85, '[\"暴力\"]', '[{\"reason\": \"画面显示第一人称射击游戏界面，包含武器和战斗HUD，符合暴力内容特征；文字内容虽语义不清但含攻击性语句，可能涉及言语暴力；声音检测到枪声和爆炸声，置信度较高，明确指向暴力场景。三方面综合判断为高风险暴力内容。\", \"riskType\": \"暴力\", \"riskScore\": 0.85, \"segmentId\": 2, \"endSeconds\": 37.02, \"textPreview\": \"哎你也肯定有一个问题绝对在天路荒而逃我反而还是你回来太对了防止重点哎你上去画准啊\", \"hasRiskSound\": true, \"startSeconds\": 25.98, \"bestFramePath\": \"audit-snapshot/Yr5XeOni5l/1/y9SYepiMqsdfHHMhEV21_2.jpg\"}, {\"reason\": \"画面显示第一人称视角持刀，场景为游戏对战环境，有远处敌人及爆炸特效；文字内容“mouth”无明显违规；声音检测到枪声和爆炸声（置信度0.62和0.43），符合游戏战斗场景。综合判断为游戏内战斗行为，属于暴力内容，但因是游戏场景且声音置信度中等，风险等级较高，建议拦截。\", \"riskType\": \"暴力\", \"riskScore\": 0.85, \"segmentId\": 3, \"endSeconds\": 75.24, \"textPreview\": \"mouth\", \"hasRiskSound\": true, \"startSeconds\": 56.82, \"bestFramePath\": \"audit-snapshot/Yr5XeOni5l/1/y9SYepiMqsdfHHMhEV21_3.jpg\"}]', '高风险 - 建议拦截; riskySegments=2; topRisk=暴力@25.98-37.02s', '2026-04-21 19:56:41', '2026-04-21 20:00:43');
INSERT INTO `ai_audit_task_item` VALUES (8, 8, '8B64N7Jkju', 'XVtAtke4RtA1QoyV3ap4', 1, 'rbDvzvoE5TQsgmW', '189392952-1-208.mp4', 'video/20260429/9976639498rbDvzvoE5TQsgmW', 179, 1, 2, 2, 0.95, '[\"暴力\"]', '[{\"reason\": \"画面中多次出现持枪人员、枪械特写及对峙场景，符合暴力内容定义；文字部分无违规内容；声音为音乐，无枪声或爆炸声等暴力音效。综合判断为高风险暴力内容，建议拦截。\", \"riskType\": \"暴力\", \"riskScore\": 0.92, \"segmentId\": 1, \"endSeconds\": 6.84, \"textPreview\": \"\", \"hasRiskSound\": false, \"startSeconds\": 0.0, \"bestFramePath\": \"audit-snapshot/8B64N7Jkju/1/XVtAtke4RtA1QoyV3ap4_1.jpg\"}, {\"reason\": \"画面中多次出现持枪、射击、枪械特写及人物处于紧张对峙或被压制状态，符合暴力内容定义；文字中包含“play with fire”“taste the heat”等与暴力行为相关的隐喻性表达，且配合枪械画面强化暴力氛围；背景声音包含“Vehicle, Train”等可能暗示枪战或追逐场景的环境音，综合判断为高风险暴力内容，建议拦截。\", \"riskType\": \"暴力\", \"riskScore\": 0.92, \"segmentId\": 2, \"endSeconds\": 176.37, \"textPreview\": \"i like the match to taste the heati\'ve always liked to play withfirelight i would always like to place awayI love to wat\", \"hasRiskSound\": false, \"startSeconds\": 6.84, \"bestFramePath\": \"audit-snapshot/8B64N7Jkju/1/XVtAtke4RtA1QoyV3ap4_2.jpg\"}, {\"reason\": \"画面中多次出现手持枪械的特写镜头，人物正在操作枪支，符合暴力内容定义；文字无违规内容；背景声音为音乐，非枪声或爆炸声。综合判断，该片段属于暴力内容，风险等级高，建议拦截。\", \"riskType\": \"暴力\", \"riskScore\": 0.92, \"segmentId\": 3, \"endSeconds\": 206.37, \"textPreview\": \"\", \"hasRiskSound\": false, \"startSeconds\": 176.37, \"bestFramePath\": \"audit-snapshot/8B64N7Jkju/1/XVtAtke4RtA1QoyV3ap4_3.jpg\"}, {\"reason\": \"画面中显示人物手持枪械，进行武器操作和瞄准等动作，属于典型暴力内容；虽然无文字描述暴力，但画面本身明确呈现武器使用场景；背景音乐为拉丁美洲风格音乐，与暴力画面无冲突，但结合画面内容，整体构成高风险暴力内容，符合暴力违规标准。\", \"riskType\": \"暴力\", \"riskScore\": 0.92, \"segmentId\": 4, \"endSeconds\": 236.37, \"textPreview\": \"\", \"hasRiskSound\": false, \"startSeconds\": 206.37, \"bestFramePath\": \"audit-snapshot/8B64N7Jkju/1/XVtAtke4RtA1QoyV3ap4_4.jpg\"}, {\"reason\": \"画面中显示人物手持枪械，进行武器操作和瞄准等动作，属于典型暴力内容；文字无违规；背景声音为音乐，非暴力音效。综合判断为高风险暴力内容，建议拦截。\", \"riskType\": \"暴力\", \"riskScore\": 0.95, \"segmentId\": 5, \"endSeconds\": 266.37, \"textPreview\": \"\", \"hasRiskSound\": false, \"startSeconds\": 236.37, \"bestFramePath\": \"audit-snapshot/8B64N7Jkju/1/XVtAtke4RtA1QoyV3ap4_5.jpg\"}, {\"reason\": \"画面中显示人物手持枪械，进行武器操作和瞄准等动作，属于典型暴力内容；文字无违规；背景声音为音乐，非暴力音效。综合判断为高风险暴力内容，建议拦截。\", \"riskType\": \"暴力\", \"riskScore\": 0.95, \"segmentId\": 6, \"endSeconds\": 296.37, \"textPreview\": \"\", \"hasRiskSound\": false, \"startSeconds\": 266.37, \"bestFramePath\": \"audit-snapshot/8B64N7Jkju/1/XVtAtke4RtA1QoyV3ap4_6.jpg\"}, {\"reason\": \"画面中显示人物手持枪械，进行武器操作和瞄准等动作，属于典型暴力内容；文字无违规；背景声音为音乐，非暴力音效。综合判断为高风险暴力内容，建议拦截。\", \"riskType\": \"暴力\", \"riskScore\": 0.95, \"segmentId\": 7, \"endSeconds\": 326.37, \"textPreview\": \"\", \"hasRiskSound\": false, \"startSeconds\": 296.37, \"bestFramePath\": \"audit-snapshot/8B64N7Jkju/1/XVtAtke4RtA1QoyV3ap4_7.jpg\"}, {\"reason\": \"画面中显示人物手持枪械，进行武器操作和瞄准等动作，属于典型暴力内容；文字无违规；背景声音为音乐，非暴力音效。综合判断为高风险暴力内容，建议拦截。\", \"riskType\": \"暴力\", \"riskScore\": 0.95, \"segmentId\": 8, \"endSeconds\": 356.37, \"textPreview\": \"\", \"hasRiskSound\": false, \"startSeconds\": 326.37, \"bestFramePath\": \"audit-snapshot/8B64N7Jkju/1/XVtAtke4RtA1QoyV3ap4_8.jpg\"}, {\"reason\": \"画面中显示人物手持枪械，且有装有消音器的武器，属于典型暴力武器展示；虽无直接打斗或流血画面，但武器呈现及持枪动作符合暴力内容定义；背景音乐为卡点音乐，无直接暴力音效，但结合画面内容整体构成暴力风险；文字和标题无违规内容，但画面本身已构成高风险暴力内容，综合判断为高风险。\", \"riskType\": \"暴力\", \"riskScore\": 0.92, \"segmentId\": 9, \"endSeconds\": 357.64, \"textPreview\": \"\", \"hasRiskSound\": false, \"startSeconds\": 356.37, \"bestFramePath\": \"audit-snapshot/8B64N7Jkju/1/XVtAtke4RtA1QoyV3ap4_9.jpg\"}]', '高风险 - 建议拦截; riskySegments=9; topRisk=暴力@0.0-6.84s; modalities=audio,audio_event,metadata_text,speech_text,vad,visual; coverage=complete', '2026-04-29 18:36:10', '2026-04-29 18:52:33');
INSERT INTO `ai_audit_task_item` VALUES (9, 9, 'a49SrbrIlN', 'kxqc3Ed7YfiyOoanqrry', 1, 'EZIBIeCoXQWOY4c', '189392952-1-208.mp4', 'video/20260429/9976639498EZIBIeCoXQWOY4c', 179, 1, 2, 2, 0.92, '[\"violence\"]', '[{\"reason\": \"画面中多次出现持枪人员、枪械特写及对峙场景，符合暴力内容定义；文字部分无违规，但背景音乐为卡点音乐，未提供具体音频内容，但画面本身已构成明显暴力元素；综合判断为高风险暴力内容，建议拦截。\", \"riskType\": \"violence\", \"riskScore\": 0.92, \"segmentId\": 1, \"endSeconds\": 6.84, \"textPreview\": \"\", \"hasRiskSound\": false, \"startSeconds\": 0.0, \"bestFramePath\": \"audit-snapshot/a49SrbrIlN/1/kxqc3Ed7YfiyOoanqrry_1.jpg\"}]', '高风险 - 建议拦截; riskySegments=1; topRisk=violence@0.0-6.84s; modalities=audio,audio_event,metadata_text,speech_text,vad,visual; coverage=complete', '2026-04-29 19:18:20', '2026-04-29 19:23:53');
INSERT INTO `ai_audit_task_item` VALUES (10, 10, 'lc6beANXzy', 'UQbHGIRr2ndprPRBZSAV', 1, 'zwzGpCGb75O2KSW', '189392952-1-208.mp4', 'video/20260429/9976639498zwzGpCGb75O2KSW', 179, 1, 2, 2, 0.92, '[\"violence\"]', '[{\"reason\": \"画面中多次出现持枪人员、枪械特写及对峙场景，符合暴力内容定义；文字部分无违规，但背景音乐为卡点音乐，无直接暴力音效；整体内容为电影剪辑，但暴力画面占比高，视觉冲击强烈，综合风险评分高，建议拦截。\", \"isRisky\": true, \"riskType\": \"violence\", \"riskScore\": 0.92, \"segmentId\": 1, \"endSeconds\": 6.84, \"sourceType\": \"visual_fallback\", \"textPreview\": \"\", \"hasRiskSound\": false, \"startSeconds\": 0.0, \"bestFramePath\": \"audit-snapshot/lc6beANXzy/1/UQbHGIRr2ndprPRBZSAV_1.jpg\"}, {\"reason\": \"画面中包含多处持枪、射击、枪械特写及人物处于紧张对峙或被压制状态的场景，符合暴力内容定义；文字内容包含\'play with fire\'、\'taste the heat\'、\'I love to watch the castles burn\'等具有暴力美学和血腥暗示的歌词，强化暴力氛围；背景声音为叙述性旁白和chant，配合画面营造紧张刺激的枪战氛围，整体构成高风险暴力内容。\", \"isRisky\": true, \"riskType\": \"violence\", \"riskScore\": 0.92, \"segmentId\": 2, \"endSeconds\": 176.37, \"sourceType\": \"speech\", \"textPreview\": \"i like the match to taste the heati\'ve always liked to play with fire i always like to play fire i ride the edge my spee\", \"hasRiskSound\": false, \"startSeconds\": 6.84, \"bestFramePath\": \"audit-snapshot/lc6beANXzy/1/UQbHGIRr2ndprPRBZSAV_2.jpg\"}, {\"reason\": \"画面展示的是手持枪械的特写镜头，属于影视剪辑中常见的动作场景，无实际暴力行为或血腥内容；文字部分无任何违规信息；背景声音为音乐，非枪声或爆炸声。整体内容符合卡点剪辑的常规表现形式，无政治敏感、色情或恐怖元素，风险极低。\", \"isRisky\": false, \"riskType\": \"normal\", \"riskScore\": 0.1, \"segmentId\": 3, \"endSeconds\": 179.51, \"sourceType\": \"visual_fallback\", \"textPreview\": \"\", \"hasRiskSound\": false, \"startSeconds\": 176.37, \"bestFramePath\": \"audit-snapshot/lc6beANXzy/1/UQbHGIRr2ndprPRBZSAV_3.jpg\"}]', '高风险 - 建议拦截; riskySegments=2; topRisk=violence@0.0-6.84s; modalities=audio,audio_event,metadata_text,speech_text,vad,visual; coverage=complete', '2026-04-29 23:14:28', '2026-04-29 23:20:39');
INSERT INTO `ai_audit_task_item` VALUES (11, 11, 'taQehKU5du', 'wYn15OnTnkyB7z6wjDS7', 1, 'Scp1I3kBd86SGv0', '189392952-1-208.mp4', 'video/20260430/9976639498Scp1I3kBd86SGv0', 179, 1, 2, 3, 0.50, '[\"audio_event\"]', '[{\"reason\": \"画面中展示的是影视作品中的武器（如步枪）和人物持枪动作，属于虚构场景，无真实暴力、流血或伤害行为；文字内容为空；背景声音为音乐，无枪声或爆炸声；元数据表明为电影剪辑，符合影视内容范畴。未发现色情、政治、诈骗、赌博、毒品、未成年人风险、自残、隐私泄露、仇恨言论、恐怖极端、低俗擦边、动物虐待、版权侵权或非法交易等违规类型。综合判断为正常影视内容。\", \"isRisky\": false, \"riskType\": \"normal\", \"riskScore\": 0.25, \"segmentId\": 1, \"endSeconds\": 6.84, \"sourceType\": \"visual_fallback\", \"textPreview\": \"\", \"hasRiskSound\": false, \"startSeconds\": 0.0, \"bestFramePath\": \"audit-snapshot/taQehKU5du/1/wYn15OnTnkyB7z6wjDS7_1.jpg\"}, {\"reason\": \"画面展示的是影视片段，包含持枪、驾驶、科幻场景等，但无真实暴力、血腥或伤害行为；文字为歌词片段，内容涉及危险、火、秘密等抽象表达，无色情、政治或低俗暗示；背景声音为Skateboard、Speech、Train，无枪声、爆炸或尖叫声等危险音频；整体符合影视卡点剪辑的常规内容，无违规元素。\", \"isRisky\": false, \"riskType\": \"normal\", \"riskScore\": 0.15, \"segmentId\": 2, \"endSeconds\": 36.84, \"sourceType\": \"speech\", \"textPreview\": \"i \'m saying inside the danger gets me high can \'t help myself got secrets i can \'t tell al ong the smell of gasoline i l\", \"hasRiskSound\": false, \"startSeconds\": 6.84, \"bestFramePath\": \"audit-snapshot/taQehKU5du/1/wYn15OnTnkyB7z6wjDS7_2.jpg\"}, {\"reason\": \"画面展示的是持枪、战术装备、车辆等影视场景，但无真实暴力、流血或伤害行为；文字内容为歌词式表达，涉及‘play with fire’等比喻性语句，非真实暴力威胁；背景声音为音乐、滑板、车辆声，无枪声或爆炸等危险音效；整体属于影视剪辑卡点，符合平台对非真实暴力内容的容忍度，无违规风险。\", \"isRisky\": false, \"riskType\": \"normal\", \"riskScore\": 0.25, \"segmentId\": 3, \"endSeconds\": 66.84, \"sourceType\": \"speech\", \"textPreview\": \"i like to play with fire i like to play with fire i \'ve always liked to play with fire i like the edge my speed goes in \", \"hasRiskSound\": false, \"startSeconds\": 36.84, \"bestFramePath\": \"audit-snapshot/taQehKU5du/1/wYn15OnTnkyB7z6wjDS7_3.jpg\"}, {\"reason\": \"画面展示的是影视作品中的枪械、持枪动作及战斗场景，但无真实暴力伤害、血腥或威胁性内容；文字为歌词片段，表达的是对火与毁灭的审美化描述，非暴力或色情语义；声音检测到chant，但无具体歌词内容支持违规，且上下文为音乐卡点剪辑，属于艺术表达范畴。综合判断为非违规内容。 Risk audio event detected by YAMNet: Chant(0.45); fallback to manual review.\", \"isRisky\": true, \"riskType\": \"audio_event\", \"riskScore\": 0.5, \"segmentId\": 4, \"endSeconds\": 96.84, \"sourceType\": \"speech\", \"textPreview\": \"i love to watch the cast les burn these golden ashes turn to dirt i \'ve always liked to play with fire i like to play wi\", \"hasRiskSound\": true, \"startSeconds\": 66.84, \"bestFramePath\": \"audit-snapshot/taQehKU5du/1/wYn15OnTnkyB7z6wjDS7_4.jpg\"}, {\"reason\": \"画面展示的是影视片段中的武器持握、枪械特写及人物在丛林等场景中，属于影视作品中常见的武器展示或战斗准备场景，无真实暴力、血腥或伤害行为。文字内容为重复的歌词片段（\'I\'ve always liked to play with fire\'等），属于音乐卡点剪辑中的歌词，无色情、政治或违规语义。背景音乐为拉丁美洲弗拉门戈风格，无违规音频事件。整体内容符合影视剪辑与音乐卡点的常规用途，无平台禁止内容。\", \"isRisky\": false, \"riskType\": \"normal\", \"riskScore\": 0.15, \"segmentId\": 5, \"endSeconds\": 126.84, \"sourceType\": \"speech\", \"textPreview\": \"i like to play with fire i like to play with fire i \'ve always liked to play with fire i like to watch the flames get hi\", \"hasRiskSound\": false, \"startSeconds\": 96.84, \"bestFramePath\": \"audit-snapshot/taQehKU5du/1/wYn15OnTnkyB7z6wjDS7_5.jpg\"}, {\"reason\": \"画面展示的是影视片段中的角色持枪、对峙等场景，但无真实暴力、流血或伤害行为；文字内容为重复的歌词片段（\'I\'ve always liked to play with fire\'等），属于影视音乐卡点剪辑常见语境，无色情、政治或违规诱导内容；背景为音乐，无枪声、爆炸等危险音效；整体符合影视剪辑卡点测试的常规内容，无平台禁止的违规类型。\", \"isRisky\": false, \"riskType\": \"normal\", \"riskScore\": 0.15, \"segmentId\": 6, \"endSeconds\": 156.84, \"sourceType\": \"speech\", \"textPreview\": \"play with fire i like to play with fire We edo\", \"hasRiskSound\": false, \"startSeconds\": 126.84, \"bestFramePath\": \"audit-snapshot/taQehKU5du/1/wYn15OnTnkyB7z6wjDS7_6.jpg\"}, {\"reason\": \"画面展示的是影视作品中的武器持握和射击场景，但无真实暴力、流血或伤害行为；文字内容为重复的歌词片段，无色情或政治敏感词；背景音乐为影视配乐，无枪声或爆炸声等真实危险音效。整体属于影视剪辑卡点，符合平台对非真实暴力内容的宽松处理标准，无违规风险。\", \"isRisky\": false, \"riskType\": \"normal\", \"riskScore\": 0.15, \"segmentId\": 7, \"endSeconds\": 176.37, \"sourceType\": \"speech\", \"textPreview\": \"\", \"hasRiskSound\": false, \"startSeconds\": 156.84, \"bestFramePath\": \"audit-snapshot/taQehKU5du/1/wYn15OnTnkyB7z6wjDS7_7.jpg\"}, {\"reason\": \"画面展示的是手持枪械的特写镜头，属于影视作品中常见的武器展示场景，无真实暴力、流血或打斗动作；文字内容为空；背景声音为音乐，无枪声或爆炸声；结合元数据可知为电影剪辑，属于非真实危险场景，符合影视作品中武器展示的常规处理，不构成平台违规。\", \"isRisky\": false, \"riskType\": \"normal\", \"riskScore\": 0.15, \"segmentId\": 8, \"endSeconds\": 179.51, \"sourceType\": \"visual_fallback\", \"textPreview\": \"\", \"hasRiskSound\": false, \"startSeconds\": 176.37, \"bestFramePath\": \"audit-snapshot/taQehKU5du/1/wYn15OnTnkyB7z6wjDS7_8.jpg\"}]', '中风险 - 建议人工复核; riskySegments=1; topRisk=audio_event@66.84-96.84s; modalities=audio,audio_event,metadata_text,speech_text,vad,visual; coverage=complete', '2026-04-30 00:27:55', '2026-04-30 01:41:25');

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
-- Records of category_info
-- ----------------------------
INSERT INTO `category_info` VALUES (3, 'code', '编程', 0, '', 3);
INSERT INTO `category_info` VALUES (4, 'game', '游戏', 0, 'cover/202603/h0rrr5PFqHdhwh2Hq0XMTzYHl9iT5M.png', 2);
INSERT INTO `category_info` VALUES (6, 'codelanguage', 'java', 3, '', 1);
INSERT INTO `category_info` VALUES (7, 'song', 'song', 0, '', 5);
INSERT INTO `category_info` VALUES (8, 'anime', '动漫', 0, 'cover/202603/0PRmc2ITc6RcFAlcQx7NYTae7WFZlf.png', 6);
INSERT INTO `category_info` VALUES (9, 'live', '生活', 0, 'cover/202603/wJZLGfL2Ie2Pnn48bzv3Hczn6JAcf0.png', 7);
INSERT INTO `category_info` VALUES (11, 'mi', '原神', 4, 'cover/202603/LDOW5yofmeRHTX4xWk9HNaSinOSJhx.png', 1);
INSERT INTO `category_info` VALUES (12, 'movie', '电影', 0, 'cover/20260429/I1pIH97D0Pi0CBFTMKch8b8x04zKC6.webp', 8);

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
INSERT INTO `user_info` VALUES ('9874085469', 'biean', 'jiangbing267@gmail.com', '078577e334574e6952b333ec2f73a255', 1, '2003-07-26', 'asas', '啊啊', '2026-01-29 17:38:02', '2026-04-23 10:58:28', '192.168.255.34', 1, '关注我', 11, 11, 'cover/20260401/bNdzu3itdE9odByaluPOVPCd31xdbP.png');
INSERT INTO `user_info` VALUES ('9976639498', 'sjb', '1678530981@qq.com', '216cb981a053bc960922dd55da684230', 2, NULL, NULL, NULL, '2026-03-22 00:53:10', '2026-04-29 18:34:22', '192.168.1.2', 1, NULL, 10, 9, NULL);

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
INSERT INTO `video_info` VALUES ('vuVLXyezPv', 'cover/20260402/t8EMKs40iFXb5hbg6kay0dX0ITpFzD.jpg', '低头人生', '9976639498', '2026-04-02 20:26:48', '2026-04-02 20:29:42', 4, NULL, 0, NULL, '人生,生活', '人生', NULL, 454, 99, 0, 1, 1, 0, 1, 0, '2026-04-23 11:02:26');

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
INSERT INTO `video_info_file_post` VALUES ('j8NTwZr3id7ADnjDkkdk', '49ejjnhaTJXAs3M', '9976639498', 'qQDkiVveIa', 1, 'video_【动物丰容】5分钟快速理解！_0.mp4', 65249783, 'video/20260421/997663949849ejjnhaTJXAs3M', 1, 1, 381);
INSERT INTO `video_info_file_post` VALUES ('kxqc3Ed7YfiyOoanqrry', 'EZIBIeCoXQWOY4c', '9976639498', 'a49SrbrIlN', 1, '189392952-1-208.mp4', 27070822, 'video/20260429/9976639498EZIBIeCoXQWOY4c', 1, 1, 179);
INSERT INTO `video_info_file_post` VALUES ('nDggQuSRpx7o1ltJGSom', 'gTzZoz6sCfsgsoY', '9976639498', 'qQDkiVveIa', 2, '低头人生.mp4', 61564716, 'video/20260421/9976639498gTzZoz6sCfsgsoY', 1, 1, 168);
INSERT INTO `video_info_file_post` VALUES ('oUHlOnAAGrNADhq7erby', '4XE9BgPlq0YX5Vj', '9874085469', 'uVCqA6teRT', 2, '打瓦.mp4', 24101428, 'video/20260321/98740854694XE9BgPlq0YX5Vj', 0, 1, 120);
INSERT INTO `video_info_file_post` VALUES ('sjo1rSSvrwW32lPvnery', 'uJrTelqnC4nRWO5', '9874085469', '8HJhBHlP6A', 2, 'a356e90f-e6d9-480c-a302-f0d778f6f62a_北京市奖学金申请演讲.mp4', 100259955, 'video/20260324/9874085469uJrTelqnC4nRWO5', 0, 1, 363);
INSERT INTO `video_info_file_post` VALUES ('UQbHGIRr2ndprPRBZSAV', 'zwzGpCGb75O2KSW', '9976639498', 'lc6beANXzy', 1, '189392952-1-208.mp4', 27070822, 'video/20260429/9976639498zwzGpCGb75O2KSW', 1, 1, 179);
INSERT INTO `video_info_file_post` VALUES ('w7oDso9Aa6ombbXmljHK', 'tWnapgHvWydWcRO', '9976639498', 'XSSgmZJFWr', 1, 'video_【动物丰容】5分钟快速理解！_0.mp4', 65249783, 'video/20260421/9976639498tWnapgHvWydWcRO', 1, 1, 381);
INSERT INTO `video_info_file_post` VALUES ('WFQh0PjrDJXZOfd7QtOt', 'aewKDoWbETMavQL', '9874085469', 'uVCqA6teRT', 1, 'a356e90f-e6d9-480c-a302-f0d778f6f62a_北京市奖学金申请演讲.mp4', 100259955, 'video/20260321/9874085469aewKDoWbETMavQL', 0, 1, 363);
INSERT INTO `video_info_file_post` VALUES ('wGi66D9Wg3kQBbWmQO7M', 'KUUemGxL8uxe1jA', '9874085469', '8HJhBHlP6A', 1, '1618703307-1-192.mp4', 48061573, 'video/20260324/9874085469KUUemGxL8uxe1jA', 0, 1, 286);
INSERT INTO `video_info_file_post` VALUES ('wPkB2UEpXtQa4ZFPx4wL', 'U2wMtAXK6NvKScL', '9976639498', 'NhXZfG8EGJ', 1, '442774934-1-208.mp4', NULL, NULL, 0, NULL, NULL);
INSERT INTO `video_info_file_post` VALUES ('wYn15OnTnkyB7z6wjDS7', 'Scp1I3kBd86SGv0', '9976639498', 'taQehKU5du', 1, '189392952-1-208.mp4', 27070822, 'video/20260430/9976639498Scp1I3kBd86SGv0', 1, 1, 179);
INSERT INTO `video_info_file_post` VALUES ('XVtAtke4RtA1QoyV3ap4', 'rbDvzvoE5TQsgmW', '9976639498', '8B64N7Jkju', 1, '189392952-1-208.mp4', 27070822, 'video/20260429/9976639498rbDvzvoE5TQsgmW', 1, 1, 179);
INSERT INTO `video_info_file_post` VALUES ('xZpq3I3TGisSABufSEJ5', 'zXo31oPrgiBIk1J', '9976639498', 'NhXZfG8EGJ', 2, '打瓦.mp4', NULL, NULL, 0, NULL, NULL);
INSERT INTO `video_info_file_post` VALUES ('y9SYepiMqsdfHHMhEV21', 'Zf3jZEUt5rx7F5Y', '9976639498', 'Yr5XeOni5l', 1, '打瓦.mp4', 24101428, 'video/20260421/9976639498Zf3jZEUt5rx7F5Y', 1, 1, 120);
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
INSERT INTO `video_info_post` VALUES ('8B64N7Jkju', 'cover/20260429/IxUVdn8Mc74O2FeWpr5zNKzmQgycnK.png', '189392952-1-208', '9976639498', '2026-04-29 18:36:06', '2026-04-29 18:36:06', 12, NULL, 2, 1, NULL, '卡点', '某电影精彩片段加自增音乐卡点', NULL, 179);
INSERT INTO `video_info_post` VALUES ('8HJhBHlP6A', 'cover/20260324/CuEMjCU698EWCaEUSzqKUAAOgBTueH.png', '1618703307-1-192', '9874085469', '2026-03-24 15:51:13', '2026-03-24 15:51:13', 4, NULL, 3, 0, NULL, '歌曲', 'sdsds', '0', 649);
INSERT INTO `video_info_post` VALUES ('a49SrbrIlN', 'cover/20260429/mQqfwbfgZuoLCjTSJZrqS41fR6YWMp.png', '电影-音乐卡点', '9976639498', '2026-04-29 19:18:18', '2026-04-29 19:18:18', 12, NULL, 2, 1, NULL, '卡点', '某电影精彩片段加自增音乐卡点', NULL, 179);
INSERT INTO `video_info_post` VALUES ('BvvusWNd8J', 'cover/20260309/Wdiv8GHZEDQtD1l4Gmv78YwriXbz5t.jpg', '下载2', '9874085469', '2026-03-09 22:27:46', '2026-03-09 22:27:46', 0, 4, 3, 0, NULL, '游戏', 'aaaa', '', 286);
INSERT INTO `video_info_post` VALUES ('l4BxkDYpn0', 'cover/20260309/eyxo9NIPtNPvIP7d66HyDFwWWsom6P.jpg', '刘德华-练习', '9874085469', '2026-03-09 13:36:19', '2026-03-09 13:36:19', 0, 7, 3, 0, NULL, '刘德华', '歌曲', '', 286);
INSERT INTO `video_info_post` VALUES ('lc6beANXzy', 'cover/20260429/xLgUpvpQzRX3fbubqojlEpmAjqW15C.png', '电影卡点-测试1', '9976639498', '2026-04-29 23:14:23', '2026-04-29 23:14:23', 12, NULL, 2, 0, NULL, '卡点', '电影剪辑，音乐卡点', NULL, 179);
INSERT INTO `video_info_post` VALUES ('qNz6BVuDTC', 'cover/20260308/35CWGhAhxOvrLFWtO7F6qr23iyzLVY.jpg', '下载', '9874085469', '2026-03-08 23:12:51', '2026-03-08 23:12:51', 0, 4, 3, 0, NULL, 'asda', 'asdasd', '', 363);
INSERT INTO `video_info_post` VALUES ('qQDkiVveIa', 'cover/20260421/Zzqx4Kl09O1FQDmOPrCEAEcqGY15Yi.png', '合集01', '9976639498', '2026-04-21 19:40:21', '2026-04-21 19:40:21', 9, NULL, 2, 0, NULL, '人生,动物', '测试合集', NULL, 549);
INSERT INTO `video_info_post` VALUES ('taQehKU5du', 'cover/20260430/twPtcF4qph5zIAPBEDM5uxFnHEYqRN.png', '电影-音乐卡点测试3', '9976639498', '2026-04-30 00:27:53', '2026-04-30 00:27:53', 12, NULL, 2, 0, NULL, '卡点', '电影剪辑，音乐卡点', NULL, 179);
INSERT INTO `video_info_post` VALUES ('uVCqA6teRT', 'cover/20260321/pU4ve8W9jW3KkZXbF2UDBl8lQ0jiFb.png', '测试数据1', '9874085469', '2026-03-21 13:13:23', '2026-03-21 13:13:23', 4, NULL, 3, 0, NULL, '终末地', '测试数据1', NULL, 483);
INSERT INTO `video_info_post` VALUES ('vuVLXyezPv', 'cover/20260402/t8EMKs40iFXb5hbg6kay0dX0ITpFzD.jpg', '低头人生', '9976639498', '2026-04-02 20:26:48', '2026-04-02 20:29:42', 4, NULL, 3, 0, NULL, '人生,生活', '人生', NULL, 454);
INSERT INTO `video_info_post` VALUES ('XSSgmZJFWr', 'cover/20260421/Ft47P8vxhQdvj7zQNd8KRQ6Zinzql6.png', '动物', '9976639498', '2026-04-21 13:41:23', '2026-04-21 13:41:23', 9, NULL, 2, 0, NULL, '动物', '了解动物i，理解动物', NULL, 381);
INSERT INTO `video_info_post` VALUES ('Yr5XeOni5l', 'cover/20260421/TzvmsFwxDruZHIgOn1Gdx7QFoJj1wh.png', '打瓦', '9976639498', '2026-04-21 19:56:40', '2026-04-21 19:56:40', 4, NULL, 2, 0, NULL, '游戏', NULL, NULL, 120);

SET FOREIGN_KEY_CHECKS = 1;
