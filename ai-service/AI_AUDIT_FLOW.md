# AI 审核逻辑流程说明

本文档按当前代码实现整理 `streama-cloud-ai` 的视频 AI 审核流程，重点说明：

- 从 RabbitMQ 收到审核请求后，代码调用了哪些方法。
- 每个方法负责什么。
- 中间数据和最终返回数据大概长什么样。
- 当前实现里可能出现漏检或审核不完整的情况。

文档基于当前代码文件：

- `main.py`
- `streama_ai_service/app.py`
- `streama_ai_service/mq.py`
- `streama_ai_service/service.py`
- `streama_ai_service/moderation.py`
- `streama_ai_service/video_moderation.py`
- `streama_ai_service/models.py`

## 1. 总体调用链

当前服务不是通过业务 HTTP 接口直接审核视频，而是通过 RabbitMQ 异步消费审核任务。

整体调用链如下：

```text
main.py
  -> create_app()
  -> AppContainer.start()
  -> RabbitAuditWorker.start()
  -> RabbitAuditWorker._on_message()
  -> AuditRequestMessage.model_validate_json()
  -> AuditWorkflow.process_request()
  -> InternalApiClient.download_temp_video()
  -> ModerationEngine.audit_video()
  -> VideoModerationService 各个模型处理方法
  -> VideoModerationService.generate_audit_report()
  -> AuditWorkflow._build_item_success()
  -> AuditResultMessage.to_message_dict()
  -> RabbitAuditWorker._publish_result()
  -> RabbitMQ result routing key
```

核心思想是：

1. 从 MQ 收到审核请求。
2. 下载每个待审核视频文件到本地临时目录。
3. 对视频做音频、语音、画面、文本、声音事件分析。
4. 将画面、转写文本、声音事件、投稿元数据组装成审核 segment。
5. 对每个 segment 调用 Qwen 多模态模型审核。
6. 融合文本规则、音频风险、业务阈值。
7. 生成内部 report。
8. 转换为上游需要的 MQ 审核结果消息。

## 2. 服务启动流程

### 2.1 `main.py`

入口文件：

```python
app = create_app()
```

如果直接运行：

```python
uvicorn.run("main:app", ...)
```

负责启动 FastAPI 应用。

### 2.2 `streama_ai_service.app:create_app()`

`create_app()` 做几件事：

- 加载配置。
- 创建 `AppContainer`。
- 注册 FastAPI 生命周期。
- 暴露健康检查接口：
  - `GET /health`
  - `GET /actuator/health`
  - `GET /actuator/info`

### 2.3 `AppContainer`

`AppContainer.__init__()` 初始化核心组件：

```text
ModerationEngine
AuditWorkflow
NacosRegistration
RabbitAuditWorker
```

`AppContainer.start()` 启动时执行：

```text
configure_logging()
engine.preload()
nacos.start()
rabbit_worker.start()
```

其中 `engine.preload()` 会提前加载审核模型。如果核心模型加载失败，服务启动会失败。

## 3. RabbitMQ 消费与发布流程

### 3.1 `RabbitAuditWorker._on_message()`

位置：`streama_ai_service/mq.py`

收到 MQ 消息后：

1. 使用 `AuditRequestMessage.model_validate_json(body)` 解析请求 JSON。
2. 构造请求去重标识 `_build_request_identity()`。
3. 如果是重复请求：
   - 已完成：复用缓存结果。
   - 正在处理：合并 delivery，避免重复跑模型。
4. 如果是新请求：放入 worker 队列。

解析失败时，会调用：

```python
AuditWorkflow.build_failure_result_from_payload()
```

生成人工复核失败结果并回传。

### 3.2 `RabbitAuditWorker._worker_loop()`

后台 worker 从队列取出请求，调用：

```python
AuditWorkflow.process_request()
```

处理完成后，将结果调度回 RabbitMQ 线程发布。

### 3.3 `RabbitAuditWorker._publish_result()`

发布结果时调用：

```python
result_message.to_message_dict()
```

然后将 JSON 发布到：

```text
exchange = streama.audit.exchange
routing_key = audit.video.result
```

发布成功后 `basic_ack()`；发布失败时 `basic_nack(requeue=False)`，消息进入死信队列。

## 4. 入站请求数据格式

