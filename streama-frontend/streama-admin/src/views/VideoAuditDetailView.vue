<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import AuditVideoPlayer from '@/components/video-audit/AuditVideoPlayer.vue'
import { auditVideo, getAiAuditItems, getAiAuditSummary, getVideoPostDetail } from '@/api/videoInfo'
import {
  canManualAudit,
  formatDateTime,
  getAiDecisionMeta,
  getAiRiskMeta,
  getVideoStatusMeta,
  normalizeAiSummary,
  normalizeCount,
  normalizeFilePosts,
  normalizeText,
  toAdminFileResourceUrl,
} from '@/utils/videoAudit'

const DEFAULT_STATUS = '2'
const DEFAULT_PAGE_NO = 1
const DEFAULT_PAGE_SIZE = 15

const route = useRoute()
const router = useRouter()
const playerRef = ref(null)

const detailLoading = ref(false)
const detailError = ref('')
const aiSummaryError = ref('')
const aiItemsError = ref('')
const playerRuntimeError = ref('')
const submittingAuditStatus = ref(0)
const postDetail = ref(null)
const aiSummary = ref(null)
const fileList = ref([])
const activeFileId = ref('')
const selectedSegmentKey = ref('')
const evidenceViewerVisible = ref(false)
const evidenceViewerSegment = ref(null)

let activeRequestToken = 0

const currentVideoId = computed(() => normalizeText(route.params.videoId))

const backQuery = computed(() => {
  const status = normalizeText(route.query.status) || DEFAULT_STATUS
  const pageNo = normalizePositiveInteger(route.query.pageNo, DEFAULT_PAGE_NO)
  const pageSize = normalizePositiveInteger(route.query.pageSize, DEFAULT_PAGE_SIZE)
  return {
    status,
    pageNo: String(pageNo),
    pageSize: String(pageSize),
  }
})

const currentFile = computed(() => {
  return fileList.value.find((item) => item.fileId === activeFileId.value) || fileList.value[0] || null
})

const currentSegments = computed(() => currentFile.value?.hitSegments || [])

const totalHitSegments = computed(() => {
  return fileList.value.reduce((total, file) => total + (file.hitSegmentCount || 0), 0)
})

const evidenceViewerImages = computed(() => {
  return (evidenceViewerSegment.value?.imageItems || []).filter((image) => !image.broken)
})

const displayAiSummary = computed(() => {
  return aiSummary.value || createFallbackSummary(postDetail.value)
})

const hasAiSummaryData = computed(() => {
  const summary = displayAiSummary.value
  if (!summary) {
    return false
  }
  return Boolean(
    summary.taskStatusMeta ||
      summary.aiDecisionMeta ||
      summary.aiRiskMeta ||
      summary.aiSummaryText ||
      summary.lastError,
  )
})

const canSubmitAudit = computed(() => {
  return canManualAudit(postDetail.value) && !detailLoading.value && !submittingAuditStatus.value
})

watch(
  () => currentVideoId.value,
  () => {
    loadDetailData()
  },
  { immediate: true },
)

watch(
  () => route.query.fileId,
  () => {
    syncActiveFileFromRoute(true)
  },
)

watch(
  () => currentFile.value,
  (file) => {
    playerRuntimeError.value = ''
    if (!file) {
      selectedSegmentKey.value = ''
      return
    }
    if (!file.hitSegments.some((segment) => segment.segmentKey === selectedSegmentKey.value)) {
      selectedSegmentKey.value = file.hitSegments[0]?.segmentKey || ''
    }
  },
  { immediate: true },
)

function normalizePositiveInteger(value, fallbackValue) {
  const numericValue = Number(value)
  if (!Number.isInteger(numericValue) || numericValue <= 0) {
    return fallbackValue
  }
  return numericValue
}

