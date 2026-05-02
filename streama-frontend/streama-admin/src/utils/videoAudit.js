const VIDEO_STATUS_META_MAP = Object.freeze({
  0: Object.freeze({ label: '转码中', tone: 'tone-neutral' }),
  1: Object.freeze({ label: '转码失败', tone: 'tone-fail' }),
  2: Object.freeze({ label: '待审核', tone: 'tone-review' }),
  3: Object.freeze({ label: '已通过', tone: 'tone-pass' }),
  4: Object.freeze({ label: '已驳回', tone: 'tone-fail' }),
})

const TASK_STATUS_META_MAP = Object.freeze({
  0: Object.freeze({ label: '排队中', tone: 'tone-neutral' }),
  1: Object.freeze({ label: '审核中', tone: 'tone-processing' }),
  2: Object.freeze({ label: '已完成', tone: 'tone-pass' }),
  3: Object.freeze({ label: '失败', tone: 'tone-fail' }),
  4: Object.freeze({ label: '已取消', tone: 'tone-neutral' }),
})

const ITEM_STATUS_META_MAP = Object.freeze({
  0: Object.freeze({ label: '排队中', tone: 'tone-neutral' }),
  1: Object.freeze({ label: '审核中', tone: 'tone-processing' }),
  2: Object.freeze({ label: '已完成', tone: 'tone-pass' }),
  3: Object.freeze({ label: '失败', tone: 'tone-fail' }),
})

const AI_DECISION_META_MAP = Object.freeze({
  1: Object.freeze({ label: '通过', tone: 'tone-pass' }),
  2: Object.freeze({ label: '驳回', tone: 'tone-fail' }),
  3: Object.freeze({ label: '人工复核', tone: 'tone-review' }),
})

const AI_RISK_META_MAP = Object.freeze({
  1: Object.freeze({ label: '低风险', tone: 'tone-pass' }),
  2: Object.freeze({ label: '中风险', tone: 'tone-review' }),
  3: Object.freeze({ label: '高风险', tone: 'tone-fail' }),
})

const TRANSFER_RESULT_META_MAP = Object.freeze({
  0: Object.freeze({ label: '转码中', tone: 'tone-neutral' }),
  1: Object.freeze({ label: '已就绪', tone: 'tone-pass' }),
  2: Object.freeze({ label: '转码失败', tone: 'tone-fail' }),
})

const UPDATE_TYPE_META_MAP = Object.freeze({
  0: Object.freeze({ label: '原有文件', tone: 'tone-neutral' }),
  1: Object.freeze({ label: '新增或更新', tone: 'tone-review' }),
})

export function normalizeText(value) {
  const text = String(value ?? '').trim()
  return text || ''
}

export function normalizeCount(value) {
  const numericValue = Number(value)
  if (!Number.isFinite(numericValue) || numericValue < 0) {
    return 0
  }
  return Math.floor(numericValue)
}

export function toFiniteNumber(value) {
  const numericValue = Number(value)
  return Number.isFinite(numericValue) ? numericValue : null
}

export function toBoolean(value) {
  if (typeof value === 'boolean') {
    return value
  }
  if (typeof value === 'string') {
    return ['1', 'true', 'yes', 'on'].includes(value.trim().toLowerCase())
  }
  return Boolean(value)
}

export function parsePossibleJson(value) {
  if (Array.isArray(value) || (value && typeof value === 'object')) {
    return value
  }

  const text = normalizeText(value)
  if (!text) {
    return null
  }

  try {
    return JSON.parse(text)
  } catch {
    return null
  }
}

function normalizeDateText(value, includeTime = false) {
  if (value === null || value === undefined || value === '') {
    return '--'
  }

  if (typeof value === 'number' || /^\d{10,}$/.test(String(value).trim())) {
    const date = new Date(Number(value))
    if (!Number.isNaN(date.getTime())) {
      return formatDateInstance(date, includeTime)
    }
  }

  const rawText = String(value).trim()
  if (!rawText) {
    return '--'
  }
  if (/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/.test(rawText)) {
    return includeTime ? rawText : rawText.slice(0, 10)
  }
  if (/^\d{4}-\d{2}-\d{2}$/.test(rawText)) {
    return rawText
  }
  if (/^\d{4}-\d{2}-\d{2}T/.test(rawText)) {
    const normalized = rawText.replace('T', ' ').replace(/\.\d+$/, '').replace(/Z$/, '')
    return includeTime ? normalized.slice(0, 19) : normalized.slice(0, 10)
  }

  const parsedDate = new Date(rawText)
  if (Number.isNaN(parsedDate.getTime())) {
    return includeTime ? rawText : rawText.split(' ')[0] || rawText
  }
  return formatDateInstance(parsedDate, includeTime)
}