请求模型定义在 `streama_ai_service/models.py`。

顶层模型：`AuditRequestMessage`

主要字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `requestId` | string | 审核请求 ID |
| `videoId` | string | 视频 ID |
| `auditVersion` | int | 审核版本 |
| `sourceType` | int | 来源类型 |
| `triggerTime` | datetime/int/string | 触发时间 |
| `videoMeta` | object | 视频元数据 |
| `items` | array | 待审核文件列表 |

`videoMeta` 对应 `AuditRequestVideoMeta`：

| 字段 | 说明 |
| --- | --- |
| `userId` | 用户 ID |
| `videoName` | 视频标题 |
| `videoCover` | 封面 |
| `tags` | 标签 |
| `introduction` | 简介 |
| `pCategoryId` | 一级分类 |
| `categoryId` | 二级分类 |
| `postType` | 投稿类型 |

`items` 中每个元素对应 `AuditRequestItem`：

| 字段 | 说明 |
| --- | --- |
| `fileId` | 文件 ID |
| `fileIndex` | 文件序号 |
| `uploadId` | 上传 ID |
| `fileName` | 文件名 |
| `filePath` | 文件路径 |
| `duration` | 文件时长 |
| `updateType` | 更新类型 |

请求示例：

```json
{
  "requestId": "req-001",
  "videoId": "video-001",
  "auditVersion": 1,
  "sourceType": 1,
  "triggerTime": 1710000000000,
  "videoMeta": {
    "userId": "user-001",
    "videoName": "示例视频标题",
    "videoCover": "cover.jpg",
    "tags": "游戏,剪辑",
    "introduction": "视频简介",
    "pCategoryId": 1,
    "categoryId": 10,
    "postType": 1
  },
  "items": [
    {
      "fileId": "file-001",
      "fileIndex": 0,
      "uploadId": "upload-001",
      "fileName": "temp.mp4",
      "filePath": "video/2026/05/09/file-001",
      "duration": 120,
      "updateType": 1
    }
  ]
}
```

## 5. 业务编排流程

### 5.1 `AuditWorkflow.process_request()`

位置：`streama_ai_service/service.py`

这个方法负责处理一整个审核请求。

流程：

1. 遍历 `request.items`。
2. 每个 item 先调用进度回调：

```python
InternalApiClient.update_item_progress(..., ITEM_PROCESSING)
```

3. 下载视频：

```python
InternalApiClient.download_temp_video(request, item)
```

4. 调用审核引擎：

```python
ModerationEngine.audit_video(local_source, video_meta=request.video_meta, item_meta=item)
```

5. 成功则调用：

```python
AuditWorkflow._build_item_success()
```

6. 失败则调用：

```python
AuditWorkflow._build_item_failure()
```

7. 清理临时文件目录。
8. 最后调用 `_build_result()` 聚合整条视频审核结果。

### 5.2 `InternalApiClient.download_temp_video()`

负责下载视频文件到本地。

下载地址：

```text
GET {resource_base_url}/getResource?sourceName=...
```

`sourceName` 由 `filePath` 计算：

- 如果 `filePath` 已经以 `.mp4` 结尾，直接使用。
- 否则拼成 `filePath/temp.mp4`。

本地保存路径：

```text
temp/requests/{requestId}/{fileId}/temp.mp4
```

## 6. AI 审核主流程

主入口：`ModerationEngine.audit_video()`

位置：`streama_ai_service/moderation.py`

该方法负责串起整个 `VideoModerationService` 流水线。

### 6.1 初始化运行状态

方法内部会初始化：

```python
coverage_issues = []
modalities_checked = set()
audio_file = None
voice_segments = []
sound_events = []
transcriptions = []
```

含义：

| 变量 | 说明 |
| --- | --- |
| `coverage_issues` | 审核覆盖不完整的问题记录 |
| `modalities_checked` | 已检查的模态，如 visual/audio/vad/speech_text |
| `audio_file` | 抽取出的 WAV 音频路径 |
| `voice_segments` | VAD 检测到的人声片段 |
| `sound_events` | YAMNet 检测到的声音事件 |
| `transcriptions` | Whisper 转写结果 |

### 6.2 检测视频是否有音轨

调用：

```python
VideoModerationService.video_has_audio_stream(source)
```