function normalizeDetailPost(videoInfo = {}) {
  const videoName = normalizeText(videoInfo.videoName) || '未命名稿件'
  const nickName = normalizeText(videoInfo.nickName) || normalizeText(videoInfo.userId) || '未知作者'

  return {
    ...videoInfo,
    videoId: normalizeText(videoInfo.videoId),
    videoName,
    userId: normalizeText(videoInfo.userId),
    nickName,
    avatarUrl: toAdminFileResourceUrl(videoInfo.avatar),
    videoCoverUrl: toAdminFileResourceUrl(videoInfo.videoCover),
    createTimeText: formatDateTime(videoInfo.createTime),
    lastUpdateTimeText: formatDateTime(videoInfo.lastUpdateTime),
    tagsText: normalizeText(videoInfo.tags),
    introductionText: normalizeText(videoInfo.introduction),
    status: videoInfo.status ?? null,
    statusMeta: getVideoStatusMeta(videoInfo.status, videoInfo.statusName),
    playCount: normalizeCount(videoInfo.playCount),
    likeCount: normalizeCount(videoInfo.likeCount),
    commentCount: normalizeCount(videoInfo.commentCount),
    danmuCount: normalizeCount(videoInfo.danmuCount),
    coinCount: normalizeCount(videoInfo.coinCount),
    collectCount: normalizeCount(videoInfo.collectCount),
    aiDecision: videoInfo.aiDecision ?? null,
    aiDecisionMeta: getAiDecisionMeta(videoInfo.aiDecision),
    aiRiskLevel: videoInfo.aiRiskLevel ?? null,
    aiRiskMeta: getAiRiskMeta(videoInfo.aiRiskLevel),
    aiSummaryText: normalizeText(videoInfo.aiSummary),
  }
}

function createFallbackSummary(post) {
  if (!post) {
    return null
  }
  if (!post.aiDecisionMeta && !post.aiRiskMeta && !post.aiSummaryText) {
    return null
  }
  return {
    taskStatusMeta: null,
    aiDecisionMeta: post.aiDecisionMeta || null,
    aiRiskMeta: post.aiRiskMeta || null,
    aiSummaryText: post.aiSummaryText || '',
    auditVersionText: '--',
    modelText: '--',
    completedAtText: '--',
    triggerTimeText: '--',
    lastError: '',
  }
}

function getRequestErrorMessage(error, fallbackMessage) {
  const message = normalizeText(error?.message || error?.info || error?.payload?.info)
  return message || fallbackMessage
}

function syncActiveFileFromRoute(shouldSyncRoute) {
  const routeFileId = normalizeText(route.query.fileId)
  const matchedFile = fileList.value.find((item) => item.fileId === routeFileId)
  const nextFileId = matchedFile?.fileId || fileList.value[0]?.fileId || ''

  if (nextFileId && nextFileId !== activeFileId.value) {
    activeFileId.value = nextFileId
  }

  if (!nextFileId) {
    activeFileId.value = ''
    return
  }

  if (shouldSyncRoute && nextFileId !== routeFileId) {
    replaceCurrentFileQuery(nextFileId)
  }
}

async function loadDetailData() {
  const videoId = currentVideoId.value
  if (!videoId) {
    detailError.value = '缺少稿件 ID'
    postDetail.value = null
    aiSummary.value = null
    fileList.value = []
    return
  }

  const requestToken = ++activeRequestToken
  detailLoading.value = true
  detailError.value = ''
  aiSummaryError.value = ''
  aiItemsError.value = ''
  playerRuntimeError.value = ''
  postDetail.value = null
  aiSummary.value = null
  fileList.value = []
  activeFileId.value = ''
  selectedSegmentKey.value = ''

  const [detailResult, summaryResult, itemsResult] = await Promise.allSettled([
    getVideoPostDetail(videoId),
    getAiAuditSummary(videoId),
    getAiAuditItems(videoId),
  ])

  if (requestToken !== activeRequestToken) {
    return
  }

  if (detailResult.status !== 'fulfilled') {
    detailLoading.value = false
    detailError.value = getRequestErrorMessage(detailResult.reason, '加载稿件详情失败')
    return
  }

  const detailData = detailResult.value || {}
  const normalizedPost = normalizeDetailPost(detailData.videoInfo || {})

  if (!normalizedPost.videoId) {
    detailLoading.value = false
    detailError.value = '未找到该稿件'
    return
  }

  if (summaryResult.status === 'fulfilled') {
    aiSummary.value = normalizeAiSummary(summaryResult.value)
  } else {
    aiSummary.value = null
    aiSummaryError.value = getRequestErrorMessage(summaryResult.reason, 'AI 审核概览暂时不可用')
  }

  if (itemsResult.status !== 'fulfilled') {
    aiItemsError.value = getRequestErrorMessage(itemsResult.reason, 'AI 文件审核结果暂时不可用')
  }

  postDetail.value = normalizedPost
  fileList.value = normalizeFilePosts(
    detailData.videoInfoFilePosts,
    itemsResult.status === 'fulfilled' ? itemsResult.value : [],
  )
  syncActiveFileFromRoute(true)
  detailLoading.value = false
}