function formatDateInstance(date, includeTime = false) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  if (!includeTime) {
    return `${year}-${month}-${day}`
  }
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

export function formatDate(value) {
  return normalizeDateText(value, false)
}

export function formatDateTime(value) {
  return normalizeDateText(value, true)
}

export function formatDuration(value) {
  const totalSeconds = Number(value)
  if (!Number.isFinite(totalSeconds) || totalSeconds < 0) {
    return '--'
  }

  const safeValue = Math.floor(totalSeconds)
  const hours = Math.floor(safeValue / 3600)
  const minutes = Math.floor((safeValue % 3600) / 60)
  const seconds = safeValue % 60
  if (hours > 0) {
    return `${hours}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
  }
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
}

export function formatScore(value) {
  const numericValue = Number(value)
  return Number.isFinite(numericValue) ? numericValue.toFixed(2) : '--'
}

export function parseClockToSeconds(value) {
  const text = normalizeText(value)
  if (!text) {
    return null
  }

  const parts = text.split(':').map((part) => Number(part))
  if (parts.some((part) => !Number.isFinite(part))) {
    return null
  }

  if (parts.length === 3) {
    return parts[0] * 3600 + parts[1] * 60 + parts[2]
  }
  if (parts.length === 2) {
    return parts[0] * 60 + parts[1]
  }
  if (parts.length === 1) {
    return parts[0]
  }
  return null
}

export function toAdminFileResourceUrl(path) {
  const source = normalizeText(path)
  if (!source) {
    return ''
  }
  if (/^(https?:\/\/|data:)/i.test(source)) {
    return source
  }
  return `/admin/file/getResource?sourceName=${encodeURIComponent(source.replace(/^\/+/, ''))}`
}

export function getVideoStatusMeta(statusValue, fallbackLabel = '') {
  const statusMeta = VIDEO_STATUS_META_MAP[Number(statusValue)]
  if (statusMeta) {
    return statusMeta
  }
  const label = normalizeText(fallbackLabel)
  if (label) {
    return { label, tone: 'tone-neutral' }
  }
  return { label: '未知状态', tone: 'tone-neutral' }
}

export function getTaskStatusMeta(value) {
  return TASK_STATUS_META_MAP[Number(value)] || null
}

export function getItemStatusMeta(value) {
  return ITEM_STATUS_META_MAP[Number(value)] || null
}

export function getAiDecisionMeta(value) {
  return AI_DECISION_META_MAP[Number(value)] || null
}

export function getAiRiskMeta(value) {
  return AI_RISK_META_MAP[Number(value)] || null
}

export function getTransferResultMeta(value) {
  return TRANSFER_RESULT_META_MAP[Number(value)] || null
}

export function getUpdateTypeMeta(value) {
  return UPDATE_TYPE_META_MAP[Number(value)] || UPDATE_TYPE_META_MAP[0]
}

export function canManualAudit(post) {
  return Number(post?.status) === 2
}

export function normalizeAiSummary(summary) {
  if (!summary || typeof summary !== 'object') {
    return null
  }

  const modelName = normalizeText(summary.modelName)
  const modelVersion = normalizeText(summary.modelVersion)
  return {
    taskStatus: summary.taskStatus ?? null,
    taskStatusMeta: getTaskStatusMeta(summary.taskStatus),
    aiDecision: summary.aiDecision ?? null,
    aiDecisionMeta: getAiDecisionMeta(summary.aiDecision),
    aiRiskLevel: summary.aiRiskLevel ?? null,
    aiRiskMeta: getAiRiskMeta(summary.aiRiskLevel),
    aiSummaryText: normalizeText(summary.aiSummary),
    auditVersionText: summary.auditVersion ? `V${summary.auditVersion}` : '--',
    modelText: [modelName, modelVersion].filter(Boolean).join(' / ') || '--',
    completedAtText: formatDateTime(summary.completedAt),
    triggerTimeText: formatDateTime(summary.triggerTime),
    lastError: normalizeText(summary.lastError),
  }
}

function normalizeRiskTags(value) {
  const parsedValue = parsePossibleJson(value)
  const list = Array.isArray(parsedValue) ? parsedValue : parsedValue ? [parsedValue] : []
  return list
    .map((item) => {
      if (typeof item === 'string') {
        return item.trim()
      }
      if (item && typeof item === 'object') {
        const label = item.label || item.name || item.value || item.riskType
        return normalizeText(label) || JSON.stringify(item)
      }
      return normalizeText(item)
    })
    .filter(Boolean)
}

function extractSortValueFromTimeText(value) {
  const text = normalizeText(value)
  if (!text) {
    return null
  }
  const match = text.match(/\d+(?::\d+){0,2}(?:\.\d+)?/)
  return match ? parseClockToSeconds(match[0]) : null
}

function buildSegmentTimeText(startSeconds, endSeconds, legacyTimeText) {
  if (startSeconds !== null && endSeconds !== null && endSeconds > startSeconds) {
    return `${formatDuration(startSeconds)} - ${formatDuration(endSeconds)}`
  }
  if (startSeconds !== null) {
    return formatDuration(startSeconds)
  }
  return legacyTimeText || '--'
}

const NORMAL_RISK_TYPES = new Set(['', 'none', 'normal', 'safe', 'pass', '无', '正常'])

const RISK_TYPE_LABEL_MAP = Object.freeze({
  normal: '正常',
  none: '正常',
  violence: '暴力',
  sexual: '色情/低俗',
  political: '涉政',
  other: '其他风险',
  audio_event: '风险音频',
  audit_incomplete: '审核不完整',
  unknown: '未知',
  error: '审核异常',
})

function normalizeSegmentRiskType(value) {
  const riskType = normalizeText(value)
  if (!riskType || riskType.toLowerCase() === 'none' || riskType === '无') {
    return 'normal'
  }
  return riskType
}

function getSegmentRiskTypeText(riskType) {
  const key = normalizeText(riskType).toLowerCase()
  return RISK_TYPE_LABEL_MAP[key] || normalizeText(riskType) || '正常'
}

function getSegmentIsRisky(segment, riskType, riskScore) {
  if (segment.isRisky !== undefined || segment.is_risky !== undefined) {
    return toBoolean(segment.isRisky ?? segment.is_risky)
  }

  const normalizedRiskType = normalizeText(riskType).toLowerCase()
  if (!NORMAL_RISK_TYPES.has(normalizedRiskType) && normalizedRiskType !== 'unknown') {
    return true
  }
  return riskScore !== null && riskScore > 0.3
}

function getSegmentStatusMeta(isRisky, riskScore) {
  if (!isRisky) {
    return { text: '正常', tone: 'tone-pass' }
  }
  if (riskScore !== null && riskScore >= 0.8) {
    return { text: '高风险', tone: 'tone-fail' }
  }
  return { text: '需复核', tone: 'tone-review' }
}

function normalizeHitSegment(segment, index, fileKey) {
  if (!segment || typeof segment !== 'object') {
    return null
  }

  const startSeconds = toFiniteNumber(segment.startSeconds ?? segment.start)
  const endSeconds = toFiniteNumber(segment.endSeconds ?? segment.end)
  const legacyTimeText = normalizeText(segment.time)
  const seekSeconds = startSeconds ?? extractSortValueFromTimeText(legacyTimeText)
  const bestFramePath = normalizeText(segment.bestFramePath || segment.best_frame_path)
  const riskType = normalizeSegmentRiskType(segment.riskType || segment.type || segment.risk_tag)
  const riskScore = toFiniteNumber(segment.riskScore ?? segment.score)
  const isRisky = getSegmentIsRisky(segment, riskType, riskScore)
  const statusMeta = getSegmentStatusMeta(isRisky, riskScore)

  return {
    segmentId: segment.segmentId ?? segment.segment_id ?? index + 1,
    segmentKey: `${fileKey}-${segment.segmentId ?? segment.segment_id ?? index + 1}`,
    startSeconds,
    endSeconds,
    seekSeconds,
    timeText: buildSegmentTimeText(startSeconds, endSeconds, legacyTimeText),
    textPreview: normalizeText(segment.textPreview || segment.text || segment.preview || segment.ocrText),
    riskType,
    riskTypeText: getSegmentRiskTypeText(riskType),
    riskScore,
    riskScoreText: formatScore(segment.riskScore ?? segment.score),
    reason: normalizeText(segment.reason || segment.description),
    isRisky,
    statusText: statusMeta.text,
    statusTone: statusMeta.tone,
    hasRiskSound: toBoolean(segment.hasRiskSound),
    sourceType: normalizeText(segment.sourceType || segment.source_type),
    imageUrl: bestFramePath ? toAdminFileResourceUrl(bestFramePath) : '',
    imageBroken: false,
    sortValue: seekSeconds ?? Number.MAX_SAFE_INTEGER,
  }
}

function normalizeHitSegments(value, fileKey) {
  const parsedValue = parsePossibleJson(value)
  const list = Array.isArray(parsedValue) ? parsedValue : parsedValue ? [parsedValue] : []
  return list
    .map((segment, index) => normalizeHitSegment(segment, index, fileKey))
    .filter(Boolean)
    .sort((left, right) => {
      if (left.sortValue !== right.sortValue) {
        return left.sortValue - right.sortValue
      }
      return (right.riskScore || 0) - (left.riskScore || 0)
    })
}

function normalizeAiItem(item, index) {
  if (!item || typeof item !== 'object') {
    return null
  }

  const fileIndex = Number.isFinite(Number(item.fileIndex)) ? Number(item.fileIndex) : index + 1
  const fileId = normalizeText(item.fileId)
  const fileKey = fileId || `file-${fileIndex}`
  return {
    fileId,
    fileIndex,
    fileLabel: `P${fileIndex}`,
    fileName: normalizeText(item.fileName) || `文件 ${fileIndex}`,
    uploadId: normalizeText(item.uploadId),
    duration: toFiniteNumber(item.duration),
    durationText: formatDuration(item.duration),
    updateType: item.updateType ?? null,
    updateTypeMeta: getUpdateTypeMeta(item.updateType),
    itemStatus: item.itemStatus ?? null,
    statusMeta: getItemStatusMeta(item.itemStatus),
    itemDecision: item.itemDecision ?? null,
    decisionMeta: getAiDecisionMeta(item.itemDecision),
    riskScore: toFiniteNumber(item.riskScore),
    riskScoreText: formatScore(item.riskScore),
    riskTags: normalizeRiskTags(item.riskTagsJson ?? item.riskTags),
    hitSegments: normalizeHitSegments(item.hitSegmentsJson ?? item.hitSegments, fileKey),
    itemReason: normalizeText(item.itemReason),
  }
}

export function normalizeAiItems(items) {
  if (!Array.isArray(items)) {
    return []
  }

  return items
    .map((item, index) => normalizeAiItem(item, index))
    .filter(Boolean)
    .sort((left, right) => left.fileIndex - right.fileIndex)
}

function buildAiAvailabilityMeta(filePost, aiItem) {
  if (aiItem?.statusMeta) {
    return aiItem.statusMeta
  }

  const transferResult = Number(filePost?.transferResult)
  if (transferResult === 0) {
    return { label: '等待转码', tone: 'tone-neutral' }
  }
  if (transferResult === 2) {
    return { label: '转码失败', tone: 'tone-fail' }
  }
  if (Number(filePost?.updateType) === 1) {
    return { label: '待 AI 审核', tone: 'tone-processing' }
  }
  return { label: '未参与本轮 AI 审核', tone: 'tone-neutral' }
}

export function normalizeFilePosts(filePosts, aiItems = []) {
  const itemMap = new Map(
    normalizeAiItems(aiItems)
      .filter((item) => item.fileId)
      .map((item) => [item.fileId, item]),
  )

  if (!Array.isArray(filePosts)) {
    return []
  }

  return filePosts
    .map((filePost, index) => {
      if (!filePost || typeof filePost !== 'object') {
        return null
      }

      const fileIndex = Number.isFinite(Number(filePost.fileIndex)) ? Number(filePost.fileIndex) : index + 1
      const fileId = normalizeText(filePost.fileId)
      const aiItem = itemMap.get(fileId) || null
      return {
        fileId,
        uploadId: normalizeText(filePost.uploadId),
        fileIndex,
        fileLabel: `P${fileIndex}`,
        fileName: normalizeText(filePost.fileName) || `文件 ${fileIndex}`,
        duration: toFiniteNumber(filePost.duration),
        durationText: formatDuration(filePost.duration),
        transferResult: filePost.transferResult ?? null,
        transferResultMeta: getTransferResultMeta(filePost.transferResult),
        updateType: filePost.updateType ?? null,
        updateTypeMeta: getUpdateTypeMeta(filePost.updateType),
        isUpdatedFile: Number(filePost.updateType) === 1,
        aiItem,
        aiAvailabilityMeta: buildAiAvailabilityMeta(filePost, aiItem),
        aiDecisionMeta: aiItem?.decisionMeta || null,
        riskScore: aiItem?.riskScore ?? null,
        riskScoreText: aiItem?.riskScoreText || '--',
        riskTags: aiItem?.riskTags || [],
        hitSegments: aiItem?.hitSegments || [],
        itemReason: aiItem?.itemReason || '',
        hasAiAudit: Boolean(aiItem),
      }
    })
    .filter(Boolean)
    .sort((left, right) => left.fileIndex - right.fileIndex)
}
