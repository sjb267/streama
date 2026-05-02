<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import AuthDialog from '@/components/AuthDialog.vue'
import IconFont from '@/components/IconFont.vue'
import { loadAllCategory } from '@/api/category'
import { deleteUploadVideo, preUploadVideo, uploadImage, uploadVideo } from '@/api/file'
import { getSystemSetting } from '@/api/sysSetting'
import {
  delComment as deleteInteractionComment,
  delDanmu as deleteInteractionDanmu,
  deleteVideo,
  getVideoByVideoId,
  getVideoCountInfo,
  loadAllVideo as loadInteractionVideoList,
  loadComment as loadInteractionCommentList,
  loadDanmu as loadInteractionDanmuList,
  loadVideoPostList,
  postVideo,
  saveVideoInteraction,
} from '@/api/ucenter'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const CHUNK_SIZE = 5 * 1024 * 1024
const DEFAULT_MAX_PART_COUNT = 10
const DEFAULT_MAX_VIDEO_SIZE_MB = 100
const INTERACTION_PAGE_SIZE = 15
const NON_EDITABLE_POST_STATUS = new Set([0, 2])
const SECTION_KEYS = ['home', 'content', 'interaction', 'create']

const centerNavItems = [
  { key: 'create', label: '创建' },
  { key: 'home', label: '首页' },
  { key: 'content', label: '稿件管理' },
  { key: 'interaction', label: '互动管理' },
]

const postStatusTabs = [
  { key: 'all', label: '全部稿件', status: null, countKey: 'all' },
  { key: 'progress', label: '进行中', status: -1, countKey: 'inProgress' },
  { key: 'pass', label: '审核通过', status: 3, countKey: 'auditPassCount' },
  { key: 'fail', label: '审核失败', status: 4, countKey: 'auditFailCount' },
]
const postMetricConfigs = [
  { key: 'playCount', icon: 'icon-you' },
  { key: 'likeCount', icon: 'icon-dianzan' },
  { key: 'danmuCount', icon: 'icon-biaoqing' },
  { key: 'commentCount', icon: 'icon-huifu' },
  { key: 'coinCount', icon: 'icon-dashang' },
  { key: 'collectCount', icon: 'icon-shoucang1' },
]
const interactionTabs = [
  { key: 'comment', label: '评论管理' },
  { key: 'danmu', label: '弹幕管理' },
]

const activeSection = ref('home')
const categories = ref([])
const categoryLoading = ref(false)
const settingLoading = ref(false)
const maxPartCount = ref(DEFAULT_MAX_PART_COUNT)
const maxVideoSizeMb = ref(DEFAULT_MAX_VIDEO_SIZE_MB)
const coverUploading = ref(false)
const submitting = ref(false)
const editMode = ref('create')
const editingVideoId = ref('')
const editLoading = ref(false)
const editError = ref('')
const hydratingEditCategory = ref(false)
let editRequestId = 0

const postCountLoading = ref(false)
const postListLoading = ref(false)
const postStatusKey = ref('all')
const postList = ref([])
const postMoreMenuVisibleMap = reactive({})
const postInteractionStateMap = reactive({})
const postInteractionSavingMap = reactive({})
const postDeletingMap = reactive({})
const interactionTabKey = ref('comment')
const interactionVideoList = ref([])
const interactionVideoLoading = ref(false)
const interactionListLoading = ref(false)
const interactionSelectedVideoId = ref('')
const interactionCommentList = ref([])
const interactionDanmuList = ref([])
const interactionDeletingMap = reactive({})

const interactionPagination = reactive({
  pageNo: 1,
  pageSize: INTERACTION_PAGE_SIZE,
  totalCount: 0,
  pageTotal: 0,
})

const postPagination = reactive({
  pageNo: 1,
  pageSize: 15,
  totalCount: 0,
})

const postCountInfo = reactive({
  inProgress: 0,
  auditPassCount: 0,
  auditFailCount: 0,
})

const videoInputRef = ref(null)
const coverInputRef = ref(null)

const fileSelectTarget = ref({
  mode: 'add',
  index: null,
})

const createForm = reactive({
  videoCover: '',
  videoName: '',
  pCategoryId: '',
  categoryId: '',
  postType: 0,
  tags: '',
  introduction: '',
})
const tagInputValue = ref('')
const tagList = ref([])

const interactionState = reactive({
  closeComment: false,
  closeDanmu: false,
})

const editLockedMeta = reactive({
  pCategoryId: null,
  categoryId: null,
  interaction: '',
})

const videoParts = ref([])
const routeEditVideoId = computed(() => normalizeEditVideoId(route.query.editVideoId))

const parentCategories = computed(() => {
  return categories.value.filter((item) => getParentId(item) === 0).slice(0, 20)
})

const selectedParentCategory = computed(() => {
  return parentCategories.value.find(
    (item) => String(item?.categoryId) === String(createForm.pCategoryId),
  )
})

const childCategories = computed(() => {
  const fromChildren = selectedParentCategory.value?.children
  if (Array.isArray(fromChildren) && fromChildren.length > 0) {
    return fromChildren
  }
  const parentId = Number(createForm.pCategoryId)
  if (!parentId) {
    return []
  }
  return categories.value.filter((item) => getParentId(item) === parentId)
})

const partUploading = computed(() => {
  return videoParts.value.some((item) => item.uploading)
})

const reachedPartLimit = computed(() => {
  return videoParts.value.length >= maxPartCount.value
})

const allPartsReady = computed(() => {
  return (
    videoParts.value.length > 0 &&
    videoParts.value.every((item) => {
      if (item.status !== 'success') {
        return false
      }
      return Boolean(item.uploadId || item.fileId)
    })
  )
})

const interactionValue = computed(() => {
  let value = ''
  if (interactionState.closeComment) {
    value += '0'
  }
  if (interactionState.closeDanmu) {
    value += '1'
  }
  return value
})

const coverPreviewUrl = computed(() => {
  return toResourceUrl(createForm.videoCover)
})

const activePostStatus = computed(() => {
  return postStatusTabs.find((item) => item.key === postStatusKey.value) || postStatusTabs[0]
})

const postStatusCountMap = computed(() => {
  const inProgress = Number(postCountInfo.inProgress || 0)
  const auditPassCount = Number(postCountInfo.auditPassCount || 0)
  const auditFailCount = Number(postCountInfo.auditFailCount || 0)

  return {
    all: Math.max(0, inProgress + auditPassCount + auditFailCount),
    inProgress: Math.max(0, inProgress),
    auditPassCount: Math.max(0, auditPassCount),
    auditFailCount: Math.max(0, auditFailCount),
  }
})
const activeInteractionTab = computed(() => {
  return interactionTabs.find((item) => item.key === interactionTabKey.value) || interactionTabs[0]
})

const interactionActiveList = computed(() => {
  return interactionTabKey.value === 'danmu' ? interactionDanmuList.value : interactionCommentList.value
})

const placeholderTitle = computed(() => {
  if (activeSection.value === 'home') {
    return '创建中心首页'
  }
  if (activeSection.value === 'interaction') {
    return '互动管理'
  }
  return '页面建设中'
})

const activeSectionLabel = computed(() => {
  return centerNavItems.find((item) => item.key === activeSection.value)?.label || '创建中心'
})

const activeSectionHint = computed(() => {
  if (activeSection.value === 'create') {
    return editMode.value === 'edit'
      ? '当前为稿件编辑模式，保存后将回到稿件管理。'
      : '上传视频、补充稿件信息并完成投稿提交。'
  }
  if (activeSection.value === 'content') {
    return '查看全部稿件、审核进度和审核结果。'
  }
  if (activeSection.value === 'interaction') {
    return '按视频筛选评论与弹幕，支持分页查看和一键删除。'
  }
  return '该区域暂时预留，后续将逐步开放更多能力。'
})

const isEditMode = computed(() => {
  return editMode.value === 'edit' && Boolean(editingVideoId.value)
})

const createPanelTitle = computed(() => {
  return isEditMode.value ? '编辑稿件' : '视频投稿'
})

const createPanelDesc = computed(() => {
  return isEditMode.value
    ? '正在编辑已有稿件，修改后需要进行审核。'
    : '上传后即可填写投稿信息，支持分P、排序和删除。'
})

const submitButtonText = computed(() => {
  return isEditMode.value ? '保存修改' : '提交投稿'
})

const canSubmit = computed(() => {
  return (
    !submitting.value &&
    !editLoading.value &&
    allPartsReady.value &&
    Boolean(createForm.videoCover.trim()) &&
    Boolean(createForm.videoName.trim()) &&
    Boolean(createForm.pCategoryId) &&
    Boolean(createForm.tags.trim())
  )
})

watch(
  () => route.query.tab,
  (tab) => {
    activeSection.value = normalizeSection(tab)
  },
  { immediate: true },
)

watch(
  [() => activeSection.value, routeEditVideoId, () => authStore.isLoggedIn],
  ([section, editVideoId, isLoggedIn]) => {
    if (section !== 'create') {
      return
    }
    if (!editVideoId) {
      if (editMode.value === 'edit') {
        resetPostForm()
      }
      resetEditContext()
      return
    }
    if (!isLoggedIn) {
      authStore.openAuthDialog('login')
      resetPostForm()
      resetEditContext()
      switchSection('content')
      return
    }
    if (isEditMode.value && editingVideoId.value === editVideoId && !editError.value) {
      return
    }
    loadEditVideoDetail(editVideoId)
  },
  { immediate: true },
)

watch(
  () => parentCategories.value,
  (list) => {
    if (list.length === 0) {
      createForm.pCategoryId = ''
      createForm.categoryId = ''
      return
    }
    const currentExists = list.some(
      (item) => String(item?.categoryId) === String(createForm.pCategoryId),
    )
    if (!currentExists) {
      createForm.pCategoryId = String(list[0].categoryId)
    }
  },
  { immediate: true },
)

watch(
  () => createForm.pCategoryId,
  () => {
    if (hydratingEditCategory.value) {
      return
    }
    createForm.categoryId = ''
  },
)

watch(
  [() => activeSection.value, () => authStore.isLoggedIn],
  ([section, isLoggedIn]) => {
    if (section === 'content' && isLoggedIn) {
      refreshPostData()
      return
    }
    if (section === 'interaction') {
      if (!isLoggedIn) {
        resetInteractionState()
        return
      }
      refreshInteractionData({ resetPage: true })
    }
  },
  { immediate: true },
)

onMounted(() => {
  authStore.initAutoLogin()
  loadCategories()
  loadSystemSetting()
})

function normalizeSection(value) {
  const next = String(value || 'home').toLowerCase()
  return SECTION_KEYS.includes(next) ? next : 'home'
}

function normalizeEditVideoId(value) {
  return String(value || '').trim()
}

function toNullableNumber(value) {
  if (value === null || value === undefined || value === '') {
    return null
  }
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : null
}

function normalizeTag(value) {
  return String(value || '').replace(/\s+/g, ' ').trim()
}

function parseTagTokens(rawValue) {
  return String(rawValue || '')
    .split(/[,，\n]+/)
    .map((item) => normalizeTag(item))
    .filter(Boolean)
}