负责使用 `ffmpeg.probe()` 检测视频里是否有 audio stream。

返回：

```python
True 或 False
```

如果检测失败：

```text
coverage_issues += ["audio_probe_error:..."]
```

并默认按有音频处理。

### 6.3 抽取音频

调用：

```python
VideoModerationService.extract_audio_from_video(source)
```

负责用 ffmpeg 从视频中抽取音频：

- 编码：`pcm_s16le`
- 声道：单声道
- 采样率：`16000`
- 输出：WAV 文件

返回示例：

```text
temp/audio_ab12cd34.wav
```

成功后：

```python
modalities_checked.add("audio")
```

失败后：

```text
coverage_issues += ["audio_extract_error:..."]
```

### 6.4 VAD 人声检测

调用：

```python
VideoModerationService.detect_voice_segments(audio_file)
```

负责使用 WebRTC VAD 检测人声时间段。

默认参数：

```python
min_segment_duration=5
max_silence_duration=2
```

返回数据格式：

```json
[
  {
    "start": 12.3,
    "end": 20.5,
    "duration": 8.2
  }
]
```

成功后：

```python
modalities_checked.add("vad")
```

失败后：

```text
coverage_issues += ["vad_error:..."]
```

注意：如果 VAD 没检测到人声，返回空数组，后续 Whisper 不会执行。

### 6.5 YAMNet 声音事件检测

调用：

```python
VideoModerationService.detect_sound_events(
    audio_file,
    top_k=settings.sound_top_k,
    confidence_threshold=settings.sound_confidence_threshold,
)
```

负责检测背景声音事件，包括普通声音和风险声音。

当前风险声音类别包括：

- 枪声
- 爆炸
- 尖叫
- 玻璃破碎
- 警报
- 打斗
- 性暗示声音
- 哭喊、痛苦声音
- 群体骚乱
- 警笛

返回数据格式：

```json
[
  {
    "time": 2.0,
    "class": "Gunshot",
    "confidence": 0.82,
    "is_risk": true,
    "risk_category": "gunshot"
  }
]
```

成功后：

```python
modalities_checked.add("audio_event")
```

失败后：

```text
coverage_issues += ["yamnet_error:..."]
```

### 6.6 Whisper 语音转写

调用条件：

```python
if voice_segments:
    transcriptions = service.transcribe_audio_segments(...)
```

也就是说，只有 VAD 检测到人声片段时，Whisper 才会执行。

调用：

```python
VideoModerationService.transcribe_audio_segments(
    audio_file,
    voice_segments,
    language=settings.language,
)
```

返回数据格式：

```json
[
  {
    "segment_id": 1,
    "start": 12.3,
    "end": 20.5,
    "duration": 8.2,
    "text": "这里是语音转写文本",
    "language": "zh",
    "segments": [
      {
        "start": 12.3,
        "end": 14.0,
        "text": "这里是语音",
        "words": []
      }
    ],
    "words": [
      {
        "word": "这里",
        "start": 12.3,
        "end": 12.8,
        "probability": 0.9
      }
    ]
  }
]
```

成功后：

```python
modalities_checked.add("speech_text")
```

失败后：

```text
coverage_issues += ["whisper_error:..."]
```

### 6.7 抽取关键帧

先从 YAMNet 结果里取风险声音时间：

```python
risk_sound_times = [
    event["time"]
    for event in sound_events
    if event.get("is_risk")
]
```

然后调用：

```python
VideoModerationService.extract_keyframes(
    source,
    extraction_rate=settings.extraction_rate,
    focus_times=risk_sound_times,
)
```

该方法负责从视频中抽帧。

当前抽帧来源包括：

- 按 `extraction_rate` 均匀抽帧。
- 视频开头和结尾附近的必选帧。
- 风险声音时间点附近的焦点帧。
- 场景突变前后的 transition 帧。

返回数据格式：

```json
[
  {
    "frame_id": 0,
    "timestamp": 0.0,
    "path": "keyframes/abcd1234/frame_0000_0.00s.jpg",
    "frame_count": 0,
    "reasons": ["transition_after"]
  }
]
```

部分均匀抽出的帧可能没有 `reasons` 字段。

成功后：

```python
modalities_checked.add("visual")
```

如果没有抽到帧：