function replaceCurrentFileQuery(fileId) {
  const nextFileId = normalizeText(fileId)
  router.replace({
    name: 'video-audit-detail',
    params: {
      videoId: currentVideoId.value,
    },
    query: nextFileId
      ? { ...backQuery.value, fileId: nextFileId }
      : { ...backQuery.value },
  })
}

function backToList() {
  router.push({
    name: 'video-manage',
    query: { ...backQuery.value },
  })
}

function handleSelectFile(file) {
  if (!file?.fileId || file.fileId === activeFileId.value) {
    return
  }
  activeFileId.value = file.fileId
  selectedSegmentKey.value = file.hitSegments[0]?.segmentKey || ''
  replaceCurrentFileQuery(file.fileId)
}

function handleSelectSegment(segment) {
  if (!segment) {
    return
  }
  selectedSegmentKey.value = segment.segmentKey
  playerRef.value?.seekTo(segment.seekSeconds ?? segment.startSeconds ?? 0)
}

function handleSegmentKeydown(segment) {
  handleSelectSegment(segment)
}

function handleMarkerSelect(segment) {
  if (!segment) {
    return
  }
  selectedSegmentKey.value = segment.segmentKey
}

function handlePlayerError(message) {
  playerRuntimeError.value = normalizeText(message)
}

function hasVisibleSegmentImages(segment) {
  return Boolean(segment?.imageItems?.some((image) => !image.broken))
}

function getVisibleSegmentImageCount(segment) {
  return segment?.imageItems?.filter((image) => !image.broken).length || 0
}

function openEvidenceViewer(segment) {
  if (!hasVisibleSegmentImages(segment)) {
    return
  }
  evidenceViewerSegment.value = segment
  evidenceViewerVisible.value = true
}

function markSegmentImageBroken(image) {
  if (image) {
    image.broken = true
  }
}

async function submitAudit(status) {
  if (!postDetail.value?.videoId || !canSubmitAudit.value) {
    return
  }

  submittingAuditStatus.value = status
  try {
    await auditVideo({
      videoId: postDetail.value.videoId,
      status,
    })
    ElMessage.success(status === 3 ? '稿件审核通过' : '稿件已驳回')
    backToList()
  } catch (error) {
    ElMessage.error(getRequestErrorMessage(error, '提交人工审核结果失败'))
  } finally {
    submittingAuditStatus.value = 0
  }
}
</script>