function mergeTagList(baseList, incomingList) {
  const nextList = [...baseList]
  let addedCount = 0
  let duplicateCount = 0
  let overflow = false
  incomingList.forEach((tag) => {
    if (nextList.includes(tag)) {
      duplicateCount += 1
      return
    }
    const nextCandidate = [...nextList, tag]
    if (nextCandidate.join(',').length > 300) {
      overflow = true
      return
    }
    nextList.push(tag)
    addedCount += 1
  })
  return {
    nextList,
    addedCount,
    duplicateCount,
    overflow,
  }
}

function syncTagListToForm() {
  createForm.tags = tagList.value.join(',')
}

function setTagListFromText(rawValue) {
  const parsedTags = parseTagTokens(rawValue)
  const { nextList } = mergeTagList([], parsedTags)
  tagList.value = nextList
  syncTagListToForm()
  tagInputValue.value = ''
}

function handleTagInputEnter() {
  const { nextList, addedCount, duplicateCount, overflow } = mergeTagList(
    tagList.value,
    parseTagTokens(tagInputValue.value),
  )
  tagList.value = nextList
  syncTagListToForm()
  if (overflow) {
    ElMessage.warning('标签总长度最多 300 个字符')
  } else if (addedCount === 0 && duplicateCount > 0) {
    ElMessage.info('该标签已存在')
  }
  tagInputValue.value = ''
}

function removeTag(index) {
  if (!Number.isInteger(index) || index < 0 || index >= tagList.value.length) {
    return
  }
  tagList.value.splice(index, 1)
  syncTagListToForm()
}

function toVideoIdText(value) {
  return String(value || '').trim()
}

function hasInteractionFlag(interaction, flag) {
  return String(interaction || '').includes(flag)
}

function buildInteractionFromFlags(closeComment, closeDanmu) {
  let interaction = ''
  if (closeComment) {
    interaction += '0'
  }
  if (closeDanmu) {
    interaction += '1'
  }
  return interaction
}

function getParentId(item) {
  return Number(item?.pcategoryId ?? item?.pCategoryId ?? 0)
}

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

function createPartItem() {
  return {
    localId: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
    fileId: '',
    fileName: '',
    uploadId: '',
    progress: 0,
    status: 'empty',
    error: '',
    uploading: false,
    fileSize: 0,
  }
}

async function loadCategories() {
  categoryLoading.value = true
  try {
    const data = await loadAllCategory()
    categories.value = Array.isArray(data) ? data : []
  } catch (_error) {
    categories.value = []
  } finally {
    categoryLoading.value = false
  }
}

async function loadSystemSetting() {
  settingLoading.value = true
  try {
    const data = await getSystemSetting()
    const serverPartCount = Number(data?.videoPCount)
    const serverVideoSize = Number(data?.videoSize)
    maxPartCount.value = Number.isFinite(serverPartCount)
      ? Math.max(1, Math.min(serverPartCount, DEFAULT_MAX_PART_COUNT))
      : DEFAULT_MAX_PART_COUNT
    maxVideoSizeMb.value = Number.isFinite(serverVideoSize)
      ? Math.max(1, serverVideoSize)
      : DEFAULT_MAX_VIDEO_SIZE_MB
  } catch (_error) {
    maxPartCount.value = DEFAULT_MAX_PART_COUNT
    maxVideoSizeMb.value = DEFAULT_MAX_VIDEO_SIZE_MB
  } finally {
    settingLoading.value = false
  }
}

async function loadPostCount() {
  if (!authStore.isLoggedIn) {
    return
  }

  postCountLoading.value = true
  try {
    const data = await getVideoCountInfo()
    postCountInfo.inProgress = Number(data?.inProgress || 0)
    postCountInfo.auditPassCount = Number(data?.auditPassCount || 0)
    postCountInfo.auditFailCount = Number(data?.auditFailCount || 0)
  } catch (_error) {
    postCountInfo.inProgress = 0
    postCountInfo.auditPassCount = 0
    postCountInfo.auditFailCount = 0
  } finally {
    postCountLoading.value = false
  }
}

async function loadPostList() {
  if (!authStore.isLoggedIn) {
    postList.value = []
    postPagination.totalCount = 0
    resetPostActionStateMap()
    return
  }

  postListLoading.value = true
  try {
    const data = await loadVideoPostList({
      status: activePostStatus.value.status,
      pageNo: postPagination.pageNo,
      pageSize: postPagination.pageSize,
    })

    postList.value = Array.isArray(data?.list) ? data.list : []
    postPagination.totalCount = Number(data?.totalCount || 0)
    postPagination.pageNo = Number(data?.pageNo || postPagination.pageNo)
    postPagination.pageSize = Number(data?.pageSize || postPagination.pageSize)
    prunePostActionStateMap(postList.value)
  } catch (_error) {
    postList.value = []
    postPagination.totalCount = 0
    resetPostActionStateMap()
  } finally {
    postListLoading.value = false
  }
}

async function refreshPostData() {
  await Promise.all([loadPostCount(), loadPostList()])
}

function resetInteractionState() {
  interactionTabKey.value = 'comment'
  interactionVideoList.value = []
  interactionSelectedVideoId.value = ''
  interactionCommentList.value = []
  interactionDanmuList.value = []
  interactionPagination.pageNo = 1
  interactionPagination.pageSize = INTERACTION_PAGE_SIZE
  interactionPagination.totalCount = 0
  interactionPagination.pageTotal = 0
  Object.keys(interactionDeletingMap).forEach((key) => {
    delete interactionDeletingMap[key]
  })
}

function normalizePaginationNumber(value, fallback) {
  const parsed = Number(value)
  if (!Number.isFinite(parsed)) {
    return fallback
  }
  return Math.max(0, Math.floor(parsed))
}

function normalizeInteractionVideo(item = {}) {
  return {
    videoId: toVideoIdText(item.videoId),
    videoCover: String(item.videoCover || '').trim(),
    videoName: String(item.videoName || '').trim(),
    lastUpdateTime: String(item.lastUpdateTime || '').trim(),
  }
}

function normalizeInteractionComment(item = {}) {
  return {
    commentId: Number(item.commentId || 0),
    videoId: toVideoIdText(item.videoId),
    videoCover: String(item.videoCover || '').trim(),
    videoName: String(item.videoName || '').trim(),
    content: String(item.content || '').trim(),
    imgPath: String(item.imgPath || '').trim(),
    userId: String(item.userId || '').trim(),
    nickName: String(item.nickName || '').trim(),
    avatar: String(item.avatar || '').trim(),
    replyNickName: String(item.replyNickName || '').trim(),
    likeCount: normalizeMetricCount(item.likeCount),
    hateCount: normalizeMetricCount(item.hateCount),
    postTime: String(item.postTime || '').trim(),
  }
}

function normalizeInteractionDanmu(item = {}) {
  return {
    danmuId: Number(item.danmuId || 0),
    videoId: toVideoIdText(item.videoId),
    videoCover: String(item.videoCover || '').trim(),
    videoName: String(item.videoName || '').trim(),
    userId: String(item.userId || '').trim(),
    nickName: String(item.nickName || '').trim(),
    text: String(item.text || '').trim(),
    mode: normalizePaginationNumber(item.mode, 0),
    color: String(item.color || '').trim(),
    time: normalizePaginationNumber(item.time, 0),
    postTime: String(item.postTime || '').trim(),
  }
}

async function loadInteractionVideos() {
  if (!authStore.isLoggedIn) {
    interactionVideoList.value = []
    interactionSelectedVideoId.value = ''
    return
  }

  interactionVideoLoading.value = true
  try {
    const data = await loadInteractionVideoList()
    const nextList = Array.isArray(data) ? data.map((item) => normalizeInteractionVideo(item)) : []
    interactionVideoList.value = nextList.filter((item) => Boolean(item.videoId))
    if (
      interactionSelectedVideoId.value &&
      !interactionVideoList.value.some((item) => item.videoId === interactionSelectedVideoId.value)
    ) {
      interactionSelectedVideoId.value = ''
    }
  } catch (_error) {
    interactionVideoList.value = []
    interactionSelectedVideoId.value = ''
  } finally {
    interactionVideoLoading.value = false
  }
}

async function loadInteractionList() {
  if (!authStore.isLoggedIn) {
    interactionCommentList.value = []
    interactionDanmuList.value = []
    interactionPagination.totalCount = 0
    interactionPagination.pageTotal = 0
    return
  }

  interactionListLoading.value = true
  try {
    const params = {
      pageNo: Math.max(1, Number(interactionPagination.pageNo || 1)),
    }
    if (interactionSelectedVideoId.value) {
      params.videoId = interactionSelectedVideoId.value
    }
    const data = interactionTabKey.value === 'danmu'
      ? await loadInteractionDanmuList(params)
      : await loadInteractionCommentList(params)
    const payload = data && typeof data === 'object' ? data : {}
    const rawList = Array.isArray(payload.list) ? payload.list : []

    interactionPagination.pageNo = Math.max(
      1,
      normalizePaginationNumber(payload.pageNo, interactionPagination.pageNo || 1),
    )
    interactionPagination.pageSize = Math.max(
      1,
      normalizePaginationNumber(payload.pageSize, interactionPagination.pageSize || INTERACTION_PAGE_SIZE),
    )
    interactionPagination.totalCount = Math.max(
      0,
      normalizePaginationNumber(payload.totalCount, rawList.length),
    )
    interactionPagination.pageTotal = Math.max(
      0,
      normalizePaginationNumber(
        payload.pageTotal,
        Math.ceil(
          interactionPagination.pageSize > 0
            ? interactionPagination.totalCount / interactionPagination.pageSize
            : 0,
        ),
      ),
    )

    if (interactionTabKey.value === 'danmu') {
      interactionDanmuList.value = rawList.map((item) => normalizeInteractionDanmu(item))
      interactionCommentList.value = []
    } else {
      interactionCommentList.value = rawList.map((item) => normalizeInteractionComment(item))
      interactionDanmuList.value = []
    }
  } catch (_error) {
    interactionCommentList.value = []
    interactionDanmuList.value = []
    interactionPagination.totalCount = 0
    interactionPagination.pageTotal = 0
  } finally {
    interactionListLoading.value = false
  }
}

async function refreshInteractionData({ resetPage = false } = {}) {
  if (resetPage) {
    interactionPagination.pageNo = 1
  }
  await loadInteractionVideos()
  await loadInteractionList()
}

function handleInteractionTabChange(tabKey) {
  const nextTab = String(tabKey || '')
  if (!interactionTabs.some((item) => item.key === nextTab) || nextTab === interactionTabKey.value) {
    return
  }
  interactionTabKey.value = nextTab
  interactionPagination.pageNo = 1
  loadInteractionList()
}

function handleInteractionVideoChange(videoId) {
  interactionSelectedVideoId.value = toVideoIdText(videoId)
  interactionPagination.pageNo = 1
  loadInteractionList()
}

function handleInteractionPageNoChange(pageNo) {
  interactionPagination.pageNo = Math.max(1, Number(pageNo || 1))
  loadInteractionList()
}