```text
coverage_issues += ["no_keyframes"]
```

如果抽帧异常：

```text
coverage_issues += ["keyframe_extract_error:..."]
```

### 6.8 构建审核 segment

调用：

```python
VideoModerationService.build_audit_segments(
    transcriptions=transcriptions,
    frames_info=frames_info,
    sound_events=sound_events,
    video_path=source,
    video_meta=video_meta,
    item_meta=item_meta,
    coverage_issues=coverage_issues,
)
```

该方法负责将转写文本、关键帧、声音事件和元数据组合成审核片段。

主要逻辑：

1. 从 `video_meta` 和 `item_meta` 构造 `metadata_summary`。
2. 估算视频时长。
3. 如果有 Whisper 转写：
   - 按语音片段生成 `speech` segment。
   - 长语音会按 30 秒视觉窗口拆分。
   - 如果有词级时间戳，则每个窗口只取窗口内文本。
4. 计算没有语音覆盖的时间段。
5. 对无语音覆盖的画面生成 `visual_fallback` segment。
6. 如果完全没有 segment 但有关键帧，则用关键帧时间范围生成兜底 segment。
7. 为每个 segment 选择候选帧、最佳帧和 contact sheet。

segment 简化结构：

```json
{
  "segment_id": 1,
  "start": 0.0,
  "end": 30.0,
  "duration": 30.0,
  "text": "当前窗口文本",
  "window_text": "当前窗口文本",
  "source_transcript_text": "",
  "source_text_time_range": "",
  "text_scope": "window",
  "source_type": "speech",
  "metadata_summary": "title=标题; tags=标签",
  "text_policy_matches": [],
  "text_policy_summary": "无文本规则命中",
  "long_text_context": false,
  "best_frame_time": 12.0,
  "best_frame_path": "keyframes/abcd/frame_0012_12.00s.jpg",
  "contact_sheet_path": "keyframes/abcd/segment_1_contact.jpg",
  "segment_frames": [
    {
      "frame_id": 12,
      "timestamp": 12.0,
      "path": "keyframes/abcd/frame_0012_12.00s.jpg"
    }
  ],
  "frame_count": 5,
  "time_diff": 0.2,
  "modalities": ["visual", "speech_text", "metadata_text"],
  "coverage_issues": []
}
```

### 6.9 对齐声音事件

调用：

```python
VideoModerationService.align_sound_with_segments(aligned_segments, sound_events)
```

该方法把 YAMNet 声音事件按时间范围贴回每个 segment。

新增或更新字段：

```json
{
  "sound_events": [
    {
      "time": 2.0,
      "class": "Gunshot",
      "confidence": 0.82,
      "is_risk": true,
      "risk_category": "gunshot"
    }
  ],
  "risk_sounds": [
    {
      "time": 2.0,
      "class": "Gunshot",
      "confidence": 0.82,
      "is_risk": true,
      "risk_category": "gunshot"
    }
  ],
  "has_risk_sound": true,
  "sound_summary": "检测到风险声音..."
}
```

如果 segment 有声音事件，会在 `modalities` 里加入：

```text
audio_event
```

### 6.10 Qwen 多模态审核

批量入口：

```python
VideoModerationService.batch_audit(aligned_segments, max_segments=None)
```

内部逐个 segment 调用：

```python
VideoModerationService.audit_with_qwen(segment)
```

`audit_with_qwen()` 会：

1. 优先使用 `contact_sheet_path` 作为图片。
2. 如果没有 contact sheet，使用 `best_frame_path`。
3. 如果还没有，尝试使用 `segment_frames[0].path`。
4. 调用 `build_audit_prompt(segment)` 构造提示词。
5. 将图片和 prompt 送给 Qwen。
6. 解析 Qwen 返回的 JSON。
7. 写入模型输入 trace。

Qwen prompt 中包含：

- 时间范围。
- 画面信息。
- 当前窗口语音文本。
- 长语音上下文。
- 投稿标题、简介、标签、文件名。
- 文本规则预检结果。
- 背景声音摘要。
- 审核风险类型和输出格式要求。

Qwen 期望返回格式：