<template>
  <div class="audit-detail-page">
    <div class="page-head">
      <div class="page-copy">
        <el-button link class="back-btn" @click="backToList">
          返回稿件列表
        </el-button>
        <h2>稿件审核</h2>
        <p>按文件查看稿件内容、AI 审核结果和命中证据，最后由管理员给出人工审核结论。</p>
      </div>

      <span v-if="postDetail" class="meta-pill" :class="postDetail.statusMeta.tone">
        {{ postDetail.statusMeta.label }}
      </span>
    </div>

    <el-alert
      v-if="detailError"
      type="error"
      show-icon
      :closable="false"
      :title="detailError"
    />

    <div v-else-if="detailLoading" class="loading-shell">
      <div class="loading-card">稿件详情加载中...</div>
    </div>

    <template v-else-if="postDetail">
      <section class="overview-card">
        <div class="cover-wrap">
          <img
            v-if="postDetail.videoCoverUrl"
            :src="postDetail.videoCoverUrl"
            alt="稿件封面"
            class="video-cover"
          />
          <div v-else class="video-cover empty">暂无封面</div>
        </div>

        <div class="overview-main">
          <div class="title-row">
            <div class="title-copy">
              <h3>{{ postDetail.videoName }}</h3>
              <p>作者：{{ postDetail.nickName }} | 用户 ID：{{ postDetail.userId || '--' }}</p>
            </div>

            <span class="meta-pill" :class="postDetail.statusMeta.tone">
              {{ postDetail.statusMeta.label }}
            </span>
          </div>

          <div class="meta-grid">
            <article class="meta-card">
              <span class="meta-label">投稿时间</span>
              <strong>{{ postDetail.createTimeText }}</strong>
            </article>
            <article class="meta-card">
              <span class="meta-label">更新时间</span>
              <strong>{{ postDetail.lastUpdateTimeText }}</strong>
            </article>
            <article class="meta-card">
              <span class="meta-label">播放 / 评论</span>
              <strong>{{ postDetail.playCount }} / {{ postDetail.commentCount }}</strong>
            </article>
            <article class="meta-card">
              <span class="meta-label">点赞 / 收藏</span>
              <strong>{{ postDetail.likeCount }} / {{ postDetail.collectCount }}</strong>
            </article>
          </div>

          <div v-if="postDetail.tagsText" class="copy-block">
            <span class="meta-label">标签</span>
            <p>{{ postDetail.tagsText }}</p>
          </div>

          <div v-if="postDetail.introductionText" class="copy-block">
            <span class="meta-label">简介</span>
            <p>{{ postDetail.introductionText }}</p>
          </div>
        </div>
      </section>

      <section class="summary-card">
        <div class="section-head summary-head-clean">
          <div>
            <h3>AI 审核建议</h3>
            <p>聚焦结论、风险和证据，辅助人工快速复核。</p>
          </div>
          <span class="section-chip">{{ totalHitSegments }} 个风险片段</span>
        </div>

        <div class="notice-stack">
          <el-alert
            v-if="aiSummaryError"
            type="warning"
            show-icon
            :closable="false"
            :title="aiSummaryError"
          />
          <el-alert
            v-if="aiItemsError"
            type="warning"
            show-icon
            :closable="false"
            :title="aiItemsError"
          />
        </div>

        <template v-if="hasAiSummaryData">
          <div class="summary-grid">
            <article class="meta-card">
              <span class="meta-label">AI 建议</span>
              <span
                v-if="displayAiSummary?.aiDecisionMeta"
                class="meta-pill"
                :class="displayAiSummary.aiDecisionMeta.tone"
              >
                {{ displayAiSummary.aiDecisionMeta.label }}
              </span>
              <strong v-else>--</strong>
            </article>
            <article class="meta-card">
              <span class="meta-label">风险等级</span>
              <span
                v-if="displayAiSummary?.aiRiskMeta"
                class="meta-pill"
                :class="displayAiSummary.aiRiskMeta.tone"
              >
                {{ displayAiSummary.aiRiskMeta.label }}
              </span>
              <strong v-else>--</strong>
            </article>
            <article class="meta-card">
              <span class="meta-label">风险片段</span>
              <strong>{{ totalHitSegments }}</strong>
            </article>
            <article class="meta-card">
              <span class="meta-label">完成状态</span>
              <span
                v-if="displayAiSummary?.taskStatusMeta"
                class="meta-pill"
                :class="displayAiSummary.taskStatusMeta.tone"
              >
                {{ displayAiSummary.taskStatusMeta.label }}
              </span>
              <strong v-else>--</strong>
            </article>
          </div>

          <details class="copy-block summary-copy technical-details">
            <summary>AI 详情</summary>
            <span class="meta-label">AI 摘要</span>
            <p>{{ displayAiSummary?.aiSummaryText || '当前稿件暂无 AI 审核摘要。' }}</p>
          </details>

          <el-alert
            v-if="displayAiSummary?.lastError"
            type="error"
            show-icon
            :closable="false"
            :title="displayAiSummary.lastError"
          />
        </template>

        <div v-else class="empty-state compact-empty">
          当前稿件暂无 AI 审核概览。
        </div>
      </section>

      <section class="review-layout">
        <div class="player-column">
          <div class="panel">
            <div class="section-head compact">
              <div>
                <h3>播放预览</h3>
                <p v-if="currentFile">{{ currentFile.fileLabel }} | {{ currentFile.fileName }}</p>
                <p v-else>当前文件暂无可播放内容。</p>
              </div>

              <div v-if="currentFile" class="file-badges">
                <span
                  v-if="currentFile.hasPendingUpdateAudit"
                  class="meta-pill"
                  :class="currentFile.updateTypeMeta.tone"
                >
                  {{ currentFile.updateTypeMeta.label }}
                </span>
                <span
                  v-if="currentFile.hasTransferIssue && currentFile.transferResultMeta"
                  class="meta-pill"
                  :class="currentFile.transferResultMeta.tone"
                >
                  {{ currentFile.transferResultMeta.label }}
                </span>
                <span class="meta-pill" :class="currentFile.aiAvailabilityMeta.tone">
                  {{ currentFile.aiAvailabilityMeta.label }}
                </span>
              </div>
            </div>

            <AuditVideoPlayer
              ref="playerRef"
              :file-id="currentFile?.fileId || ''"
              :markers="currentSegments"
              :active-marker-key="selectedSegmentKey"
              :fallback-duration="currentFile?.duration || 0"
              @marker-select="handleMarkerSelect"
              @error="handlePlayerError"
            />

            <el-alert
              v-if="playerRuntimeError"
              type="warning"
              show-icon
              :closable="false"
              :title="playerRuntimeError"
            />
          </div>
        </div>

        <aside class="side-column">
          <section class="panel">
            <div class="section-head compact">
              <div>
                <h3>稿件文件</h3>
                <p>共 {{ fileList.length }} 个文件</p>
              </div>
            </div>

            <div v-if="fileList.length > 0" class="file-list">
              <button
                v-for="file in fileList"
                :key="file.fileId || file.fileLabel"
                type="button"
                class="file-item"
                :class="{ active: file.fileId === activeFileId }"
                @click="handleSelectFile(file)"
              >
                <div class="file-item-head">
                  <span class="file-label">{{ file.fileLabel }}</span>
                  <span v-if="file.fileId === activeFileId" class="playing-label">当前查看</span>
                </div>

                <p class="file-name" :title="file.fileName">{{ file.fileName }}</p>
                <p class="file-meta">时长 {{ file.durationText }}</p>

                <div class="file-badges">
                  <span
                    v-if="file.hasPendingUpdateAudit"
                    class="mini-pill"
                    :class="file.updateTypeMeta.tone"
                  >
                    {{ file.updateTypeMeta.label }}
                  </span>
                  <span
                    v-if="file.hasTransferIssue && file.transferResultMeta"
                    class="mini-pill"
                    :class="file.transferResultMeta.tone"
                  >
                    {{ file.transferResultMeta.label }}
                  </span>
                  <span class="mini-pill" :class="file.aiAvailabilityMeta.tone">
                    {{ file.aiAvailabilityMeta.label }}
                  </span>
                  <span
                    v-if="file.aiDecisionMeta"
                    class="mini-pill"
                    :class="file.aiDecisionMeta.tone"
                  >
                    {{ file.aiDecisionMeta.label }}
                  </span>
                </div>
              </button>
            </div>

            <div v-else class="empty-state">
              当前稿件未返回任何文件信息。
            </div>
          </section>

          <section v-if="currentFile" class="panel">
            <div class="section-head compact">
              <div>
                <h3>{{ currentFile.fileLabel }} AI 审核结果</h3>
                <p>{{ currentFile.fileName }}</p>
              </div>
            </div>

            <div class="detail-metrics">
              <article v-if="!currentFile.hasAiAudit" class="meta-card">
                <span class="meta-label">AI 状态</span>
                <span class="meta-pill" :class="currentFile.aiAvailabilityMeta.tone">
                  {{ currentFile.aiAvailabilityMeta.label }}
                </span>
              </article>
              <article class="meta-card">
                <span class="meta-label">AI 建议</span>
                <span
                  v-if="currentFile.aiDecisionMeta"
                  class="meta-pill"
                  :class="currentFile.aiDecisionMeta.tone"
                >
                  {{ currentFile.aiDecisionMeta.label }}
                </span>
                <strong v-else>--</strong>
              </article>
              <article class="meta-card">
                <span class="meta-label">风险分数</span>
                <strong>{{ currentFile.riskScoreText }}</strong>
              </article>
              <article class="meta-card">
                <span class="meta-label">片段数量</span>
                <strong>{{ currentFile.hitSegmentCount }}</strong>
              </article>
            </div>

            <el-alert
              v-if="!currentFile.hasAiAudit && !currentFile.isUpdatedFile"
              type="info"
              show-icon
              :closable="false"
              title="该原有文件未参与本轮 AI 审核。"
            />
            <el-alert
              v-else-if="!currentFile.hasAiAudit && currentFile.isUpdatedFile"
              type="warning"
              show-icon
              :closable="false"
              title="该新增或更新文件暂未返回 AI 审核结果。"
            />

            <div v-if="currentFile.riskTags.length > 0" class="tag-list">
              <span
                v-for="tag in currentFile.riskTags"
                :key="`${currentFile.fileId}-${tag}`"
                class="tag-item"
              >
                {{ tag }}
              </span>
            </div>

            <details v-if="currentFile.itemReason" class="copy-block technical-details">
              <summary>文件级说明</summary>
              <span class="meta-label">AI 说明</span>
              <p>{{ currentFile.itemReason || '暂无文件级 AI 说明。' }}</p>
            </details>
          </section>
        </aside>
      </section>

      <section class="panel evidence-panel">
        <div class="section-head evidence-head-clean">
          <div>
            <h3>风险片段</h3>
            <p>点击片段跳转视频，横向查看多帧证据。</p>
          </div>
          <span class="section-chip">{{ currentSegments.length }} 条</span>
        </div>

        <div v-if="currentSegments.length > 0" class="segment-list">
          <div
            v-for="segment in currentSegments"
            :key="segment.segmentKey"
            class="segment-card"
            :class="{
              active: selectedSegmentKey === segment.segmentKey,
              risky: segment.isRisky,
              normal: !segment.isRisky,
            }"
            role="button"
            tabindex="0"
            @click="handleSelectSegment(segment)"
            @keydown.enter.prevent="handleSegmentKeydown(segment)"
            @keydown.space.prevent="handleSegmentKeydown(segment)"
          >
            <div class="segment-head">
              <div class="segment-copy">
                <p class="segment-title">{{ segment.timeText }}</p>
                <p class="segment-subtitle">{{ segment.riskTypeText }} | 分数 {{ segment.riskScoreText }}</p>
              </div>
              <span class="mini-pill" :class="segment.statusTone">{{ segment.statusText }}</span>
            </div>

            <p v-if="segment.hasReason" class="segment-reason segment-reason-clean" :title="segment.reason">
              {{ segment.reasonPreview }}
            </p>
            <p v-if="segment.hasTextPreview" class="segment-preview segment-preview-clean">{{ segment.textPreview }}</p>
            <div
              v-if="segment.showAudioRiskTag || hasVisibleSegmentImages(segment)"
              class="segment-evidence-tags"
            >
              <span v-if="segment.showAudioRiskTag" class="mini-pill tone-review">音频命中</span>
              <button
                v-if="hasVisibleSegmentImages(segment)"
                type="button"
                class="segment-evidence-trigger"
                @click.stop="openEvidenceViewer(segment)"
              >
                证据图 {{ getVisibleSegmentImageCount(segment) }} 帧
              </button>
            </div>
          </div>
        </div>

        <div v-else class="empty-state">
          当前文件暂无 AI 审核片段。
        </div>
      </section>

      <el-dialog
        v-model="evidenceViewerVisible"
        class="evidence-viewer-dialog"
        :title="evidenceViewerSegment?.timeText ? `证据图 - ${evidenceViewerSegment.timeText}` : '证据图'"
        width="760px"
        append-to-body
      >
        <div class="evidence-viewer-body">
          <div v-if="evidenceViewerImages.length > 0" class="evidence-viewer-strip">
            <img
              v-for="image in evidenceViewerImages"
              :key="image.key"
              :src="image.url"
              alt="片段证据图"
              class="evidence-viewer-image"
              @error="markSegmentImageBroken(image)"
            />
          </div>
          <div v-else class="empty-state compact-empty">
            暂无可展示的证据图。
          </div>
        </div>
      </el-dialog>

      <section class="audit-actions">
        <div class="action-copy">
          AI 审核结果仅供参考，请结合实际播放内容完成最终人工审核。
        </div>

        <div v-if="canManualAudit(postDetail)" class="action-buttons">
          <el-button
            type="success"
            :loading="submittingAuditStatus === 3"
            :disabled="!canSubmitAudit"
            @click="submitAudit(3)"
          >
            通过
          </el-button>
          <el-button
            type="danger"
            :loading="submittingAuditStatus === 4"
            :disabled="!canSubmitAudit"
            @click="submitAudit(4)"
          >
            驳回
          </el-button>
        </div>

        <div v-else class="action-status">
          当前稿件状态为 {{ postDetail.statusMeta.label }}，无法在此继续人工审核。
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.audit-detail-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.page-copy h2 {
  margin: 8px 0 0;
  color: #1f2a44;
  font-size: 24px;
}