function isInteractionDeleting(type, targetId) {
  return Boolean(interactionDeletingMap[`${type}:${targetId}`])
}

function getInteractionVideoName(item = {}) {
  return String(item?.videoName || '').trim() || '未命名视频'
}

function getInteractionUserName(item = {}) {
  return String(item?.nickName || '').trim() || String(item?.userId || '').trim() || '匿名用户'
}

function formatDateTime(value) {
  if (!value) {
    return '--'
  }
  const text = String(value).trim()
  if (!text) {
    return '--'
  }
  if (/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/.test(text)) {
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

function formatDanmuTime(secondValue) {
  const totalSeconds = normalizePaginationNumber(secondValue, 0)
  const minute = String(Math.floor(totalSeconds / 60)).padStart(2, '0')
  const second = String(totalSeconds % 60).padStart(2, '0')
  return `${minute}:${second}`
}

function formatDanmuMode(modeValue) {
  const mode = normalizePaginationNumber(modeValue, 0)
  const modeMap = {
    0: '滚动',
    1: '顶部',
    2: '底部',
    3: '逆向',
  }
  return modeMap[mode] || `模式${mode}`
}

function openVideoPlayer(videoId) {
  const targetVideoId = toVideoIdText(videoId)
  if (!targetVideoId) {
    return
  }
  router.push({
    name: 'video-play',
    params: {
      videoId: targetVideoId,
    },
  })
}

async function refreshInteractionAfterDelete() {
  if (interactionActiveList.value.length === 1 && interactionPagination.pageNo > 1) {
    interactionPagination.pageNo -= 1
  }
  await refreshInteractionData()
}

async function handleInteractionCommentDelete(row) {
  const commentId = Number(row?.commentId || 0)
  if (!commentId) {
    ElMessage.warning('未找到评论ID，暂无法删除')
    return
  }
  try {
    await ElMessageBox.confirm('删除评论后不可恢复，是否继续？', '删除评论', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      confirmButtonClass: 'el-button--danger',
    })
  } catch (_error) {
    return
  }

  const loadingKey = `comment:${commentId}`
  interactionDeletingMap[loadingKey] = true
  try {
    await deleteInteractionComment({ commentId })
    ElMessage.success('评论已删除')
    await refreshInteractionAfterDelete()
  } catch (_error) {
    ElMessage.error('删除评论失败，请稍后重试')
  } finally {
    interactionDeletingMap[loadingKey] = false
  }
}

async function handleInteractionDanmuDelete(row) {
  const danmuId = Number(row?.danmuId || 0)
  if (!danmuId) {
    ElMessage.warning('未找到弹幕ID，暂无法删除')
    return
  }
  try {
    await ElMessageBox.confirm('删除弹幕后不可恢复，是否继续？', '删除弹幕', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      confirmButtonClass: 'el-button--danger',
    })
  } catch (_error) {
    return
  }

  const loadingKey = `danmu:${danmuId}`
  interactionDeletingMap[loadingKey] = true
  try {
    await deleteInteractionDanmu({ danmuId })
    ElMessage.success('弹幕已删除')
    await refreshInteractionAfterDelete()
  } catch (_error) {
    ElMessage.error('删除弹幕失败，请稍后重试')
  } finally {
    interactionDeletingMap[loadingKey] = false
  }
}

function getStatusCount(tab) {
  return Number(postStatusCountMap.value[tab.countKey] || 0)
}

function normalizeMetricCount(value) {
  const numericValue = Number(value)
  if (!Number.isFinite(numericValue) || numericValue < 0) {
    return 0
  }
  return Math.floor(numericValue)
}

function getPostMetricValue(row, key) {
  return normalizeMetricCount(row?.[key])
}