```json
{
  "is_risky": true,
  "risk_type": "violence",
  "risk_score": 0.85,
  "reason": "画面中出现真实暴力行为，并伴随尖叫声音。",
  "risk_subtype": "real_violence",
  "severity": "high",
  "confidence": 0.9,
  "content_context": "real",
  "policy_action": "reject",
  "evidence_modalities": ["visual", "audio_event"]
}
```

如果没有可用图片，`audit_with_qwen()` 不会调用 Qwen，会返回：

```json
{
  "is_risky": true,
  "risk_type": "audit_incomplete",
  "risk_score": 0.5,
  "reason": "No usable visual frame was available; fallback to manual review.",
  "segment_id": 1,
  "audit_incomplete": true
}
```

如果 Qwen 未加载，也会返回类似 `audit_incomplete`。

如果 Qwen 返回内容无法解析 JSON，也会返回 `audit_incomplete`。

### 6.11 模型结果融合

`batch_audit()` 在拿到 Qwen 结果后，依次调用：

```python
_normalize_audit_result()
_apply_text_policy_fusion()
_apply_audio_risk_fusion()
_apply_business_calibration()
```

#### `_normalize_audit_result()`

负责将模型结果规范化：

- 规范 `risk_type`。
- 限制 `risk_score` 到 0 到 1。
- 处理 `audit_incomplete`。
- 补充 `segment_id` 和 `time_range`。
- 规范 `severity`、`confidence`、`policy_action`、`evidence_modalities`。

#### `_apply_text_policy_fusion()`

负责融合文本规则命中结果。

如果标题、简介、标签、文件名或语音文本命中文本规则，会提高风险分数，并可能覆盖风险类型。

例如命中诈骗、赌博、毒品等高风险关键词时，可能直接拉高到拒绝分数。

#### `_apply_audio_risk_fusion()`

负责融合 YAMNet 风险声音。

如果 segment 有 `has_risk_sound=true`：

- 最低风险分数提高到 `0.5`。
- 如果模型原本判断正常，风险类型改为 `audio_event`。
- 通常给 `policy_action=review`。
- `reason` 中追加风险声音说明。

#### `_apply_business_calibration()`

负责业务校准。

例如：

- 游戏、影视、动画里的武器或战斗，如果没有真实伤害证据，暴力风险会被限制为人工复核，不直接拒绝。
- 零容忍类型，如诈骗、赌博、毒品、未成年人安全、自残、隐私、恐怖极端、非法交易，在分数足够时会强化为拒绝。

### 6.12 生成内部 report

调用：

```python
VideoModerationService.generate_audit_report(
    aligned_segments,
    source,
    coverage_issues=coverage_issues,
    modalities_checked=sorted(modalities_checked),
)
```

返回内部 report：

```json
{
  "video_path": "temp/requests/req-001/file-001/temp.mp4",
  "total_segments": 3,
  "risky_segments": 1,
  "risk_rate": 0.3333,
  "high_risk_count": 0,
  "medium_risk_count": 1,
  "low_risk_count": 2,
  "risk_type_distribution": {
    "audio_event": 1
  },
  "modalities_checked": ["audio", "audio_event", "vad", "visual"],
  "coverage_issues": [],
  "audit_complete": true,
  "decision": "中风险 - 建议人工复核",
  "decision_level": "medium",
  "segments": [
    {
      "segment_id": 1,
      "time": "0.00s-30.00s",
      "text_preview": "...",
      "has_risk_sound": true,
      "audit_result": {}
    }
  ]
}
```

同时保存到本地：

```text
audit_results/audit_report_YYYYMMDD_HHMMSS.json
```

决策等级：

| `decision_level` | 业务含义 |
| --- | --- |
| `high` | 拒绝 |
| `medium` | 人工复核 |
| `low` | 通过 |

如果存在 `coverage_issues` 或 segment 的 `audit_incomplete=true`，通常会使 `audit_complete=false`，最终倾向人工复核。

## 7. 最终 MQ 返回数据格式

最终返回模型：`AuditResultMessage`

由 `AuditWorkflow._build_result()` 构造，再由：

```python
AuditResultMessage.to_message_dict()
```

转换成 camelCase JSON。

### 7.1 顶层字段

