<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import {
  cancelTopComment,
  doCommentAction,
  loadComment,
  postComment,
  topComment,
  userDelComment,
} from '@/api/comment'
import { uploadImage } from '@/api/file'
import IconFont from '@/components/IconFont.vue'

const route = useRoute()
const router = useRouter()

const props = defineProps({
  videoId: {
    type: String,
    required: true,
  },
  videoUserId: {
    type: String,
    default: '',
  },
  currentUserId: {
    type: String,
    default: '',
  },
})

const COMMENT_PAGE_SIZE = 15
const MAX_COMMENT_LENGTH = 500
const COMMENT_GOOD_ACTION_TYPE = 0
const COMMENT_NOGOOD_ACTION_TYPE = 1

const orderType = ref(0)
const initialLoading = ref(false)
const dataReady = ref(false)
const isCommentClosed = ref(false)
const commentList = ref([])
const pagination = reactive({
  pageNo: 1,
  pageSize: COMMENT_PAGE_SIZE,
  pageTotal: 0,
  totalCount: 0,
})

const posting = ref(false)
const imageUploading = ref(false)
const composerContent = ref('')
const composerImgPath = ref('')
const replyTarget = ref(null)
const imageInputRef = ref(null)
const commentActionLoadingMap = reactive({})
const userCommentActionMap = ref({})

const displayCommentCount = computed(() => {
  return Math.max(0, Number(pagination.totalCount || 0))
})

const normalizedCurrentUserId = computed(() => String(props.currentUserId || '').trim())
const normalizedVideoUserId = computed(() => String(props.videoUserId || '').trim())
const canManageTopComment = computed(() => {
  if (!normalizedCurrentUserId.value || !normalizedVideoUserId.value) {
    return false
  }
  return normalizedCurrentUserId.value === normalizedVideoUserId.value
})

const canSubmitComment = computed(() => {
  if (isCommentClosed.value || posting.value || imageUploading.value) {
    return false
  }
  const text = String(composerContent.value || '').trim()
  return text.length > 0 && text.length <= MAX_COMMENT_LENGTH
})

const orderOptions = [
  { label: '按热度', value: 0 },
  { label: '按时间', value: 1 },
]

function toResourceUrl(path) {
  const source = String(path || '').trim()
  if (!source) {
    return ''
  }
  if (source.startsWith('http://') || source.startsWith('https://') || source.startsWith('data:')) {
    return source
  }
  return `/file/getResource?sourceName=${encodeURIComponent(source.replace(/^\/+/, ''))}`
}