.page-copy p {
  margin: 8px 0 0;
  color: #6f7f9f;
  font-size: 14px;
  line-height: 1.7;
}

.back-btn {
  padding-left: 0;
}

.loading-shell {
  min-height: 320px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.loading-card,
.panel,
.overview-card,
.summary-card,
.audit-actions {
  border: 1px solid #dbe4fb;
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(246, 249, 255, 0.98));
  box-shadow: 0 12px 28px rgba(56, 84, 144, 0.08);
}

.loading-card {
  padding: 20px 28px;
  color: #60759f;
  font-size: 14px;
}

.overview-card {
  padding: 18px;
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
  gap: 18px;
}

.cover-wrap {
  min-width: 0;
}

.video-cover {
  width: 100%;
  aspect-ratio: 16 / 9;
  border-radius: 16px;
  border: 1px solid #dbe4fb;
  background: #eef3ff;
  object-fit: cover;
}

.video-cover.empty {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6f7f9f;
  font-size: 13px;
}

.overview-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.title-row,
.section-head,
.segment-head,
.file-item-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.title-copy {
  min-width: 0;
}

.title-copy h3,
.section-head h3 {
  margin: 0;
  color: #1f2a44;
}

.title-copy h3 {
  font-size: 20px;
}

.title-copy p,
.section-head p,
.segment-subtitle,
.file-meta,
.action-copy,
.action-status,
.empty-state {
  margin: 6px 0 0;
  color: #6f7f9f;
  font-size: 13px;
  line-height: 1.6;
}