| 字段 | 说明 |
| --- | --- |
| `requestId` | 请求 ID |
| `videoId` | 视频 ID |
| `auditVersion` | 审核版本 |
| `modelName` | 模型名称 |
| `modelVersion` | 模型版本 |
| `completedAt` | 完成时间，毫秒时间戳 |
| `videoDecision` | 整体审核结论 |
| `videoRiskLevel` | 整体风险等级 |
| `videoSummary` | 整体摘要 |
| `items` | 每个文件的审核结果 |

### 7.2 状态常量

文件处理状态：

| 常量 | 数值 | 说明 |
| --- | --- | --- |
| `ITEM_PROCESSING` | `1` | 处理中 |
| `ITEM_FINISHED` | `2` | 已完成 |
| `ITEM_FAIL` | `3` | 失败 |

审核结论：

| 常量 | 数值 | 说明 |
| --- | --- | --- |
| `DECISION_PASS` | `1` | 通过 |
| `DECISION_REJECT` | `2` | 拒绝 |
| `DECISION_MANUAL` | `3` | 人工复核 |

风险等级：

| 常量 | 数值 | 说明 |
| --- | --- | --- |
| `RISK_LOW` | `1` | 低风险 |
| `RISK_MEDIUM` | `2` | 中风险 |
| `RISK_HIGH` | `3` | 高风险 |

### 7.3 item 结果字段

每个 `items` 元素来自 `AuditResultItem`。

| 字段 | 说明 |
| --- | --- |
| `fileId` | 文件 ID |
| `itemStatus` | 文件处理状态 |
| `itemDecision` | 文件审核结论 |
| `riskScore` | 文件最高风险分数 |
| `riskTags` | 风险标签列表 |
| `hitSegments` | 命中的审核片段，当前实现会返回所有 segment 摘要 |
| `itemReason` | 文件审核原因摘要 |

### 7.4 hitSegments 字段

每个 segment 返回给上游的字段：

| 字段 | 说明 |
| --- | --- |
| `segmentId` | 片段 ID |
| `startSeconds` | 开始秒数 |
| `endSeconds` | 结束秒数 |
| `textPreview` | 文本预览 |
| `riskType` | 风险类型 |
| `riskScore` | 风险分数 |
| `reason` | 审核理由 |
| `isRisky` | 是否风险片段 |
| `hasRiskSound` | 是否有风险声音 |
| `bestFramePath` | 导出的最佳帧路径 |
| `sourceType` | segment 来源，如 `speech` 或 `visual_fallback` |

返回示例：

```json
{
  "requestId": "req-001",
  "videoId": "video-001",
  "auditVersion": 1,
  "modelName": "qwen-video-moderation",
  "modelVersion": "1.0.0",
  "completedAt": 1710000000000,
  "videoDecision": 3,
  "videoRiskLevel": 2,
  "videoSummary": "videoDecision=manual_review; file-001: ...",
  "items": [
    {
      "fileId": "file-001",
      "itemStatus": 2,
      "itemDecision": 3,
      "riskScore": 0.5,
      "riskTags": ["audio_event"],
      "hitSegments": [
        {
          "segmentId": 1,
          "startSeconds": 0.0,
          "endSeconds": 30.0,
          "textPreview": "",
          "riskType": "audio_event",
          "riskScore": 0.5,
          "reason": "Risk audio event detected by YAMNet...",
          "isRisky": true,
          "hasRiskSound": true,
          "bestFramePath": "audit-snapshot/video-001/1/file-001_1.jpg",
          "sourceType": "visual_fallback"
        }
      ],
      "itemReason": "中风险 - 建议人工复核; riskySegments=1; topRisk=audio_event@0.0-30.0s; modalities=audio,audio_event,visual; coverage=complete"
    }
  ]
}
```

## 8. 审核结果聚合规则

### 8.1 单文件结果

`AuditWorkflow._build_item_success()` 根据内部 report 的 `decision_level` 映射：

| `decision_level` | `itemDecision` | `riskLevel` |
| --- | --- | --- |
| `low` | `1` 通过 | `1` 低风险 |
| `medium` | `3` 人工复核 | `2` 中风险 |
| `high` | `2` 拒绝 | `3` 高风险 |

如果 `report.audit_complete=false`，文件风险分数至少会被提高到 `0.5`。

### 8.2 整体视频结果

`AuditWorkflow._aggregate_decision()` 聚合多个 item：