function formatDate(value) {
  if (!value) {
    return '--'
  }
  const text = String(value).trim()
  if (!text) {
    return '--'
  }
  if (/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/.test(text) || /^\d{4}-\d{2}-\d{2}$/.test(text)) {
    return text
  }
  if (/^\d{4}-\d{2}-\d{2}T/.test(text)) {
    return text.replace('T', ' ').slice(0, 19)
  }
  const date = new Date(text)
  if (Number.isNaN(date.getTime())) {
    return text
  }
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  const second = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}:${second}`
}

function resetCommentState() {
  commentList.value = []
  isCommentClosed.value = false
  pagination.pageNo = 1
  pagination.pageSize = COMMENT_PAGE_SIZE
  pagination.pageTotal = 0
  pagination.totalCount = 0
  userCommentActionMap.value = {}
  dataReady.value = false
}

function normalizeComment(comment = {}) {
  const children = Array.isArray(comment.children) ? comment.children : []
  return {
    commentId: Number(comment.commentId || 0),
    pCommentId: Number(comment.pCommentId || 0),
    videoId: String(comment.videoId || ''),
    videoUserId: String(comment.videoUserId || ''),
    content: String(comment.content || ''),
    imgPath: String(comment.imgPath || ''),
    userId: String(comment.userId || ''),
    replyUserId: comment.replyUserId === null || comment.replyUserId === undefined ? null : String(comment.replyUserId),
    topType: Number(comment.topType || 0),
    postTime: String(comment.postTime || ''),
    likeCount: Number(comment.likeCount || 0),
    hateCount: Number(comment.hateCount || 0),
    avatar: String(comment.avatar || ''),
    nickName: String(comment.nickName || ''),
    replyAvatar: String(comment.replyAvatar || ''),
    replyNickName: String(comment.replyNickName || ''),
    children: children.map((child) => ({
      ...normalizeComment({
        ...child,
        children: [],
      }),
      pCommentId: Number(child?.pCommentId || comment.commentId || 0),
    })),
  }
}

function normalizeUserActionMap(userActionList = []) {
  const actionMap = {}
  if (!Array.isArray(userActionList)) {
    return actionMap
  }

  userActionList.forEach((item) => {
    const commentId = Number(item?.commentId || 0)
    const actionType = Number(item?.actionType)
    const actionCount = Number(item?.actionCount ?? 1)
    if (!commentId || ![COMMENT_GOOD_ACTION_TYPE, COMMENT_NOGOOD_ACTION_TYPE].includes(actionType) || actionCount <= 0) {
      return
    }
    actionMap[commentId] = actionType
  })

  return actionMap
}

function normalizeLoadCommentResponse(data) {
  if (Array.isArray(data)) {
    return {
      closed: true,
      list: [],
      pageNo: 1,
      pageSize: COMMENT_PAGE_SIZE,
      pageTotal: 0,
      totalCount: 0,
      userActionMap: {},
    }
  }

  const payload = data && typeof data === 'object' ? data : {}
  const commentData = payload?.commentData && typeof payload.commentData === 'object'
    ? payload.commentData
    : payload
  const list = Array.isArray(commentData?.list) ? commentData.list : []

  return {
    closed: false,
    list: list.map((item) => normalizeComment(item)),
    pageNo: Math.max(1, Number(commentData?.pageNo || 1)),
    pageSize: Math.max(1, Number(commentData?.pageSize || COMMENT_PAGE_SIZE)),
    pageTotal: Math.max(0, Number(commentData?.pageTotal || 0)),
    totalCount: Math.max(0, Number(commentData?.totalCount || list.length || 0)),
    userActionMap: normalizeUserActionMap(payload?.userActionList),
  }
}

async function fetchComments() {
  const normalizedVideoId = String(props.videoId || '').trim()
  if (!normalizedVideoId) {
    resetCommentState()
    return
  }

  initialLoading.value = true
  try {
    const data = await loadComment({
      videoId: normalizedVideoId,
      pageNo: '',
      orderType: Number(orderType.value || 0),
    })
    const normalized = normalizeLoadCommentResponse(data)

    isCommentClosed.value = normalized.closed
    pagination.pageNo = normalized.pageNo
    pagination.pageSize = normalized.pageSize
    pagination.pageTotal = normalized.pageTotal
    pagination.totalCount = normalized.totalCount
    commentList.value = normalized.list
    userCommentActionMap.value = normalized.userActionMap
  } catch (_error) {
    commentList.value = []
    isCommentClosed.value = false
    pagination.pageNo = 1
    pagination.pageSize = COMMENT_PAGE_SIZE
    pagination.pageTotal = 0
    pagination.totalCount = 0
    userCommentActionMap.value = {}
  } finally {
    dataReady.value = true
    initialLoading.value = false
  }
}

async function reloadComments() {
  await fetchComments()
}

function clearComposer() {
  composerContent.value = ''
  composerImgPath.value = ''
  replyTarget.value = null
}

function getCommentDisplayName(comment = {}) {
  return String(comment?.nickName || '').trim() || '匿名用户'
}

function canDeleteComment(comment = {}) {
  const commentUserId = String(comment.userId || '').trim()
  if (!commentUserId || !normalizedCurrentUserId.value) {
    return false
  }
  return commentUserId === normalizedCurrentUserId.value
}

function canToggleTop(comment = {}) {
  if (!canManageTopComment.value) {
    return false
  }
  return Number(comment?.pCommentId || 0) === 0
}

function isCommentActionLoading(commentId) {
  return Boolean(commentActionLoadingMap[String(commentId || '')])
}

function setCommentActionLoading(commentId, loading) {
  const key = String(commentId || '')
  if (!key) {
    return
  }
  commentActionLoadingMap[key] = Boolean(loading)
}

function getCurrentCommentActionType(comment = {}) {
  const commentId = Number(comment.commentId || 0)
  if (!commentId) {
    return -1
  }
  return Number(userCommentActionMap.value[commentId] ?? -1)
}

function isCommentGoodActive(comment = {}) {
  return getCurrentCommentActionType(comment) === COMMENT_GOOD_ACTION_TYPE
}

function isCommentNoGoodActive(comment = {}) {
  return getCurrentCommentActionType(comment) === COMMENT_NOGOOD_ACTION_TYPE
}

async function handleCommentReaction(comment = {}, actionType) {
  const normalizedVideoId = String(props.videoId || '').trim()
  const commentId = Number(comment.commentId || 0)
  const normalizedActionType = Number(actionType)

  if (
    !normalizedVideoId
    || !commentId
    || ![COMMENT_GOOD_ACTION_TYPE, COMMENT_NOGOOD_ACTION_TYPE].includes(normalizedActionType)
    || isCommentActionLoading(commentId)
  ) {
    return
  }

  setCommentActionLoading(commentId, true)
  try {
    await doCommentAction({
      videoId: normalizedVideoId,
      commentId,
      actionType: normalizedActionType,
      actionCount: 1,
    })
    await reloadComments()
  } catch (_error) {
    // Error message handled by request interceptor.
  } finally {
    setCommentActionLoading(commentId, false)
  }
}

function handleReply(comment = {}) {
  replyTarget.value = {
    commentId: Number(comment.commentId || 0),
    nickName: getCommentDisplayName(comment),
  }
}

function cancelReply() {
  replyTarget.value = null
}

async function handleSubmitComment() {
  if (!canSubmitComment.value) {
    if (String(composerContent.value || '').trim().length > MAX_COMMENT_LENGTH) {
      ElMessage.warning(`评论内容最多 ${MAX_COMMENT_LENGTH} 字`)
    }
    return
  }

  const payload = {
    videoId: String(props.videoId || '').trim(),
    content: String(composerContent.value || '').trim(),
  }

  if (replyTarget.value?.commentId) {
    payload.replyCommentId = Number(replyTarget.value.commentId)
  }
  if (composerImgPath.value) {
    payload.imgPath = composerImgPath.value
  }

  posting.value = true
  try {
    await postComment(payload)
    ElMessage.success(replyTarget.value ? '回复成功' : '评论成功')
    clearComposer()
    await reloadComments()
  } catch (_error) {
    // Error message handled by request interceptor.
  } finally {
    posting.value = false
  }
}

function handleSelectImage() {
  if (isCommentClosed.value || imageUploading.value) {
    return
  }
  imageInputRef.value?.click()
}

async function handleImageChange(event) {
  const file = event?.target?.files?.[0]
  if (event?.target) {
    event.target.value = ''
  }
  if (!file) {
    return
  }
  if (!String(file.type || '').startsWith('image/')) {
    ElMessage.warning('仅支持图片文件')
    return
  }

  imageUploading.value = true
  try {
    const path = await uploadImage(file, false)
    composerImgPath.value = String(path || '')
    ElMessage.success('图片上传成功')
  } catch (_error) {
    // Error message handled by request interceptor.
  } finally {
    imageUploading.value = false
  }
}

function removeComposerImage() {
  composerImgPath.value = ''
}

async function handleDeleteComment(comment = {}) {
  const commentId = Number(comment.commentId || 0)
  if (!commentId || isCommentActionLoading(commentId)) {
    return
  }

  try {
    await ElMessageBox.confirm('确认删除这条评论吗？', '删除评论', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch (_cancel) {
    return
  }

  setCommentActionLoading(commentId, true)
  try {
    await userDelComment({ commentId })
    ElMessage.success('删除成功')
    await reloadComments()
  } catch (_error) {
    // Error message handled by request interceptor.
  } finally {
    setCommentActionLoading(commentId, false)
  }
}

async function handleToggleTop(comment = {}) {
  const commentId = Number(comment.commentId || 0)
  if (!commentId || !canToggleTop(comment) || isCommentActionLoading(commentId)) {
    return
  }

  setCommentActionLoading(commentId, true)
  try {
    if (Number(comment.topType || 0) === 1) {
      await cancelTopComment({ commentId })
      ElMessage.success('已取消置顶')
    } else {
      await topComment({ commentId })
      ElMessage.success('已置顶评论')
    }
    await reloadComments()
  } catch (_error) {
    // Error message handled by request interceptor.
  } finally {
    setCommentActionLoading(commentId, false)
  }
}

function switchOrderType(nextValue) {
  const target = Number(nextValue)
  if (![0, 1].includes(target) || target === orderType.value) {
    return
  }
  orderType.value = target
}

function navigateWithPageRefresh(location) {
  const fallbackPush = () => {
    router.push(location).catch(() => {})
  }

  const resolved = router.resolve(location)
  if (typeof window === 'undefined') {
    fallbackPush()
    return
  }

  const targetFullPath = String(resolved?.fullPath || '').trim()
  const currentFullPath = String(route.fullPath || '').trim()
  if (targetFullPath && targetFullPath === currentFullPath) {
    window.location.reload()
    return
  }

  const targetHref = String(resolved?.href || '').trim()
  if (targetHref) {
    window.location.assign(targetHref)
    return
  }

  fallbackPush()
}

function goUserCenter(userId) {
  const targetUserId = String(userId || '').trim()
  if (!targetUserId) {
    return
  }
  navigateWithPageRefresh({
    path: '/user-center',
    query: {
      userId: targetUserId,
    },
  })
}

watch(
  () => props.videoId,
  async (videoId) => {
    if (!String(videoId || '').trim()) {
      resetCommentState()
      clearComposer()
      return
    }
    await reloadComments()
  },
  { immediate: true },
)

watch(orderType, async () => {
  const normalizedVideoId = String(props.videoId || '').trim()
  if (!normalizedVideoId) {
    return
  }
  await reloadComments()
})
</script>

<template>
  <section class="panel comment-panel">
    <div class="comment-head">
      <h2>评论区</h2>
      <div class="comment-head-right">
        <span class="comment-total">{{ displayCommentCount }} 条评论</span>
        <div class="order-tabs">
          <button
            v-for="option in orderOptions"
            :key="option.value"
            type="button"
            class="order-tab"
            :class="{ active: orderType === option.value }"
            @click="switchOrderType(option.value)"
          >
            {{ option.label }}
          </button>
        </div>
      </div>
    </div>

    <div class="composer-card" :class="{ disabled: isCommentClosed }">
      <div class="composer-main">
        <div v-if="replyTarget" class="replying-tip">
          <span>回复 {{ replyTarget.nickName }}</span>
          <button type="button" class="inline-action" @click="cancelReply">取消</button>
        </div>

        <el-input
          v-model="composerContent"
          type="textarea"
          :rows="4"
          resize="none"
          :maxlength="MAX_COMMENT_LENGTH"
          show-word-limit
          :disabled="isCommentClosed"
          :placeholder="isCommentClosed ? '评论区已关闭' : '写下你的评论...'"
        />

        <div v-if="composerImgPath" class="composer-image-preview">
          <img :src="toResourceUrl(composerImgPath)" alt="评论图片" />
          <button type="button" class="inline-action danger" @click="removeComposerImage">移除图片</button>
        </div>

        <div class="composer-actions">
          <div class="composer-action-left">
            <button
              type="button"
              class="inline-action"
              :disabled="isCommentClosed || imageUploading"
              @click="handleSelectImage"
            >
              {{ imageUploading ? '图片上传中...' : '上传图片' }}
            </button>
            <span v-if="isCommentClosed" class="closed-tip">评论区已关闭</span>
          </div>

          <el-button
            type="primary"
            :disabled="!canSubmitComment"
            :loading="posting"
            @click="handleSubmitComment"
          >
            {{ replyTarget ? '发布回复' : '发布评论' }}
          </el-button>
        </div>
      </div>

      <input
        ref="imageInputRef"
        class="hidden-file"
        type="file"
        accept="image/*"
        @change="handleImageChange"
      />
    </div>

    <el-skeleton v-if="initialLoading && !dataReady" :rows="5" animated />

    <el-alert
      v-else-if="isCommentClosed"
      type="warning"
      show-icon
      :closable="false"
      title="评论区已关闭"
    />

    <el-empty
      v-else-if="commentList.length === 0"
      description="暂无评论，快来抢沙发"
      :image-size="90"
    />

    <div v-else class="comment-list">
      <article
        v-for="comment in commentList"
        :key="`comment-${comment.commentId}`"
        class="comment-item"
        :class="{ top: Number(comment.topType || 0) === 1 }"
      >
        <div class="comment-main">
          <button
            type="button"
            class="comment-user-entry"
            @click="goUserCenter(comment.userId)"
          >
            <el-avatar :src="toResourceUrl(comment.avatar)" :size="38">
              <IconFont name="icon-morentouxiang" size="16px" />
            </el-avatar>
          </button>

          <div class="comment-body">
            <div class="comment-meta">
              <button
                type="button"
                class="comment-author-entry"
                @click="goUserCenter(comment.userId)"
              >
                {{ getCommentDisplayName(comment) }}
              </button>
              <span class="comment-time">{{ formatDate(comment.postTime) }}</span>
              <span v-if="Number(comment.topType || 0) === 1" class="top-tag">置顶</span>
            </div>

            <p class="comment-content">{{ comment.content }}</p>

            <img
              v-if="comment.imgPath"
              class="comment-image"
              :src="toResourceUrl(comment.imgPath)"
              alt="评论图片"
            />

            <div class="comment-actions">
              <button
                type="button"
                class="inline-action reaction-action"
                :class="{ active: isCommentGoodActive(comment) }"
                :disabled="isCommentActionLoading(comment.commentId)"
                @click="handleCommentReaction(comment, COMMENT_GOOD_ACTION_TYPE)"
              >
                <IconFont class="reaction-icon" name="icon-dianzan" size="14px" />
                <span>{{ Number(comment.likeCount || 0) }}</span>
              </button>
              <button
                type="button"
                class="inline-action reaction-action nogood"
                :class="{ active: isCommentNoGoodActive(comment) }"
                :disabled="isCommentActionLoading(comment.commentId)"
                @click="handleCommentReaction(comment, COMMENT_NOGOOD_ACTION_TYPE)"
              >
                <IconFont class="reaction-icon nogood" name="icon-dianzan" size="14px" />
                <span>{{ Number(comment.hateCount || 0) }}</span>
              </button>
              <button type="button" class="inline-action" @click="handleReply(comment)">回复</button>
              <button
                v-if="canDeleteComment(comment)"
                type="button"
                class="inline-action danger"
                :disabled="isCommentActionLoading(comment.commentId)"
                @click="handleDeleteComment(comment)"
              >
                删除
              </button>
              <button
                v-if="canToggleTop(comment)"
                type="button"
                class="inline-action"
                :disabled="isCommentActionLoading(comment.commentId)"
                @click="handleToggleTop(comment)"
              >
                {{ Number(comment.topType || 0) === 1 ? '取消置顶' : '置顶' }}
              </button>
            </div>

            <div v-if="comment.children.length > 0" class="child-comment-list">
              <article
                v-for="child in comment.children"
                :key="`comment-${comment.commentId}-child-${child.commentId}`"
                class="child-comment-item"
              >
                <button
                  type="button"
                  class="comment-user-entry"
                  @click="goUserCenter(child.userId)"
                >
                  <el-avatar :src="toResourceUrl(child.avatar)" :size="30">
                    <IconFont name="icon-morentouxiang" size="14px" />
                  </el-avatar>
                </button>

                <div class="child-comment-body">
                  <div class="comment-meta">
                    <button
                      type="button"
                      class="comment-author-entry"
                      @click="goUserCenter(child.userId)"
                    >
                      {{ getCommentDisplayName(child) }}
                    </button>
                    <span class="comment-time">{{ formatDate(child.postTime) }}</span>
                  </div>

                  <p class="comment-content child-content">
                    <template v-if="child.replyNickName">
                      <span class="reply-prefix">回复 {{ child.replyNickName }}：</span>
                    </template>
                    {{ child.content }}
                  </p>

                  <img
                    v-if="child.imgPath"
                    class="comment-image child-image"
                    :src="toResourceUrl(child.imgPath)"
                    alt="回复图片"
                  />

                  <div class="comment-actions">
                    <button
                      type="button"
                      class="inline-action reaction-action"
                      :class="{ active: isCommentGoodActive(child) }"
                      :disabled="isCommentActionLoading(child.commentId)"
                      @click="handleCommentReaction(child, COMMENT_GOOD_ACTION_TYPE)"
                    >
                      <IconFont class="reaction-icon" name="icon-dianzan" size="14px" />
                      <span>{{ Number(child.likeCount || 0) }}</span>
                    </button>
                    <button
                      type="button"
                      class="inline-action reaction-action nogood"
                      :class="{ active: isCommentNoGoodActive(child) }"
                      :disabled="isCommentActionLoading(child.commentId)"
                      @click="handleCommentReaction(child, COMMENT_NOGOOD_ACTION_TYPE)"
                    >
                      <IconFont class="reaction-icon nogood" name="icon-dianzan" size="14px" />
                      <span>{{ Number(child.hateCount || 0) }}</span>
                    </button>
                    <button type="button" class="inline-action" @click="handleReply(child)">回复</button>
                    <button
                      v-if="canDeleteComment(child)"
                      type="button"
                      class="inline-action danger"
                      :disabled="isCommentActionLoading(child.commentId)"
                      @click="handleDeleteComment(child)"
                    >
                      删除
                    </button>
                  </div>
                </div>
              </article>
            </div>
          </div>
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.comment-panel {
  margin-top: 14px;
  padding: 16px;
  border: 1px solid #dce6ff;
  border-radius: 14px;
  background: linear-gradient(180deg, #fbfdff, #f5f9ff);
}

.comment-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.comment-head h2 {
  margin: 0;
  color: #203153;
  font-size: 18px;
}

.comment-head-right {
  display: inline-flex;
  align-items: center;
  gap: 12px;
}

.comment-total {
  color: #677ba4;
  font-size: 13px;
}

.order-tabs {
  display: inline-flex;
  align-items: center;
  border: 1px solid #d7e2ff;
  border-radius: 999px;
  overflow: hidden;
  background: #ffffff;
}

.order-tab {
  border: none;
  min-height: 30px;
  padding: 0 12px;
  background: transparent;
  color: #5f739a;
  font-size: 13px;
  cursor: pointer;
  transition: color 0.2s ease, background-color 0.2s ease;
}

.order-tab.active {
  background: #edf2ff;
  color: #3d73ff;
}

.composer-card {
  margin-top: 14px;
  border: 1px solid #dbe6ff;
  border-radius: 12px;
  background: #ffffff;
  padding: 12px;
}

.composer-card.disabled {
  opacity: 0.85;
}

.composer-main {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.replying-tip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #5c7098;
  font-size: 13px;
}

.composer-image-preview {
  display: inline-flex;
  align-items: flex-start;
  gap: 10px;
}

.composer-image-preview img {
  width: 130px;
  max-width: 100%;
  border-radius: 8px;
  border: 1px solid #d9e5ff;
  object-fit: cover;
}

.composer-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.composer-action-left {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.closed-tip {
  color: #9a7a34;
  font-size: 12px;
}

.hidden-file {
  display: none;
}

.comment-list {
  margin-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.comment-item {
  border: 1px solid #dde7ff;
  border-radius: 12px;
  background: #ffffff;
  padding: 12px;
}

.comment-item.top {
  border-color: #aec0ff;
  background: linear-gradient(180deg, #ffffff 0%, #f4f8ff 100%);
}

.comment-main {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.comment-body {
  min-width: 0;
  flex: 1;
}

.comment-user-entry {
  border: none;
  background: transparent;
  padding: 0;
  cursor: pointer;
}

.comment-user-entry:focus-visible {
  outline: 2px solid #5d76ff;
  outline-offset: 3px;
  border-radius: 999px;
}

.comment-meta {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.comment-author-entry {
  border: none;
  background: transparent;
  padding: 0;
  color: #26395f;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}

.comment-author-entry:hover {
  color: #3b72ff;
}

.comment-author-entry:focus-visible {
  outline: 2px solid #5d76ff;
  outline-offset: 3px;
  border-radius: 6px;
}

.comment-time {
  color: #7e8faf;
  font-size: 12px;
}

.top-tag {
  border-radius: 999px;
  border: 1px solid #bfd0ff;
  background: #edf3ff;
  color: #3b72ff;
  font-size: 11px;
  padding: 1px 7px;
}

.comment-content {
  margin: 8px 0 0;
  color: #2b4068;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.reply-prefix {
  color: #4f6fb7;
}

.comment-image {
  margin-top: 8px;
  max-width: min(280px, 100%);
  border-radius: 8px;
  border: 1px solid #dae5ff;
}

.comment-actions {
  margin-top: 8px;
  display: inline-flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.inline-action {
  border: none;
  background: transparent;
  color: #5d74a0;
  font-size: 12px;
  padding: 0;
  cursor: pointer;
  transition: color 0.2s ease;
}

.inline-action:hover {
  color: #3b72ff;
}

.inline-action:disabled {
  cursor: wait;
  opacity: 0.7;
}

.inline-action.danger {
  color: #c14a5a;
}

.inline-action.danger:hover {
  color: #ad273a;
}

.reaction-action {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.reaction-action .reaction-icon {
  transition: transform 0.2s ease;
}

.reaction-action.active,
.reaction-action.active:hover {
  color: #2f6bff;
}

.reaction-action.nogood .reaction-icon {
  transform: rotate(180deg);
}

.reaction-action.nogood.active,
.reaction-action.nogood.active:hover {
  color: #d24f73;
}

.child-comment-list {
  margin-top: 10px;
  border-radius: 10px;
  background: #f7faff;
  border: 1px solid #e2ebff;
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.child-comment-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.child-comment-body {
  min-width: 0;
  flex: 1;
}

.child-content {
  font-size: 13px;
}

.child-image {
  max-width: min(240px, 100%);
}

@media (max-width: 760px) {
  .comment-panel {
    padding: 12px;
  }

  .comment-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .composer-actions {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>