.meta-grid,
.summary-grid,
.detail-metrics {
  display: grid;
  gap: 12px;
}

.meta-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.summary-grid,
.detail-metrics {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin-top: 14px;
}

.summary-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.meta-card {
  border: 1px solid #e2e9fb;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.92);
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.meta-label {
  color: #6b7ca0;
  font-size: 12px;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.meta-card strong,
.copy-block p,
.file-name,
.segment-title,
.segment-reason,
.segment-preview {
  margin: 0;
  color: #233554;
  line-height: 1.6;
}

.copy-block {
  border: 1px solid #e2e9fb;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.88);
  padding: 14px;
}

.summary-card,
.panel,
.audit-actions {
  padding: 18px;
}

.summary-copy {
  margin-top: 14px;
}

.technical-details {
  color: #42587f;
}

.technical-details summary {
  cursor: pointer;
  color: #51678f;
  font-size: 13px;
  font-weight: 700;
}

.technical-details .meta-label {
  display: block;
  margin-top: 10px;
}

.notice-stack {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 14px;
}

.compact-empty {
  min-height: 100px;
}

.review-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(340px, 0.92fr);
  gap: 16px;
}

.player-column,
.side-column {
  min-width: 0;
}

.side-column {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.section-head.compact p {
  margin-top: 4px;
}

.meta-pill,
.mini-pill,
.section-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid transparent;
  border-radius: 999px;
  white-space: nowrap;
  line-height: 1;
}

.meta-pill {
  padding: 7px 12px;
  font-size: 12px;
  font-weight: 600;
}

.mini-pill,
.section-chip {
  padding: 7px 10px;
  font-size: 11px;
  font-weight: 600;
}

.section-chip {
  color: #5b6e95;
  background: #f3f7ff;
  border-color: #d5e0fb;
}

.tone-pass {
  color: #18794e;
  background: #e8f7ee;
  border-color: #9ed6b4;
}

.tone-fail {
  color: #b42318;
  background: #fff1f3;
  border-color: #ffc5cf;
}

.tone-review {
  color: #b45309;
  background: #fff6dd;
  border-color: #ffd47c;
}

.tone-neutral {
  color: #556b92;
  background: #eef3ff;
  border-color: #d7e0f8;
}

.tone-processing {
  color: #1d4ed8;
  background: #e8f1ff;
  border-color: #b8d2ff;
}

.file-list,
.segment-list,
.file-badges,
.tag-list {
  display: flex;
  gap: 10px;
}

.file-list,
.segment-list {
  flex-direction: column;
  margin-top: 14px;
}

.file-badges,
.tag-list {
  flex-wrap: wrap;
}

.file-item,
.segment-card {
  width: 100%;
  border: 1px solid #dce5fb;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.94);
  padding: 14px;
  text-align: left;
  cursor: pointer;
  overflow: hidden;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.file-item:hover,
.segment-card:hover {
  border-color: #8ea7f8;
  box-shadow: 0 10px 20px rgba(58, 92, 168, 0.12);
}

.segment-card:focus-visible {
  outline: 3px solid rgba(93, 118, 255, 0.24);
  outline-offset: 2px;
}

.segment-card.risky {
  border-color: #ffc5cf;
}

.segment-card.normal {
  border-color: #bde7cf;
}

.file-item.active,
.segment-card.active {
  border-color: #5d76ff;
  background: linear-gradient(180deg, #f5f8ff, #eef3ff);
}

.file-label,
.playing-label {
  font-size: 12px;
  font-weight: 700;
}

.file-label {
  color: #51678f;
}

.playing-label {
  color: #4564ff;
}

.file-name {
  margin-top: 8px;
  font-size: 15px;
  font-weight: 700;
}

.segment-copy {
  min-width: 0;
}

.segment-title {
  font-size: 15px;
  font-weight: 700;
}

.segment-reason,
.segment-preview {
  margin-top: 10px;
  color: #42587f;
  font-size: 13px;
  line-height: 1.6;
}

.segment-reason-clean {
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.segment-preview-clean {
  color: #5b6e95;
}

.segment-evidence-tags {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
}

.segment-evidence-trigger {
  border: 1px solid #d6dff8;
  border-radius: 999px;
  background: #f5f7ff;
  color: #42587f;
  padding: 7px 10px;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
  cursor: pointer;
}

.segment-evidence-trigger:hover {
  border-color: #8ea7f8;
  color: #3657d8;
}

.evidence-viewer-body {
  min-width: 0;
}

.evidence-viewer-strip {
  display: flex;
  gap: 12px;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  height: 260px;
  box-sizing: border-box;
  overflow-x: auto;
  overflow-y: hidden;
  padding: 10px;
  border: 1px solid #e3eaf9;
  border-radius: 12px;
  background: #f7f9ff;
  scroll-snap-type: x mandatory;
  scrollbar-width: thin;
}

.evidence-viewer-image {
  flex: 0 0 420px;
  width: 420px;
  height: 236px;
  object-fit: contain;
  display: block;
  border: 1px solid #dce5fb;
  border-radius: 10px;
  background: #eef3ff;
  scroll-snap-align: start;
}

.tag-item {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #d6dff8;
  border-radius: 999px;
  background: #f4f7ff;
  color: #4b5f86;
  padding: 7px 10px;
  font-size: 12px;
}

.evidence-panel {
  min-width: 0;
}

.audit-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.action-buttons {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

@media (max-width: 1180px) {
  .overview-card,
  .review-layout {
    grid-template-columns: 1fr;
  }

  .summary-grid,
  .detail-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 820px) {
  .page-head,
  .title-row,
  .section-head,
  .segment-head,
  .audit-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .meta-grid,
  .summary-grid,
  .detail-metrics {
    grid-template-columns: 1fr;
  }

  .action-buttons {
    width: 100%;
  }

  .action-buttons :deep(.el-button) {
    flex: 1;
  }
}
</style>