1. 如果任一 item 失败：整体人工复核。
2. 否则如果任一 item 拒绝：整体拒绝。
3. 否则如果任一 item 人工复核：整体人工复核。
4. 否则整体通过。

## 9. 模型和模态说明

当前涉及的模型或算法：

| 名称 | 方法 | 作用 |
| --- | --- | --- |
| ffmpeg | `video_has_audio_stream()` / `extract_audio_from_video()` | 探测视频、抽取音频 |
| WebRTC VAD | `detect_voice_segments()` | 检测人声时间段 |
| YAMNet | `detect_sound_events()` | 检测背景声音事件和风险声音 |
| Whisper | `transcribe_audio_segments()` | 语音转文字 |
| OpenCV | `extract_keyframes()` | 抽取关键帧和转场帧 |
| Qwen VL | `audit_with_qwen()` | 多模态审核判断 |

`modalities_checked` 可能包含：

| 值 | 说明 |
| --- | --- |
| `audio` | 成功抽取音频 |
| `vad` | 执行了 VAD |
| `audio_event` | 执行了 YAMNet |
| `speech_text` | 执行了 Whisper 并得到转写 |
| `visual` | 抽到了关键帧 |
| `metadata_text` | segment 中包含投稿元数据 |
| `text_policy` | 文本规则有命中 |

## 10. 可能漏检或审核不完整的情况

### 10.1 无语音但画面违规

当前 `build_audit_segments()` 已经有 `visual_fallback` 逻辑：没有转写时，会按视频时长生成视觉兜底 segment。

但仍有风险：

- 如果 `duration` 无法估算，视觉 fallback 可能生成不足。
- 如果 `frames_info` 为空，segment 即使生成，也没有可用图片。
- Qwen 审核依赖图片，没有图片会返回 `audit_incomplete`，不是完整视觉判断。

可能表现：

```text
coverage_issues = ["no_keyframes"]
risk_type = "audit_incomplete"
decision_level = "medium"
```

### 10.2 违规画面出现在未抽中的短暂帧

当前不是把视频全部帧送给 Qwen，而是抽：

- 均匀关键帧。
- 视频开头/结尾帧。
- 风险声音附近帧。
- 场景突变帧。

如果违规画面只出现很短时间，并且：

- 没有明显风险声音。
- 没有造成明显场景突变。
- 刚好不在均匀抽帧点上。

就可能没有进入 `frames_info`，最终也不会被 Qwen 看到。

### 10.3 有风险声音但没有对应可用画面

YAMNet 风险声音会通过 `_apply_audio_risk_fusion()` 融合进结果。

但是 `audit_with_qwen()` 正常需要图片。如果 segment 没有：

- `contact_sheet_path`
- `best_frame_path`
- `segment_frames[0].path`

则不会调用 Qwen，而是返回：

```text
risk_type = audit_incomplete
risk_score = 0.5
audit_incomplete = true
```

这表示系统知道审核不完整，会倾向人工复核，但不是完整的多模态判断。

### 10.4 音频中有违规语义但 VAD 没检测出来

当前 Whisper 调用条件是：

```python
if voice_segments:
    transcribe_audio_segments(...)
```

如果 VAD 没检测出人声：

- `voice_segments=[]`
- Whisper 不执行。
- 语音文本不会进入 prompt。
- 文本规则也无法扫描这部分语义。

例如低音量、背景噪声强、音乐里夹带语音、多人重叠说话，都可能导致 VAD 漏检。

### 10.5 音频抽取失败

如果 `extract_audio_from_video()` 失败，后续不会有：

- VAD
- YAMNet
- Whisper

会记录：

```text
coverage_issues = ["audio_extract_error:..."]
```

画面仍可能继续审核，但声音和语义风险可能漏掉。

### 10.6 YAMNet 未识别风险声音

YAMNet 只覆盖声音事件，不理解语义。

如果真实风险声音：

- 声音太短。
- 被背景音乐盖住。
- 类别没有命中当前风险关键词。
- 置信度低于 `sound_confidence_threshold`。

则不会标记为 `is_risk=true`，也不会形成 `focus_times` 去加强附近抽帧。

### 10.7 Whisper 转写为空或错误

即使 VAD 命中，Whisper 也可能：

- 转写为空。
- 语言识别错误。
- 漏词。
- 将敏感词识别错。