function formatPostDate(value) {
  if (!value) {
    return '--'
  }

  const rawText = String(value).trim()
  if (!rawText) {
    return '--'
  }

  const firstSegment = rawText.includes('T') ? rawText.split('T')[0] : rawText.split(' ')[0]
  if (/^\d{4}-\d{2}-\d{2}$/.test(firstSegment)) {
    return firstSegment
  }

  const parsedDate = new Date(rawText)
  if (Number.isNaN(parsedDate.getTime())) {
    return rawText
  }

  const year = parsedDate.getFullYear()
  const month = String(parsedDate.getMonth() + 1).padStart(2, '0')
  const day = String(parsedDate.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function getPostStatusClass(statusValue) {
  const status = Number(statusValue)
  if (status === 0 || status === 2) {
    return 'pending'
  }
  if (status === 3) {
    return 'pass'
  }
  if (status === 1 || status === 4) {
    return 'fail'
  }
  return 'default'
}

function isPostEditDisabled(row) {
  const status = Number(row?.status)
  if (NON_EDITABLE_POST_STATUS.has(status)) {
    return true
  }
  const statusName = String(row?.statusName || '')
  return statusName.includes('转码') || statusName.includes('待审核')
}

function getPostEditButtonTitle(row) {
  return isPostEditDisabled(row) ? '待审核或转码中不可编辑' : '编辑稿件'
}

function resetPostActionStateMap() {
  ;[
    postMoreMenuVisibleMap,
    postInteractionStateMap,
    postInteractionSavingMap,
    postDeletingMap,
  ].forEach((targetMap) => {
    Object.keys(targetMap).forEach((key) => {
      delete targetMap[key]
    })
  })
}

function prunePostActionStateMap(list) {
  const videoIdSet = new Set(
    (Array.isArray(list) ? list : [])
      .map((item) => toVideoIdText(item?.videoId))
      .filter(Boolean),
  )
  ;[
    postMoreMenuVisibleMap,
    postInteractionStateMap,
    postInteractionSavingMap,
    postDeletingMap,
  ].forEach((targetMap) => {
    Object.keys(targetMap).forEach((key) => {
      if (!videoIdSet.has(key)) {
        delete targetMap[key]
      }
    })
  })
}

function getPostInteractionState(row) {
  const videoId = toVideoIdText(row?.videoId)
  if (!videoId) {
    return { closeComment: false, closeDanmu: false }
  }
  if (!postInteractionStateMap[videoId]) {
    postInteractionStateMap[videoId] = {
      closeComment: hasInteractionFlag(row?.interaction, '0'),
      closeDanmu: hasInteractionFlag(row?.interaction, '1'),
    }
  }
  return postInteractionStateMap[videoId]
}

function onPostMoreMenuShow(row) {
  const videoId = toVideoIdText(row?.videoId)
  if (!videoId) {
    return
  }
  postMoreMenuVisibleMap[videoId] = true
  postInteractionStateMap[videoId] = {
    closeComment: hasInteractionFlag(row?.interaction, '0'),
    closeDanmu: hasInteractionFlag(row?.interaction, '1'),
  }
}

function onPostMoreMenuHide(row) {
  const videoId = toVideoIdText(row?.videoId)
  if (!videoId) {
    return
  }
  postMoreMenuVisibleMap[videoId] = false
}

function isPostInteractionSaving(row) {
  const videoId = toVideoIdText(row?.videoId)
  return Boolean(videoId && postInteractionSavingMap[videoId])
}

function isPostDeleting(row) {
  const videoId = toVideoIdText(row?.videoId)
  return Boolean(videoId && postDeletingMap[videoId])
}

async function savePostInteraction(row, nextState) {
  const videoId = toVideoIdText(row?.videoId)
  if (!videoId) {
    ElMessage.warning('未找到稿件ID，暂无法保存互动设置')
    return false
  }
  postInteractionSavingMap[videoId] = true
  try {
    const interaction = buildInteractionFromFlags(nextState.closeComment, nextState.closeDanmu)
    await saveVideoInteraction({
      videoId,
      interaction,
    })
    row.interaction = interaction
    postInteractionStateMap[videoId] = {
      closeComment: nextState.closeComment,
      closeDanmu: nextState.closeDanmu,
    }
    ElMessage.success('互动设置已保存')
    return true
  } catch (_error) {
    ElMessage.error('互动设置保存失败，请重试')
    return false
  } finally {
    postInteractionSavingMap[videoId] = false
  }
}

async function onPostInteractionToggle(row, key, checkedValue) {
  const videoId = toVideoIdText(row?.videoId)
  if (!videoId) {
    ElMessage.warning('未找到稿件ID，暂无法保存互动设置')
    return
  }
  const currentState = getPostInteractionState(row)
  const previousState = {
    closeComment: currentState.closeComment,
    closeDanmu: currentState.closeDanmu,
  }
  const nextState = {
    closeComment: key === 'closeComment' ? Boolean(checkedValue) : previousState.closeComment,
    closeDanmu: key === 'closeDanmu' ? Boolean(checkedValue) : previousState.closeDanmu,
  }
  postInteractionStateMap[videoId] = nextState
  const success = await savePostInteraction(row, nextState)
  if (!success) {
    postInteractionStateMap[videoId] = previousState
  }
}

async function handlePostDelete(row) {
  const videoId = toVideoIdText(row?.videoId)
  if (!videoId) {
    ElMessage.warning('未找到稿件ID，暂无法删除')
    return
  }
  try {
    await ElMessageBox.confirm('删除后不可恢复，是否继续？', '删除稿件', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      confirmButtonClass: 'el-button--danger',
    })
  } catch (_error) {
    return
  }
  postDeletingMap[videoId] = true
  try {
    await deleteVideo({ videoId })
    ElMessage.success('稿件已删除')
    delete postMoreMenuVisibleMap[videoId]
    delete postInteractionStateMap[videoId]
    delete postInteractionSavingMap[videoId]
    delete postDeletingMap[videoId]
    if (postList.value.length === 1 && postPagination.pageNo > 1) {
      postPagination.pageNo -= 1
    }
    await refreshPostData()
  } catch (_error) {
    ElMessage.error('删除失败，请稍后重试')
  } finally {
    if (videoId in postDeletingMap) {
      postDeletingMap[videoId] = false
    }
  }
}

function resetEditLockedMeta() {
  editLockedMeta.pCategoryId = null
  editLockedMeta.categoryId = null
  editLockedMeta.interaction = ''
}

function resetEditContext() {
  editRequestId += 1
  editMode.value = 'create'
  editingVideoId.value = ''
  editLoading.value = false
  editError.value = ''
  resetEditLockedMeta()
}

function applyInteractionValueToForm(value) {
  const text = String(value || '')
  interactionState.closeComment = text.includes('0')
  interactionState.closeDanmu = text.includes('1')
}

function createEditPartItem(item, index) {
  const data = item && typeof item === 'object' ? item : {}
  const fileId = String(data.fileId || '').trim()
  const uploadId = String(data.uploadId || '').trim()
  return {
    localId: `${Date.now()}-${index}-${Math.random().toString(16).slice(2)}`,
    fileId,
    fileName: String(data.fileName || `P${index + 1}`).trim(),
    uploadId,
    progress: 100,
    status: 'success',
    error: '',
    uploading: false,
    fileSize: Math.max(0, Number(data.fileSize || 0)),
  }
}

async function loadEditVideoDetail(videoId) {
  const targetVideoId = normalizeEditVideoId(videoId)
  if (!targetVideoId) {
    return
  }
  const requestId = ++editRequestId
  editLoading.value = true
  editError.value = ''
  try {
    const payload = await getVideoByVideoId({ videoId: targetVideoId })
    if (requestId !== editRequestId) {
      return
    }
    const videoInfo = payload && typeof payload === 'object' ? payload.videoInfo : null
    const filePosts = Array.isArray(payload?.videoInfoFilePosts) ? payload.videoInfoFilePosts : []
    const videoData = videoInfo && typeof videoInfo === 'object' ? videoInfo : {}
    const resolvedVideoId = normalizeEditVideoId(videoData.videoId || targetVideoId)
    if (!resolvedVideoId) {
      throw new Error('invalid video id')
    }

    const sortedFilePosts = [...filePosts].sort((a, b) => {
      return Number(a?.fileIndex || 0) - Number(b?.fileIndex || 0)
    })

    createForm.videoCover = String(videoData.videoCover || '').trim()
    createForm.videoName = String(videoData.videoName || '').trim()
    const parentCategoryId = videoData.pCategoryId ?? videoData.pcategoryId
    const childCategoryId = videoData.categoryId
    editLockedMeta.pCategoryId = toNullableNumber(parentCategoryId)
    editLockedMeta.categoryId = toNullableNumber(childCategoryId)
    editLockedMeta.interaction = String(videoData.interaction ?? '')
    hydratingEditCategory.value = true
    try {
      createForm.pCategoryId = parentCategoryId === null || parentCategoryId === undefined
        ? ''
        : String(parentCategoryId)
      createForm.categoryId = childCategoryId === null || childCategoryId === undefined
        ? ''
        : String(childCategoryId)
      await nextTick()
    } finally {
      hydratingEditCategory.value = false
    }
    createForm.postType = Number(videoData.postType ?? 0)
    setTagListFromText(videoData.tags)
    createForm.introduction = String(videoData.introduction || '').trim()
    applyInteractionValueToForm(videoData.interaction)
    videoParts.value = sortedFilePosts.map((item, index) => createEditPartItem(item, index))

    editingVideoId.value = resolvedVideoId
    editMode.value = 'edit'
    editError.value = ''
  } catch (_error) {
    if (requestId !== editRequestId) {
      return
    }
    editError.value = '稿件详情加载失败，请稍后重试'
    ElMessage.error(editError.value)
    resetPostForm()
    resetEditContext()
    switchSection('content')
  } finally {
    if (requestId === editRequestId) {
      editLoading.value = false
    }
  }
}

function handlePostEdit(row) {
  if (isPostEditDisabled(row)) {
    return
  }
  const videoId = normalizeEditVideoId(row?.videoId)
  if (!videoId) {
    ElMessage.warning('未找到稿件ID，暂无法编辑')
    return
  }
  switchSection('create', { editVideoId: videoId })
}

function handleStatusTabChange(statusKey) {
  if (postStatusKey.value === statusKey) {
    return
  }
  postStatusKey.value = statusKey
  postPagination.pageNo = 1
  loadPostList()
}

function handlePostPageNoChange(pageNo) {
  postPagination.pageNo = pageNo
  loadPostList()
}

function handlePostPageSizeChange(pageSize) {
  postPagination.pageSize = pageSize
  postPagination.pageNo = 1
  loadPostList()
}

function switchSection(section, options = {}) {
  const next = normalizeSection(section)
  const editVideoId = normalizeEditVideoId(options.editVideoId)

  if (next !== 'create' && editMode.value === 'edit') {
    resetPostForm()
    resetEditContext()
  } else if (next === 'create' && !editVideoId && editMode.value === 'edit') {
    resetPostForm()
    resetEditContext()
  }

  activeSection.value = next
  const nextQuery = { ...route.query }
  if (next === 'home') {
    delete nextQuery.tab
  } else {
    nextQuery.tab = next
  }
  if (next === 'create' && editVideoId) {
    nextQuery.editVideoId = editVideoId
  } else {
    delete nextQuery.editVideoId
  }
  router.replace({ query: nextQuery })
}

function goHome() {
  router.push('/')
}

function checkAuthBeforeUpload() {
  if (authStore.isLoggedIn) {
    return true
  }
  authStore.openAuthDialog('login')
  return false
}

function isLegacyEditPart(part) {
  return isEditMode.value && Boolean(String(part?.fileId || '').trim())
}

function canReplacePart(part) {
  if (!part) {
    return false
  }
  if (!isEditMode.value) {
    return true
  }
  return !isLegacyEditPart(part)
}

function getReplacePartTitle(part) {
  if (part?.uploading) {
    return '上传中，请稍后'
  }
  if (!canReplacePart(part)) {
    return '旧文件不可替换，可新增分P后删除旧分P'
  }
  return '重新上传当前分P'
}

function triggerVideoPicker(mode = 'add', index = null) {
  if (mode === 'add' && reachedPartLimit.value) {
    ElMessage.warning(`最多添加 ${maxPartCount.value} 个分P`)
    return
  }
  if (mode === 'replace' && Number.isInteger(index)) {
    const targetPart = videoParts.value[index]
    if (!targetPart || targetPart.uploading) {
      return
    }
    if (!canReplacePart(targetPart)) {
      ElMessage.warning('旧文件不可替换，可新增分P后删除旧分P')
      return
    }
  }
  if (!checkAuthBeforeUpload()) {
    return
  }
  fileSelectTarget.value = { mode, index }
  if (videoInputRef.value) {
    videoInputRef.value.value = ''
    videoInputRef.value.click()
  }
}

function triggerCoverPicker() {
  if (!checkAuthBeforeUpload()) {
    return
  }
  if (coverInputRef.value) {
    coverInputRef.value.value = ''
    coverInputRef.value.click()
  }
}

function isVideoFile(file) {
  const name = String(file?.name || '').toLowerCase()
  return file?.type?.startsWith('video/') || /\.(mp4|mov|mkv|avi|webm|m4v)$/.test(name)
}

function stripFileExtension(fileName) {
  return fileName.replace(/\.[^.]+$/, '')
}

function formatFileSize(size) {
  const value = Number(size || 0)
  if (value < 1024) {
    return `${value} B`
  }
  if (value < 1024 * 1024) {
    return `${(value / 1024).toFixed(1)} KB`
  }
  if (value < 1024 * 1024 * 1024) {
    return `${(value / 1024 / 1024).toFixed(1)} MB`
  }
  return `${(value / 1024 / 1024 / 1024).toFixed(1)} GB`
}

async function handleVideoInputChange(event) {
  const input = event.target
  const file = input.files?.[0]
  input.value = ''
  if (!file) {
    return
  }
  if (!isVideoFile(file)) {
    ElMessage.error('请上传视频文件')
    return
  }
  if (file.size > maxVideoSizeMb.value * 1024 * 1024) {
    ElMessage.error(`单个视频请控制在 ${maxVideoSizeMb.value}MB 以内`)
    return
  }

  const target = fileSelectTarget.value
  if (target.mode === 'replace' && Number.isInteger(target.index)) {
    await startUploadPart(target.index, file, true)
    return
  }

  if (reachedPartLimit.value) {
    ElMessage.warning(`最多添加 ${maxPartCount.value} 个分P`)
    return
  }

  videoParts.value.push(createPartItem())
  await startUploadPart(videoParts.value.length - 1, file, false)
}

async function startUploadPart(index, file, replacing) {
  const part = videoParts.value[index]
  if (!part || part.uploading) {
    return
  }

  const oldUploadId = part.uploadId
  part.fileName = file.name
  part.fileSize = file.size
  part.progress = 0
  part.status = 'uploading'
  part.error = ''
  part.fileId = ''
  part.uploadId = ''
  part.uploading = true

  try {
    if (replacing && oldUploadId) {
      await safeDeleteUploadTask(oldUploadId)
    }

    const totalChunks = Math.max(1, Math.ceil(file.size / CHUNK_SIZE))
    const uploadIdPayload = await preUploadVideo({
      fileName: file.name,
      chunks: totalChunks,
    })
    const uploadId = resolveUploadId(uploadIdPayload)
    if (!uploadId) {
      throw new Error('No uploadId returned')
    }

    for (let chunkIndex = 0; chunkIndex < totalChunks; chunkIndex += 1) {
      const start = chunkIndex * CHUNK_SIZE
      const end = Math.min(start + CHUNK_SIZE, file.size)
      const chunkFile = file.slice(start, end)

      await uploadVideo({
        uploadId,
        chunkIndex,
        chunkFile,
        onUploadProgress: (progressEvent) => {
          const loaded = Number(progressEvent?.loaded ?? 0)
          const total = Number(progressEvent?.total ?? chunkFile.size ?? 1)
          const chunkProgress = total > 0 ? loaded / total : 0
          const overall = ((chunkIndex + chunkProgress) / totalChunks) * 100
          part.progress = Math.max(part.progress, Math.min(99, Math.round(overall)))
        },
      })

      part.progress = Math.round(((chunkIndex + 1) / totalChunks) * 100)
    }

    part.uploadId = uploadId
    part.status = 'success'
    part.error = ''
    part.progress = 100
    if (!createForm.videoName.trim()) {
      createForm.videoName = stripFileExtension(file.name)
    }
  } catch (_error) {
    part.uploadId = ''
    part.status = 'error'
    part.error = '上传失败，请重新上传'
    part.progress = 0
  } finally {
    part.uploading = false
  }
}

function resolveUploadId(payload) {
  if (typeof payload === 'string') {
    return payload
  }
  if (payload && typeof payload === 'object') {
    return payload.uploadId || payload.id || ''
  }
  return ''
}

async function safeDeleteUploadTask(uploadId) {
  try {
    await deleteUploadVideo(uploadId)
  } catch (_error) {
    // Keep UI flow smooth even when cleanup call fails.
  }
}

function shouldDeleteUploadContextOnRemove(part) {
  const uploadId = String(part?.uploadId || '').trim()
  if (!uploadId) {
    return false
  }
  if (!isEditMode.value) {
    return true
  }
  // Old persisted parts in edit mode are not pre-upload contexts.
  return !isLegacyEditPart(part)
}

async function removePart(index) {
  const part = videoParts.value[index]
  if (!part || part.uploading) {
    return
  }
  if (shouldDeleteUploadContextOnRemove(part)) {
    await safeDeleteUploadTask(part.uploadId)
  }
  videoParts.value.splice(index, 1)
}

function movePart(index, direction) {
  const targetIndex = index + direction
  if (targetIndex < 0 || targetIndex >= videoParts.value.length) {
    return
  }
  const nextList = [...videoParts.value]
  ;[nextList[index], nextList[targetIndex]] = [nextList[targetIndex], nextList[index]]
  videoParts.value = nextList
}

async function handleCoverInputChange(event) {
  const input = event.target
  const file = input.files?.[0]
  input.value = ''
  if (!file) {
    return
  }
  if (!file.type?.startsWith('image/')) {
    ElMessage.error('请上传图片作为封面')
    return
  }

  coverUploading.value = true
  try {
    const result = await uploadImage(file, false)
    if (!result || typeof result !== 'string') {
      throw new Error('Cover upload failed')
    }
    createForm.videoCover = result
    ElMessage.success('封面上传成功')
  } catch (_error) {
    ElMessage.error('封面上传失败，请重试')
  } finally {
    coverUploading.value = false
  }
}

function resetPostForm() {
  videoParts.value = []
  createForm.videoCover = ''
  createForm.videoName = ''
  createForm.pCategoryId = parentCategories.value.length > 0
    ? String(parentCategories.value[0].categoryId)
    : ''
  createForm.categoryId = ''
  createForm.postType = 0
  setTagListFromText('')
  createForm.introduction = ''
  interactionState.closeComment = false
  interactionState.closeDanmu = false
  resetEditLockedMeta()
}

async function submitPost() {
  if (!canSubmit.value) {
    ElMessage.warning('请先完成分P上传并填写必填项')
    return
  }
  if (!checkAuthBeforeUpload()) {
    return
  }

  submitting.value = true
  try {
    const editing = isEditMode.value
    const requestPCategoryId = editing
      ? (editLockedMeta.pCategoryId ?? Number(createForm.pCategoryId))
      : Number(createForm.pCategoryId)
    const requestCategoryId = editing
      ? (editLockedMeta.categoryId === null ? undefined : editLockedMeta.categoryId)
      : (createForm.categoryId ? Number(createForm.categoryId) : undefined)
    const requestInteraction = editing ? editLockedMeta.interaction : interactionValue.value
    await postVideo({
      videoId: editing ? editingVideoId.value : undefined,
      videoCover: createForm.videoCover.trim(),
      videoName: createForm.videoName.trim(),
      pCategoryId: requestPCategoryId,
      categoryId: requestCategoryId,
      postType: Number(createForm.postType),
      tags: createForm.tags.trim(),
      introduction: createForm.introduction.trim(),
      interaction: requestInteraction,
      uploadFileList: JSON.stringify(
        videoParts.value.map((item) => ({
          fileId: item.fileId || null,
          uploadId: item.uploadId || null,
          fileName: item.fileName,
        })),
      ),
    })
    if (editing) {
      ElMessage.success('稿件保存成功')
      resetPostForm()
      resetEditContext()
      switchSection('content')
      await refreshPostData()
    } else {
      ElMessage.success('投稿提交成功')
      resetPostForm()
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="creator-page">
    <div class="creator-layout">
      <aside class="panel creator-sidebar">
        <div class="creator-brand">
          <strong>Streama</strong>
          <span>Creator Studio</span>
        </div>

        <button class="home-trigger" type="button" @click="goHome">返回首页</button>

        <div class="sidebar-nav" role="tablist" aria-label="创建中心导航">
          <button
            v-for="item in centerNavItems"
            :key="item.key"
            class="sidebar-item"
            :class="{ active: activeSection === item.key, 'create-entry': item.key === 'create' }"
            type="button"
            @click="switchSection(item.key)"
          >
            {{ item.label }}
          </button>
        </div>
      </aside>

      <main class="panel creator-main">
        <div class="main-banner">
          <div class="banner-left">
            <p class="banner-kicker">CREATOR CENTER</p>
            <h1>{{ activeSectionLabel }}</h1>
            <p class="banner-desc">{{ activeSectionHint }}</p>
          </div>
          <div class="banner-chip">{{ activeSection === 'create' ? '投稿工作台' : activeSectionLabel }}</div>
        </div>

        <section v-if="activeSection === 'create'" v-loading="editLoading" class="create-panel">
          <div class="create-head">
            <div>
              <h2>{{ createPanelTitle }}</h2>
              <p>{{ createPanelDesc }}</p>
            </div>
            <div class="head-meta">
              <span v-if="isEditMode">编辑中：{{ editingVideoId }}</span>
              <span>最多 {{ maxPartCount }} 个分P</span>
              <span v-if="settingLoading">设置加载中</span>
            </div>
          </div>

          <div v-if="videoParts.length === 0" class="first-upload-area">
            <button class="upload-drop-zone" type="button" @click="triggerVideoPicker('add')">
              <span class="upload-main">点击上传视频</span>
              <span class="upload-sub">虚线区域用于首个分P上传</span>
              <span class="upload-rule">单个视频最大 {{ maxVideoSizeMb }}MB，最多 {{ maxPartCount }} 个分P</span>
            </button>
          </div>

          <template v-else>
            <div class="part-toolbar">
              <p>分P管理（{{ videoParts.length }}/{{ maxPartCount }}）</p>
              <el-button type="primary" :disabled="reachedPartLimit || partUploading" @click="triggerVideoPicker('add')">
                添加分P
              </el-button>
            </div>

            <div class="part-list">
              <article
                v-for="(part, index) in videoParts"
                :key="part.localId"
                class="part-item"
                :class="[`status-${part.status}`]"
              >
                <div class="part-order">P{{ index + 1 }}</div>
                <div class="part-body">
                  <p class="part-name">{{ part.fileName || '等待上传视频文件' }}</p>
                  <p class="part-state">
                    <template v-if="part.uploading">上传中...</template>
                    <template v-else-if="part.status === 'success'">上传完成，可提交</template>
                    <template v-else-if="part.status === 'error'">{{ part.error }}</template>
                    <template v-else>未上传</template>
                  </p>
                  <el-progress
                    :percentage="part.progress"
                    :stroke-width="8"
                    :status="part.status === 'error' ? 'exception' : part.status === 'success' ? 'success' : ''"
                  />
                  <p v-if="part.fileSize" class="part-size">{{ formatFileSize(part.fileSize) }}</p>
                </div>
                <div class="part-actions">
                  <button
                    type="button"
                    :disabled="part.uploading || !canReplacePart(part)"
                    :title="getReplacePartTitle(part)"
                    @click="triggerVideoPicker('replace', index)"
                  >
                    重新上传
                  </button>
                  <button type="button" :disabled="index === 0 || part.uploading" @click="movePart(index, -1)">
                    上移
                  </button>
                  <button
                    type="button"
                    :disabled="index === videoParts.length - 1 || part.uploading"
                    @click="movePart(index, 1)"
                  >
                    下移
                  </button>
                  <button type="button" class="danger" :disabled="part.uploading" @click="removePart(index)">
                    删除
                  </button>
                </div>
              </article>
            </div>

            <el-divider />

            <div class="post-meta-block">
              <h3>投稿信息</h3>
              <p class="meta-subtitle">以下内容为投稿接口提交所需字段。</p>

              <div class="cover-uploader">
                <button class="cover-trigger" type="button" :disabled="coverUploading" @click="triggerCoverPicker">
                  <img v-if="coverPreviewUrl" :src="coverPreviewUrl" alt="视频封面预览" />
                  <span v-else>上传封面</span>
                </button>
                <div class="cover-tip">
                  <p>{{ coverUploading ? '封面上传中...' : '点击上传封面图片' }}</p>
                  <p>封面为必填项</p>
                </div>
              </div>

              <el-form label-position="top" class="meta-form">
                <div class="meta-grid">
                  <el-form-item label="稿件标题（必填）">
                    <el-input v-model="createForm.videoName" maxlength="100" show-word-limit />
                  </el-form-item>

                  <el-form-item label="标签（必填）">
                    <div class="tag-input-box">
                      <div v-if="tagList.length > 0" class="tag-list">
                        <el-tag
                          v-for="(tag, index) in tagList"
                          :key="`${tag}-${index}`"
                          closable
                          @close="removeTag(index)"
                        >
                          {{ tag }}
                        </el-tag>
                      </div>
                      <el-input
                        v-model="tagInputValue"
                        maxlength="30"
                        placeholder="输入后按 Enter 生成标签（可用逗号一次输入多个）"
                        @keydown.enter.prevent="handleTagInputEnter"
                      />
                      <p class="tag-helper">已添加 {{ tagList.length }} 个标签（总长度不超过 300）</p>
                    </div>
                  </el-form-item>

                  <el-form-item v-if="!isEditMode" label="主分区（必填）">
                    <el-select
                      v-model="createForm.pCategoryId"
                      :loading="categoryLoading || editLoading"
                      :disabled="editLoading"
                      placeholder="请选择主分区"
                      style="width: 100%"
                    >
                      <el-option
                        v-for="item in parentCategories"
                        :key="item.categoryId"
                        :label="item.categoryName"
                        :value="String(item.categoryId)"
                      />
                    </el-select>
                  </el-form-item>

                  <el-form-item v-if="!isEditMode" label="子分区（可选）">
                    <el-select
                      v-model="createForm.categoryId"
                      :loading="categoryLoading || editLoading"
                      :disabled="editLoading"
                      clearable
                      placeholder="可选子分区"
                      style="width: 100%"
                    >
                      <el-option
                        v-for="item in childCategories"
                        :key="item.categoryId"
                        :label="item.categoryName"
                        :value="String(item.categoryId)"
                      />
                    </el-select>
                  </el-form-item>
                </div>

                <el-form-item label="投稿类型">
                  <el-radio-group v-model="createForm.postType">
                    <el-radio :label="0">自制</el-radio>
                    <el-radio :label="1">转载</el-radio>
                  </el-radio-group>
                </el-form-item>

                <el-form-item v-if="!isEditMode" label="互动设置">
                  <div class="interaction-row">
                    <el-checkbox v-model="interactionState.closeComment">关闭评论区</el-checkbox>
                    <el-checkbox v-model="interactionState.closeDanmu">关闭弹幕</el-checkbox>
                  </div>
                </el-form-item>

                <el-form-item label="简介（可选）">
                  <el-input
                    v-model="createForm.introduction"
                    type="textarea"
                    :rows="4"
                    maxlength="2000"
                    show-word-limit
                    placeholder="填写投稿说明、补充信息等"
                  />
                </el-form-item>
              </el-form>

              <div class="submit-bar">
                <p v-if="!canSubmit">请先完成分P上传，并填写封面、标题、标签、主分区。</p>
                <el-button type="primary" :loading="submitting" :disabled="!canSubmit" @click="submitPost">
                  {{ submitButtonText }}
                </el-button>
              </div>
            </div>
          </template>
        </section>

        <section v-else-if="activeSection === 'content'" class="post-manage-panel">
          <div class="content-head">
            <h2>稿件管理</h2>
            <p>按状态查看已投稿的视频记录。</p>
          </div>

          <div v-if="!authStore.isLoggedIn" class="auth-empty">
            <p>登录后可查看你的稿件状态与数量统计。</p>
            <el-button type="primary" @click="authStore.openAuthDialog('login')">去登录</el-button>
          </div>

          <template v-else>
            <div class="status-tabs">
              <button
                v-for="item in postStatusTabs"
                :key="item.key"
                class="status-tab"
                :class="{ active: postStatusKey === item.key }"
                type="button"
                @click="handleStatusTabChange(item.key)"
              >
                <span>{{ item.label }}</span>
                <strong>{{ postCountLoading ? '--' : getStatusCount(item) }}</strong>
              </button>
            </div>

            <div v-loading="postListLoading" class="post-list-wrap">
              <div v-if="postList.length > 0" class="post-list">
                <article
                  v-for="(row, index) in postList"
                  :key="row.videoId || row.fileId || `${row.videoName || 'post'}-${index}`"
                  class="post-row"
                >
                  <div class="post-thumb-wrap">
                    <img
                      v-if="row.videoCover"
                      class="post-thumb"
                      :src="toResourceUrl(row.videoCover)"
                      alt="稿件封面"
                    />
                    <div v-else class="post-thumb post-thumb-empty">暂无封面</div>
                  </div>

                  <div class="post-content">
                    <div class="post-title-line">
                      <p class="post-title" :title="row.videoName || '未命名视频'">
                        {{ row.videoName || '未命名视频' }}
                      </p>
                      <span class="post-status" :class="getPostStatusClass(row.status)">
                        {{ row.statusName || '状态未知' }}
                      </span>
                    </div>

                    <p class="post-date">{{ formatPostDate(row.lastUpdateTime) }}</p>

                    <div class="post-metrics">
                      <span v-for="metric in postMetricConfigs" :key="metric.key" class="metric-item">
                        <IconFont :name="metric.icon" />
                        <strong>{{ getPostMetricValue(row, metric.key) }}</strong>
                      </span>
                    </div>
                  </div>

                  <div class="post-actions">
                    <button
                      class="post-edit-btn"
                      type="button"
                      :disabled="isPostEditDisabled(row)"
                      :title="getPostEditButtonTitle(row)"
                      @click="handlePostEdit(row)"
                    >
                      编辑
                    </button>
                    <el-popover
                      placement="bottom-end"
                      :width="220"
                      trigger="click"
                      popper-class="post-more-popover"
                      @show="onPostMoreMenuShow(row)"
                      @hide="onPostMoreMenuHide(row)"
                    >
                      <template #reference>
                        <button
                          class="post-more-btn"
                          type="button"
                          aria-label="更多操作"
                          :class="{ active: postMoreMenuVisibleMap[toVideoIdText(row.videoId)] }"
                        >
                          <span class="more-dots" aria-hidden="true">⋮</span>
                        </button>
                      </template>

                      <div class="post-more-menu" @click.stop>
                        <p class="post-more-title">互动设置</p>
                        <el-checkbox
                          :model-value="getPostInteractionState(row).closeComment"
                          :disabled="isPostInteractionSaving(row) || isPostDeleting(row)"
                          @change="(value) => onPostInteractionToggle(row, 'closeComment', value)"
                        >
                          关闭评论区
                        </el-checkbox>
                        <el-checkbox
                          :model-value="getPostInteractionState(row).closeDanmu"
                          :disabled="isPostInteractionSaving(row) || isPostDeleting(row)"
                          @change="(value) => onPostInteractionToggle(row, 'closeDanmu', value)"
                        >
                          关闭弹幕区
                        </el-checkbox>
                        <button
                          class="post-delete-action"
                          type="button"
                          :disabled="isPostDeleting(row)"
                          @click="handlePostDelete(row)"
                        >
                          {{ isPostDeleting(row) ? '删除中...' : '删除' }}
                        </button>
                      </div>
                    </el-popover>
                  </div>
                </article>
              </div>

              <el-empty v-else description="暂无稿件数据" :image-size="96" />
            </div>

            <div class="pager-wrap">
              <el-pagination
                :current-page="postPagination.pageNo"
                :page-size="postPagination.pageSize"
                :page-sizes="[15, 30, 50]"
                layout="total, sizes, prev, pager, next, jumper"
                :total="postPagination.totalCount"
                @current-change="handlePostPageNoChange"
                @size-change="handlePostPageSizeChange"
              />
            </div>
          </template>
        </section>

        <section v-else-if="activeSection === 'interaction'" class="interaction-panel">
          <div class="content-head">
            <h2>互动管理</h2>
            <p>先筛选视频，再管理评论与弹幕，支持快速删除。</p>
          </div>

          <div v-if="!authStore.isLoggedIn" class="auth-empty">
            <p>登录后可管理你的视频评论与弹幕。</p>
            <el-button type="primary" @click="authStore.openAuthDialog('login')">去登录</el-button>
          </div>

          <template v-else>
            <div class="interaction-toolbar">
              <div class="interaction-tabs">
                <button
                  v-for="tab in interactionTabs"
                  :key="tab.key"
                  class="interaction-tab"
                  :class="{ active: interactionTabKey === tab.key }"
                  type="button"
                  @click="handleInteractionTabChange(tab.key)"
                >
                  {{ tab.label }}
                </button>
              </div>

              <div class="interaction-filter">
                <el-select
                  v-model="interactionSelectedVideoId"
                  clearable
                  filterable
                  :loading="interactionVideoLoading"
                  placeholder="筛选视频（默认全部）"
                  style="width: 260px"
                  @change="handleInteractionVideoChange"
                >
                  <el-option label="全部视频" value="" />
                  <el-option
                    v-for="video in interactionVideoList"
                    :key="video.videoId"
                    :label="video.videoName || video.videoId"
                    :value="video.videoId"
                  />
                </el-select>
                <el-button :loading="interactionListLoading" @click="refreshInteractionData({ resetPage: false })">
                  刷新
                </el-button>
              </div>
            </div>

            <div v-loading="interactionListLoading" class="interaction-list-wrap">
              <div v-if="interactionActiveList.length > 0" class="interaction-list">
                <article
                  v-for="(row, index) in interactionActiveList"
                  :key="
                    interactionTabKey === 'danmu'
                      ? row.danmuId || `${row.videoId || 'video'}-${index}`
                      : row.commentId || `${row.videoId || 'video'}-${index}`
                  "
                  class="interaction-item"
                >
                  <div class="interaction-video">
                    <img
                      v-if="row.videoCover"
                      class="interaction-video-cover"
                      :src="toResourceUrl(row.videoCover)"
                      alt="视频封面"
                    />
                    <div v-else class="interaction-video-cover interaction-video-cover-empty">暂无封面</div>
                    <div class="interaction-video-meta">
                      <p class="interaction-video-title" :title="getInteractionVideoName(row)">
                        {{ getInteractionVideoName(row) }}
                      </p>
                      <p class="interaction-video-sub">视频ID：{{ row.videoId || '--' }}</p>
                      <button class="interaction-video-link" type="button" @click="openVideoPlayer(row.videoId)">
                        查看视频
                      </button>
                    </div>
                  </div>

                  <div class="interaction-body">
                    <template v-if="interactionTabKey === 'comment'">
                      <p class="interaction-content">
                        {{ row.content || '（该评论内容为空）' }}
                      </p>
                      <img
                        v-if="row.imgPath"
                        class="interaction-image"
                        :src="toResourceUrl(row.imgPath)"
                        alt="评论图片"
                      />
                      <div class="interaction-meta-row">
                        <span>用户：{{ getInteractionUserName(row) }}</span>
                        <span>点赞：{{ row.likeCount }}</span>
                        <span>点踩：{{ row.hateCount }}</span>
                        <span>时间：{{ formatDateTime(row.postTime) }}</span>
                      </div>
                    </template>

                    <template v-else>
                      <p class="interaction-content">{{ row.text || '（该弹幕内容为空）' }}</p>
                      <div class="interaction-meta-row">
                        <span>发送者：{{ getInteractionUserName(row) }}</span>
                        <span>位置：{{ formatDanmuMode(row.mode) }}</span>
                        <span>出现时间：{{ formatDanmuTime(row.time) }}</span>
                        <span>发送时间：{{ formatDateTime(row.postTime) }}</span>
                        <span class="danmu-color-tag">
                          颜色：
                          <i :style="{ background: row.color || '#FFFFFF' }" />
                          {{ row.color || '#FFFFFF' }}
                        </span>
                      </div>
                    </template>
                  </div>

                  <div class="interaction-actions">
                    <el-button
                      v-if="interactionTabKey === 'comment'"
                      type="danger"
                      plain
                      size="small"
                      :loading="isInteractionDeleting('comment', row.commentId)"
                      @click="handleInteractionCommentDelete(row)"
                    >
                      删除评论
                    </el-button>
                    <el-button
                      v-else
                      type="danger"
                      plain
                      size="small"
                      :loading="isInteractionDeleting('danmu', row.danmuId)"
                      @click="handleInteractionDanmuDelete(row)"
                    >
                      删除弹幕
                    </el-button>
                  </div>
                </article>
              </div>

              <el-empty
                v-else
                :description="
                  interactionSelectedVideoId
                    ? `${activeInteractionTab.label}：当前视频暂无数据`
                    : `${activeInteractionTab.label}：暂无数据`
                "
                :image-size="96"
              />
            </div>

            <div class="pager-wrap">
              <el-pagination
                :current-page="interactionPagination.pageNo"
                :page-size="interactionPagination.pageSize"
                layout="total, prev, pager, next"
                :total="interactionPagination.totalCount"
                @current-change="handleInteractionPageNoChange"
              />
            </div>
          </template>
        </section>

        <section v-else class="placeholder-panel">
          <h2>{{ placeholderTitle }}</h2>
          <p>当前区域按需求先保留为空，后续可以继续扩展业务内容。</p>
          <el-button type="primary" @click="switchSection('create')">进入投稿</el-button>
        </section>
      </main>
    </div>

    <input
      ref="videoInputRef"
      class="hidden-input"
      type="file"
      accept="video/*"
      @change="handleVideoInputChange"
    />
    <input
      ref="coverInputRef"
      class="hidden-input"
      type="file"
      accept="image/*"
      @change="handleCoverInputChange"
    />
  </div>

  <AuthDialog />
</template>

<style scoped>
.creator-page {
  min-height: 100vh;
  padding: 0;
  background:
    radial-gradient(1100px 420px at 6% -10%, rgba(66, 112, 255, 0.18), transparent 72%),
    radial-gradient(820px 320px at 90% 10%, rgba(33, 193, 131, 0.13), transparent 70%),
    linear-gradient(180deg, #f4f7ff 0%, #eaf2ff 100%);
}

.panel {
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(132, 155, 214, 0.24);
  box-shadow: 0 10px 28px rgba(47, 72, 137, 0.08);
  backdrop-filter: blur(4px);
}

.creator-layout {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 0;
  min-height: 100vh;
  width: 100%;
}

.creator-sidebar {
  padding: 16px 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  border-radius: 0;
  border-right: 0;
}

.home-trigger {
  height: 40px;
  border: 1px solid #d5dff4;
  background: #ffffff;
  border-radius: 999px;
  padding: 0 16px;
  color: #23314f;
  font-size: 14px;
  cursor: pointer;
  transition: border-color 0.2s ease, background-color 0.2s ease;
}

.home-trigger:hover {
  border-color: #7f97f6;
  background: #f3f6ff;
}

.sidebar-nav {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.sidebar-item {
  height: 40px;
  border: 1px solid #dbe4f8;
  border-radius: 12px;
  background: #f7f9ff;
  color: #2e4164;
  text-align: left;
  padding: 0 12px;
  font-size: 14px;
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease, background-color 0.2s ease;
}

.sidebar-item:hover {
  transform: translateY(-1px);
  border-color: #829af7;
}

.sidebar-item.active {
  background: linear-gradient(135deg, #5d76ff, #3f92ff);
  border-color: #5d76ff;
  color: #ffffff;
}

.sidebar-item.create-entry {
  min-height: 48px;
  border-radius: 14px;
  border-color: transparent;
  background: linear-gradient(135deg, #ff7a18, #ff4d67);
  color: #ffffff;
  font-weight: 700;
  box-shadow: 0 10px 20px rgba(255, 77, 103, 0.28);
}

.sidebar-item.create-entry:hover {
  border-color: transparent;
  box-shadow: 0 12px 24px rgba(255, 77, 103, 0.34);
}

.sidebar-item.create-entry.active {
  background: linear-gradient(135deg, #ff6b00, #ff375f);
  color: #ffffff;
}

.creator-main {
  padding: 24px;
  border-radius: 0;
  min-height: 100vh;
}

.create-panel {
  padding: 0;
}

.placeholder-panel {
  min-height: 360px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 10px;
  text-align: center;
}

.placeholder-panel h2 {
  margin: 0;
  color: #223252;
}

.placeholder-panel p {
  margin: 0;
  color: #6f7f9f;
}

.post-manage-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.interaction-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.interaction-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.interaction-tabs {
  display: inline-flex;
  gap: 8px;
  border-radius: 12px;
  padding: 4px;
  border: 1px solid #d8e2f7;
  background: #f7f9ff;
}

.interaction-tab {
  min-width: 96px;
  height: 34px;
  border: none;
  border-radius: 9px;
  background: transparent;
  color: #30466c;
  cursor: pointer;
  font-weight: 600;
  transition: background-color 0.2s ease, color 0.2s ease;
}

.interaction-tab:hover {
  background: #edf2ff;
}

.interaction-tab.active {
  background: linear-gradient(135deg, #5d76ff, #3f92ff);
  color: #ffffff;
}

.interaction-filter {
  display: flex;
  align-items: center;
  gap: 10px;
}

.interaction-list-wrap {
  width: 100%;
  min-height: 240px;
}

.interaction-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.interaction-item {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr) auto;
  gap: 14px;
  align-items: center;
  border: 1px solid #dce4f7;
  border-radius: 14px;
  background: #ffffff;
  padding: 12px;
}

.interaction-video {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
}

.interaction-video-cover {
  width: 120px;
  aspect-ratio: 16 / 9;
  object-fit: cover;
  border-radius: 10px;
  border: 1px solid #dce4f7;
  background: #edf2ff;
}

.interaction-video-cover-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #6f7f9f;
}

.interaction-video-meta {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.interaction-video-title {
  margin: 0;
  font-size: 14px;
  color: #243656;
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.interaction-video-sub {
  margin: 0;
  font-size: 12px;
  color: #6f7f9f;
}

.interaction-video-link {
  width: fit-content;
  border: 1px solid #d7e0f8;
  border-radius: 8px;
  background: #f7f9ff;
  color: #2d446c;
  font-size: 12px;
  line-height: 1;
  padding: 7px 10px;
  cursor: pointer;
  transition: border-color 0.2s ease, background-color 0.2s ease;
}

.interaction-video-link:hover {
  border-color: #829af7;
  background: #edf2ff;
}

.interaction-body {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.interaction-content {
  margin: 0;
  color: #25395c;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.interaction-image {
  max-width: 140px;
  max-height: 90px;
  border-radius: 8px;
  border: 1px solid #dce4f7;
  object-fit: cover;
}

.interaction-meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 14px;
  color: #607295;
  font-size: 12px;
}

.danmu-color-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.danmu-color-tag i {
  width: 12px;
  height: 12px;
  border-radius: 999px;
  border: 1px solid #d8e1f6;
  display: inline-block;
}

.interaction-actions {
  justify-self: end;
}

.content-head h2 {
  margin: 0;
  font-size: 24px;
  color: #213153;
}

.content-head p {
  margin: 8px 0 0;
  font-size: 14px;
  color: #6f7f9f;
}

.auth-empty {
  min-height: 280px;
  border: 1px dashed #c8d5f4;
  border-radius: 14px;
  background: #f9fbff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #667ea8;
}

.status-tabs {
  display: flex;
  width: 100%;
  gap: 10px;
}

.status-tab {
  border: 1px solid #dbe4f8;
  border-radius: 12px;
  background: #f7f9ff;
  color: #2e4164;
  flex: 1;
  min-width: 0;
  min-height: 66px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 5px;
  padding: 8px 12px;
  cursor: pointer;
  transition: border-color 0.2s ease, background-color 0.2s ease;
}

.status-tab span {
  font-size: 13px;
}

.status-tab strong {
  font-size: 20px;
  line-height: 1;
}

.status-tab:hover {
  border-color: #829af7;
}

.status-tab.active {
  background: rgba(93, 118, 255, 0.1);
  border-color: #5d76ff;
  color: #2e49cd;
}

.post-list-wrap {
  width: 100%;
  min-height: 240px;
}

.post-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.post-row {
  display: grid;
  grid-template-columns: 200px minmax(0, 1fr) auto;
  align-items: center;
  gap: 16px;
  border: 1px solid #dce4f7;
  border-radius: 14px;
  padding: 12px;
  background: #ffffff;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.post-row:hover {
  border-color: #9ab0f9;
  box-shadow: 0 8px 22px rgba(63, 104, 194, 0.08);
}

.post-thumb-wrap {
  width: 100%;
}

.post-thumb {
  width: 100%;
  aspect-ratio: 16 / 9;
  border-radius: 10px;
  object-fit: cover;
  border: 1px solid #dce4f7;
  background: #edf2ff;
}

.post-thumb-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6f7f9f;
  font-size: 13px;
}

.post-content {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.post-title-line {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.post-title {
  margin: 0;
  color: #223252;
  font-size: 16px;
  font-weight: 700;
  min-width: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.post-status {
  flex-shrink: 0;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 12px;
  line-height: 1.2;
  border: 1px solid transparent;
}

.post-status.pending {
  color: #b45309;
  background: #fff6db;
  border-color: #ffd47a;
}

.post-status.pass {
  color: #1f8f4d;
  background: #eaf9f1;
  border-color: #8cdcb2;
}

.post-status.fail {
  color: #bf1238;
  background: #fff0f3;
  border-color: #ffc1cf;
}

.post-status.default {
  color: #5f7397;
  background: #eef3ff;
  border-color: #d7e1f7;
}

.post-date {
  margin: 0;
  color: #6f7f9f;
  font-size: 13px;
}

.post-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 16px;
}

.metric-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #52698f;
  font-size: 13px;
  white-space: nowrap;
}

.metric-item :deep(.iconfont-svg) {
  font-size: 14px;
  color: #6b83b0;
}

.metric-item strong {
  font-size: 13px;
  font-weight: 600;
  color: #42597f;
}

.post-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.post-edit-btn,
.post-more-btn {
  border: 1px solid #d5dff4;
  background: #ffffff;
  color: #2e4164;
  height: 34px;
  border-radius: 10px;
  cursor: pointer;
  transition: border-color 0.2s ease, background-color 0.2s ease, opacity 0.2s ease;
}

.post-edit-btn {
  min-width: 74px;
  padding: 0 14px;
  font-size: 13px;
  font-weight: 600;
}

.post-edit-btn:hover:not(:disabled),
.post-more-btn:hover {
  border-color: #7f97f6;
  background: #f4f7ff;
}

.post-edit-btn:disabled {
  cursor: not-allowed;
  opacity: 0.46;
}

.post-more-btn {
  width: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
}

.post-more-btn.active {
  border-color: #7f97f6;
  background: #eef3ff;
}

.more-dots {
  font-size: 18px;
  line-height: 1;
}

:deep(.post-more-popover.el-popover) {
  border-radius: 12px;
  border: 1px solid #d9e4fa;
  padding: 10px 12px 12px;
  box-shadow: 0 12px 32px rgba(34, 60, 122, 0.16);
}

.post-more-menu {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.post-more-title {
  margin: 0;
  font-size: 13px;
  font-weight: 600;
  color: #344b75;
}

.post-more-menu :deep(.el-checkbox) {
  margin-right: 0;
}

.post-delete-action {
  margin-top: 2px;
  width: 100%;
  height: 34px;
  border: 1px solid #f5b9c7;
  border-radius: 10px;
  background: #fff4f7;
  color: #bc274a;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s ease, border-color 0.2s ease, opacity 0.2s ease;
}

.post-delete-action:hover:not(:disabled) {
  border-color: #ef8da4;
  background: #ffe8ef;
}

.post-delete-action:disabled {
  cursor: not-allowed;
  opacity: 0.56;
}

.pager-wrap {
  display: flex;
  justify-content: flex-end;
}

.create-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.create-head h2 {
  margin: 0;
  font-size: 24px;
  color: #213153;
}

.create-head p {
  margin: 8px 0 0;
  font-size: 14px;
  color: #6f7f9f;
}

.head-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 12px;
  color: #60749c;
  text-align: right;
}

.first-upload-area {
  margin-top: 18px;
}

.upload-drop-zone {
  width: 100%;
  border: 2px dashed #95a8df;
  border-radius: 16px;
  background: linear-gradient(180deg, #f8faff, #f2f6ff);
  min-height: 220px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 10px;
  cursor: pointer;
  color: #324b76;
  transition: border-color 0.2s ease, background-color 0.2s ease;
}

.upload-drop-zone:hover {
  border-color: #5d76ff;
  background: linear-gradient(180deg, #f3f6ff, #ecf2ff);
}

.upload-main {
  font-size: 22px;
  font-weight: 600;
}

.upload-sub {
  font-size: 14px;
  color: #607295;
}

.upload-rule {
  font-size: 12px;
  color: #7a89a9;
}

.part-toolbar {
  margin-top: 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.part-toolbar p {
  margin: 0;
  font-size: 14px;
  color: #2d3d5d;
}

.part-list {
  margin-top: 12px;
  display: grid;
  gap: 12px;
}

.part-item {
  border: 1px solid #dce4f7;
  border-radius: 14px;
  padding: 12px;
  display: grid;
  gap: 12px;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  background: #fbfcff;
}

.part-item.status-success {
  border-color: rgba(34, 197, 94, 0.4);
  background: linear-gradient(180deg, #fcfffd, #f5fff9);
}

.part-item.status-error {
  border-color: rgba(248, 113, 113, 0.42);
  background: linear-gradient(180deg, #fffdfd, #fff6f6);
}

.part-order {
  width: 46px;
  height: 46px;
  border-radius: 12px;
  border: 1px solid #cfdaf3;
  background: #eef3ff;
  color: #2f446d;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
}

.part-body {
  min-width: 0;
}

.part-name {
  margin: 0;
  color: #283a5e;
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.part-state {
  margin: 4px 0 8px;
  font-size: 12px;
  color: #6f7f9f;
}

.part-size {
  margin: 6px 0 0;
  font-size: 12px;
  color: #7f8ca8;
}

.part-actions {
  display: grid;
  gap: 6px;
}

.part-actions button {
  width: 80px;
  height: 30px;
  border-radius: 8px;
  border: 1px solid #d8e1f7;
  background: #ffffff;
  color: #2e4165;
  cursor: pointer;
  transition: border-color 0.2s ease, background-color 0.2s ease;
}

.part-actions button:hover:not(:disabled) {
  border-color: #7f97f6;
  background: #f4f7ff;
}

.part-actions button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.part-actions .danger {
  color: #d84a5c;
  border-color: #f4cbd1;
}

.part-actions .danger:hover:not(:disabled) {
  border-color: #eb8795;
  background: #fff5f6;
}

.post-meta-block h3 {
  margin: 0;
  font-size: 20px;
  color: #243252;
}

.meta-subtitle {
  margin: 8px 0 0;
  font-size: 13px;
  color: #6f7f9f;
}

.cover-uploader {
  margin-top: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.cover-trigger {
  width: 178px;
  height: 102px;
  border-radius: 12px;
  border: 1px dashed #8ca3df;
  background: linear-gradient(180deg, #f6f9ff, #edf3ff);
  color: #334c76;
  font-size: 14px;
  cursor: pointer;
  overflow: hidden;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.cover-trigger img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-tip p {
  margin: 0;
  font-size: 13px;
  color: #65789f;
  line-height: 1.5;
}

.meta-form {
  margin-top: 14px;
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.tag-input-box {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-list :deep(.el-tag) {
  border-radius: 999px;
  border-color: #c8d7fb;
  background: #eef4ff;
  color: #35507f;
}

.tag-helper {
  margin: 0;
  font-size: 12px;
  color: #7083a8;
  line-height: 1.4;
}

.interaction-row {
  display: flex;
  gap: 18px;
  flex-wrap: wrap;
}

.submit-bar {
  margin-top: 6px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.submit-bar p {
  margin: 0;
  color: #7a89a9;
  font-size: 13px;
}

.hidden-input {
  display: none;
}

@media (max-width: 980px) {
  .post-row {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .interaction-item {
    grid-template-columns: 1fr;
    align-items: flex-start;
  }

  .interaction-video {
    grid-template-columns: 160px minmax(0, 1fr);
  }

  .interaction-actions {
    justify-self: flex-start;
  }

  .post-thumb-wrap {
    max-width: 360px;
  }

  .post-actions {
    justify-content: flex-start;
  }

  .create-head {
    flex-direction: column;
  }

  .head-meta {
    text-align: left;
  }

  .part-item {
    grid-template-columns: 1fr;
  }

  .part-order {
    width: 58px;
  }

  .part-actions {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .part-actions button {
    width: 100%;
  }
}

@media (max-width: 720px) {
  .post-thumb-wrap {
    max-width: 100%;
  }

  .interaction-toolbar {
    align-items: stretch;
  }

  .interaction-filter {
    width: 100%;
    flex-wrap: wrap;
  }

  .interaction-filter :deep(.el-select) {
    width: 100% !important;
  }

  .interaction-video {
    grid-template-columns: 1fr;
  }

  .interaction-video-cover {
    width: 100%;
    max-width: 260px;
  }

  .post-title-line {
    flex-wrap: wrap;
  }

  .post-metrics {
    gap: 8px 12px;
  }

  .meta-grid {
    grid-template-columns: 1fr;
  }

  .cover-uploader {
    flex-direction: column;
    align-items: flex-start;
  }

  .cover-trigger {
    width: 100%;
    height: 170px;
  }

  .submit-bar {
    flex-direction: column;
    align-items: flex-start;
  }
}
/* === Redesign Overrides (ui-ux-pro-max: Vibrant & Block-based) === */
.creator-page {
  --c-primary: #5d76ff;
  --c-secondary: #3f92ff;
  --c-cta: #1fc9a2;
  --c-bg: #f6f9ff;
  --c-text: #1f2a44;
  --c-card: #fbfcff;
  --c-border: #d7e0f8;
  --c-shadow: 0 14px 40px rgba(47, 72, 137, 0.12);
  padding: 0 !important;
  font-family: 'Avenir Next', 'Fira Sans', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  background:
    radial-gradient(1000px 420px at 5% -8%, rgba(111, 137, 255, 0.2), transparent 72%),
    radial-gradient(900px 360px at 92% 8%, rgba(26, 188, 156, 0.14), transparent 68%),
    linear-gradient(180deg, #f6f9ff 0%, #eef4ff 100%) !important;
}

.panel {
  border-color: var(--c-border);
  background: var(--c-card);
  box-shadow: var(--c-shadow);
}

.creator-layout {
  grid-template-columns: 260px minmax(0, 1fr) !important;
  min-height: 100vh;
  gap: 0 !important;
}

.creator-sidebar {
  border-radius: 0 !important;
  border-right: none !important;
  padding: 18px 14px;
  background:
    linear-gradient(180deg, rgba(247, 249, 255, 0.96), rgba(241, 245, 255, 0.94));
}

.creator-brand {
  border-radius: 14px;
  padding: 12px 12px 10px;
  border: 1px solid #d7e0f8;
  background: linear-gradient(145deg, #f7f9ff, #eef3ff);
}

.creator-brand strong {
  display: block;
  font-size: 20px;
  line-height: 1.1;
  letter-spacing: 0.4px;
  color: var(--c-text);
}

.creator-brand span {
  display: block;
  margin-top: 3px;
  font-size: 12px;
  letter-spacing: 0.9px;
  color: #7282a5;
}

.home-trigger {
  border-color: #dce4f7;
  background: #ffffff;
  color: #2b3e61;
}

.home-trigger:hover {
  border-color: #92a8f8;
  background: #f4f7ff;
}

.sidebar-item {
  border-color: #d7e0f8;
  background: #f7f9ff;
  color: #2c3c5a;
  font-weight: 600;
}

.sidebar-item:hover {
  border-color: #7f97f6;
  background: #eff3ff;
}

.sidebar-item.active {
  background: linear-gradient(145deg, #5d76ff, #3f92ff);
  border-color: #5d76ff;
  color: #fff;
}

.sidebar-item.create-entry {
  min-height: 52px;
  border: none;
  border-radius: 16px;
  background: linear-gradient(130deg, #5d76ff 0%, #3f92ff 56%, #1fc9a2 100%);
  color: #fff;
  font-weight: 800;
  letter-spacing: 0.6px;
  box-shadow: 0 12px 28px rgba(93, 118, 255, 0.35);
}

.sidebar-item.create-entry:hover {
  box-shadow: 0 16px 32px rgba(63, 146, 255, 0.34);
}

.creator-main {
  border-radius: 0 !important;
  min-height: 100vh;
  padding: 22px 24px 26px;
  background:
    linear-gradient(180deg, rgba(251, 252, 255, 0.9), rgba(245, 248, 255, 0.84));
}

.main-banner {
  margin-bottom: 18px;
  border-radius: 18px;
  padding: 16px 18px;
  border: 1px solid #d7e0f8;
  background:
    linear-gradient(135deg, rgba(111, 137, 255, 0.14), rgba(26, 188, 156, 0.12)),
    #fbfcff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.banner-kicker {
  margin: 0;
  font-size: 11px;
  letter-spacing: 1.4px;
  color: #5d6e8e;
  font-weight: 700;
}

.banner-left h1 {
  margin: 6px 0 0;
  font-size: clamp(24px, 2.8vw, 34px);
  line-height: 1.06;
  color: var(--c-text);
}

.banner-desc {
  margin: 8px 0 0;
  font-size: 13px;
  color: #516488;
}

.banner-chip {
  flex-shrink: 0;
  border-radius: 999px;
  padding: 8px 14px;
  font-size: 12px;
  color: #fff;
  background: linear-gradient(135deg, #5d76ff, #1fc9a2);
}

.create-panel,
.post-manage-panel,
.interaction-panel,
.placeholder-panel {
  border-radius: 18px;
  border: 1px solid #e2e8fa;
  background: #fbfcff;
  padding: 18px;
}

.placeholder-panel {
  min-height: 420px;
}

.create-head h2,
.content-head h2 {
  color: #1f2a44;
}

.create-head p,
.content-head p,
.meta-subtitle,
.part-state,
.submit-bar p,
.cover-tip p {
  color: #6f7f9f;
}

.head-meta {
  color: #60749c;
}

.upload-drop-zone {
  border-color: #95a8df;
  background: linear-gradient(180deg, #f8faff, #f2f6ff);
  color: #324b76;
}

.upload-drop-zone:hover {
  border-color: #5d76ff;
  background: linear-gradient(180deg, #f3f6ff, #ecf2ff);
}

.part-item {
  border-color: #dce4f7;
  background: #fbfcff;
}

.part-order {
  border-color: #cfdaf3;
  background: #eef3ff;
  color: #2f446d;
}

.status-tabs {
  display: flex !important;
  flex-wrap: nowrap !important;
  overflow-x: auto;
  gap: 10px;
  width: 100%;
  padding-bottom: 4px;
  scrollbar-width: thin;
}

.status-tab {
  flex: 1 0 0;
  min-width: 170px;
  border-color: #d7e0f8;
  background: #f7f9ff;
  color: #2c3c5a;
}

.status-tab:hover {
  border-color: #7f97f6;
}

.status-tab.active {
  border-color: #5d76ff;
  background: linear-gradient(140deg, rgba(93, 118, 255, 0.14), rgba(31, 201, 162, 0.1));
  color: #1f2a44;
}

.auth-empty {
  border-color: #d7e0f8;
  background: linear-gradient(180deg, #f7f9ff, #f1f5ff);
  color: #6f7f9f;
}

.post-row {
  border-color: #d7e0f8;
  background: #fbfcff;
}

.post-row:hover {
  border-color: #95a9f8;
  box-shadow: 0 10px 24px rgba(58, 92, 168, 0.1);
}

.post-thumb {
  border-color: #d7e0f8;
}

.post-edit-btn,
.post-more-btn {
  border-color: #d7e0f8;
  color: #2c3c5a;
}

.post-edit-btn:hover:not(:disabled),
.post-more-btn:hover {
  border-color: #7f97f6;
  background: #eff3ff;
}

@media (max-width: 1100px) {
  .creator-layout {
    grid-template-columns: 220px minmax(0, 1fr) !important;
  }

  .creator-sidebar {
    flex-direction: column !important;
    align-items: stretch !important;
    justify-content: flex-start !important;
  }

  .sidebar-nav {
    min-width: 0 !important;
    flex-direction: column !important;
    flex-wrap: nowrap !important;
  }
}

@media (max-width: 720px) {
  .creator-layout {
    grid-template-columns: 94px minmax(0, 1fr) !important;
  }

  .creator-main {
    padding: 12px;
  }

  .creator-brand {
    padding: 10px 8px;
  }

  .creator-brand strong {
    font-size: 13px;
    letter-spacing: 0.2px;
  }

  .creator-brand span {
    font-size: 10px;
    letter-spacing: 0.4px;
  }

  .home-trigger,
  .sidebar-item {
    text-align: center;
    padding: 0 6px;
    font-size: 12px;
  }

  .main-banner {
    padding: 12px;
  }

  .banner-chip {
    display: none;
  }
}
</style>