如果 `text` 为空，该段不会加入有效转写文本，文本风险可能漏掉。

### 10.8 长语音分窗导致文本定位不准

`build_audit_segments()` 对长语音按 30 秒窗口拆分。

如果 Whisper 没有词级时间戳：

- 当前窗口 `window_text` 可能为空。
- 完整转写只作为 `source_transcript_text` 上下文。
- prompt 明确要求不能只靠整段上下文判定当前窗口违规。

这样可以避免错判，但也可能导致某些语义风险定位不准。

### 10.9 Qwen 模型不可用

如果 Qwen 模型或 processor 没加载：

```text
Qwen model is not loaded
```

返回：

```json
{
  "risk_type": "audit_incomplete",
  "risk_score": 0.5,
  "audit_incomplete": true
}
```

最终通常人工复核。

### 10.10 Qwen JSON 解析失败

`audit_with_qwen()` 会用正则从模型输出中提取 JSON。

如果模型没有返回合法 JSON：

```text
Qwen JSON parse failed
```

返回 `audit_incomplete`。

这不是漏过风险，而是审核不完整，需要人工复核。

### 10.11 投稿元数据风险依赖文本规则和 Qwen

标题、简介、标签、文件名会被放入 `metadata_summary`，进入 Qwen prompt，也会被文本规则扫描。

但如果：

- 文本规则关键词没覆盖。
- Qwen 没识别。
- Qwen 审核失败。

则元数据风险可能漏判或只进入人工复核。

### 10.12 contact sheet 只包含部分帧

`_create_contact_sheet()` 默认最多使用 9 张图。

如果一个 segment 内候选帧很多，会通过 `_sample_frames()` 抽样。

这意味着即使某些帧已经被抽到，也不一定全部出现在最终送给 Qwen 的 contact sheet 里。

## 11. 常见降级结果

### 11.1 处理失败

如果单个 item 整体处理异常：

```json
{
  "itemStatus": 3,
  "itemDecision": 3,
  "riskScore": 0.5,
  "riskTags": ["processing_error"],
  "hitSegments": [],
  "itemReason": "AI processing failed, fallback to manual review: ..."
}
```

### 11.2 审核不完整

如果模型、图片、解析等环节不完整：

```json
{
  "is_risky": true,
  "risk_type": "audit_incomplete",
  "risk_score": 0.5,
  "audit_incomplete": true,
  "reason": "..."
}
```

最终通常：

```text
decision_level = medium
itemDecision = 3
```

## 12. 本地输出文件

审核过程中会产生以下文件：

| 路径 | 说明 |
| --- | --- |
| `temp/requests/{requestId}/{fileId}/temp.mp4` | 下载到本地的临时视频 |
| `temp/audio_*.wav` | 从视频抽取的音频 |
| `keyframes/{sessionId}/*.jpg` | 抽取的关键帧 |
| `keyframes/{sessionId}/segment_*_contact.jpg` | 送给 Qwen 的 contact sheet |
| `audit_results/audit_report_*.json` | 内部审核报告 |
| `audit_results/model_input_traces/...` | Qwen 输入、输出和截图快照 |
| `{storage.root_dir}/audit-snapshot/{videoId}/{auditVersion}/*.jpg` | 返回给上游可展示的最佳帧 |

## 13. 最短总结

当前 AI 审核链路可以概括为：

```text
MQ 请求
  -> 下载视频
  -> 探测音轨
  -> 抽音频
  -> VAD 找人声
  -> YAMNet 找风险声音
  -> Whisper 转写语音
  -> OpenCV 抽关键帧
  -> build_audit_segments 生成审核片段
  -> Qwen 看图 + 文本 + 声音摘要
  -> 文本规则/音频风险/业务规则融合
  -> 生成内部 report
  -> 转换成 AuditResultMessage
  -> MQ 返回结果
```

当前实现的主要风险点是：它不是逐帧审核完整视频，而是基于关键帧、语音转写和声音事件做多模态抽样审核。因此，短暂画面、VAD 漏检语音、无可用画面的风险声音、关键帧提取失败、Qwen 不可用或解析失败，都可能导致漏检或审核不完整。系统对不完整情况通常会通过 `audit_incomplete`、`coverage_issues` 和人工复核结论来兜底。
