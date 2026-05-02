<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { EditPen, Female, Male, User } from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import {
  cancelFocusUser,
  focusUser,
  getUserInfo,
  loadFansList,
  loadFocusList,
  loadHomeVideoList,
  loadUserCollection,
  updateUserInfo,
} from '@/api/home'
import { uploadImage } from '@/api/file'
import { getSearchKeywordTop } from '@/api/video'
import AuthDialog from '@/components/AuthDialog.vue'
import IconFont from '@/components/IconFont.vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const pageLoading = ref(false)
const pageError = ref('')
const userProfile = ref(null)
const relationSubmitting = ref(false)

const activeTab = ref('home')
const topSearchKeyword = ref('')
const topSearchHotKeywords = ref([])
const topSearchHotLoading = ref(false)
const topSearchFocused = ref(false)
const topSearchHovered = ref(false)
const tabSearchKeyword = ref('')
const appliedVideoKeyword = ref('')
const postOrderType = ref(0)
const postOrderOptions = Object.freeze([
  { label: '最新', value: 0 },
  { label: '播放', value: 1 },
  { label: '收藏', value: 2 },
])

const overviewState = reactive({
  focusCount: 0,
  fansCount: 0,
})

const editVisible = ref(false)
const editSubmitting = ref(false)
const avatarUploading = ref(false)
const avatarInputRef = ref(null)
const editForm = reactive({
  nickName: '',
  avatar: '',
  sex: 2,
  birthday: '',
  school: '',
  personIntroduction: '',
  noticeInfo: '',
})

const relationPanelLoading = ref(false)
const relationPanelError = ref('')
const relationPanelPageNo = ref(1)
const relationPanelPageTotal = ref(1)
const relationPanelTotalCount = ref(0)
const relationPanelList = ref([])
let relationPanelRequestId = 0

const homeVideoState = reactive({
  loading: false,
  error: '',
  list: [],
  pageNo: 1,
  pageSize: 10,
  pageTotal: 1,
  totalCount: 0,
})

const postVideoState = reactive({
  loading: false,
  error: '',
  list: [],
  pageNo: 1,
  pageSize: 15,
  pageTotal: 1,
  totalCount: 0,
})

let homeVideoRequestId = 0
let postVideoRequestId = 0
let topSearchHotRequestId = 0
let topSearchHotTimer = null
let topSearchHideTimer = null

const collectionState = reactive({
  loading: false,
  error: '',
  list: [],
  pageNo: 1,
  pageSize: 15,
  pageTotal: 1,
  totalCount: 0,
})
let collectionRequestId = 0

const isLoggedIn = computed(() => authStore.isLoggedIn)
const currentUserId = computed(() => String(authStore.userInfo?.userId || '').trim())
const routeUserId = computed(() => String(route.query.userId || '').trim())
const targetUserId = computed(() => routeUserId.value || currentUserId.value)
const isViewingSelf = computed(() => {
  return Boolean(targetUserId.value) && targetUserId.value === currentUserId.value
})

const headerDisplayName = computed(() => String(authStore.userInfo?.nickName || '\u672a\u767b\u5f55\u7528\u6237'))
const headerDisplayAvatar = computed(() => toResourceUrl(authStore.userInfo?.avatar || ''))

const displayName = computed(() => {
  return String(userProfile.value?.nickName || userProfile.value?.userId || '\u672a\u77e5\u7528\u6237')
})
const displayAvatar = computed(() => toResourceUrl(userProfile.value?.avatar || ''))
const displaySex = computed(() => toSexText(userProfile.value?.sex))
const displayProfileId = computed(() => String(userProfile.value?.userId || '--'))

const profileSexType = computed(() => {
  const sex = Number(userProfile.value?.sex)
  if (sex === 0) {
    return 'female'
  }
  if (sex === 1) {
    return 'male'
  }
  return 'unknown'
})

const showFollowAction = computed(() => {
  return Boolean(targetUserId.value) && !isViewingSelf.value
})
const hasFocused = computed(() => toBooleanFocus(userProfile.value?.haveFocus))

const metricCards = computed(() => {
  const canOpenRelationPanel = isViewingSelf.value && isLoggedIn.value
  return [
    {
      key: 'focus',
      label: '\u5173\u6ce8',
      value: formatMetric(overviewState.focusCount),
      relationType: 'focus',
      clickable: canOpenRelationPanel,
    },
    {
      key: 'fans',
      label: '\u7c89\u4e1d',
      value: formatMetric(overviewState.fansCount),
      relationType: 'fans',
      clickable: canOpenRelationPanel,
    },
    {
      key: 'like',
      label: '\u83b7\u8d5e',
      value: formatMetricNullable(userProfile.value?.likeCount),
      relationType: '',
      clickable: false,
    },
    {
      key: 'play',
      label: '\u64ad\u653e',
      value: formatMetricNullable(userProfile.value?.playCount),
      relationType: '',
      clickable: false,
    },
  ]
})

const introLabelText = '\u7b80\u4ecb'
const noticeLabelText = '\u516c\u544a'
const birthdayLabelText = '\u751f\u65e5'
const schoolLabelText = '\u5b66\u6821'
const introAriaText = '\u4e2a\u4eba\u7b80\u4ecb'
const homeInfoAriaText = '\u4e3b\u9875\u8d44\u6599\u4fe1\u606f'
const collectionAriaText = '\u6536\u85cf\u89c6\u9891\u5217\u8868'
const introFallbackText = '\u8fd9\u4e2a\u4eba\u5f88\u795e\u79d8\uff0c\u8fd8\u6ca1\u6709\u7b80\u4ecb'
const noticeFallbackText = '\u6682\u672a\u53d1\u5e03\u516c\u544a'
const schoolFallbackText = '\u672a\u586b\u5199'
const relationIntroFallbackText = '\u8fd9\u4e2a\u4eba\u8fd8\u6ca1\u6709\u7b80\u4ecb'
const collectionEmptyText = '\u6682\u65e0\u6536\u85cf\u89c6\u9891'

const introDisplay = computed(() => {
  const raw = String(userProfile.value?.personIntroduction || '').trim()
  return raw || introFallbackText
})

const noticeDisplay = computed(() => {
  const raw = String(userProfile.value?.noticeInfo || '').trim()
  return raw || noticeFallbackText
})

const noticeTitle = computed(() => {
  const raw = String(userProfile.value?.noticeInfo || '').trim()
  return raw || noticeFallbackText
})

const birthdayDisplay = computed(() => {
  const value = formatDateOnly(userProfile.value?.birthday)
  return value === '--' ? '--' : value
})

const schoolDisplay = computed(() => {
  const raw = String(userProfile.value?.school || '').trim()
  return raw || schoolFallbackText
})

const relationActiveType = computed(() => {
  return activeTab.value === 'fans' ? 'fans' : 'focus'
})

const relationPanelTitle = computed(() => {
  return relationActiveType.value === 'fans' ? '\u6211\u7684\u7c89\u4e1d' : '\u6211\u7684\u5173\u6ce8'
})

const relationPanelEmptyText = computed(() => {
  return relationActiveType.value === 'fans'
    ? '\u8fd8\u6ca1\u6709\u7c89\u4e1d'
    : '\u8fd8\u6ca1\u6709\u5173\u6ce8\u7528\u6237'
})

const relationPanelTimeLabel = computed(() => {
  return relationActiveType.value === 'fans'
    ? '\u5173\u6ce8\u4f60\u7684\u65f6\u95f4'
    : '\u5173\u6ce8\u65f6\u95f4'
})

const relationPanelTotalText = computed(() => {
  return `\u5171 ${relationPanelTotalCount.value} \u4eba`
})

const canSearchVideo = computed(() => {
  return activeTab.value === 'home' || activeTab.value === 'video'
})

const tabSearchPlaceholder = computed(() => {
  return canSearchVideo.value ? '\u641c\u89c6\u9891' : '\u4ec5\u4e3b\u9875/\u6295\u7a3f\u53ef\u641c\u7d22'
})

const showTopSearchHotPanel = computed(() => {
  return (
    (topSearchFocused.value || topSearchHovered.value) &&
    (topSearchHotLoading.value || topSearchHotKeywords.value.length > 0)
  )
})

const showVideoContent = computed(() => {
  return activeTab.value === 'home' || activeTab.value === 'video'
})

const activeVideoTitle = computed(() => {
  return activeTab.value === 'home' ? '\u0054\u0061\u7684\u89c6\u9891' : '\u6295\u7a3f'
})

const activeVideoEmptyText = computed(() => {
  const keyword = String(appliedVideoKeyword.value || '').trim()
  if (keyword) {
    return `\u672a\u627e\u5230\u4e0e\u201c${keyword}\u201d\u76f8\u5173\u7684\u89c6\u9891`
  }
  return activeTab.value === 'home'
    ? '\u0054\u0061\u8fd8\u6ca1\u6709\u53d1\u5e03\u89c6\u9891'
    : '\u6682\u65e0\u6295\u7a3f\u89c6\u9891'
})

const activeVideoState = computed(() => {
  return activeTab.value === 'home' ? homeVideoState : postVideoState
})

const activeVideoList = computed(() => {
  return Array.isArray(activeVideoState.value.list) ? activeVideoState.value.list : []
})

const activeVideoLoading = computed(() => {
  return Boolean(activeVideoState.value.loading)
})

const activeVideoError = computed(() => {
  return String(activeVideoState.value.error || '')
})

const activeVideoTotalCount = computed(() => {
  return Math.max(0, Number(activeVideoState.value.totalCount || 0))
})

const activeVideoPageNo = computed(() => {
  return Math.max(1, Number(activeVideoState.value.pageNo || 1))
})

const activeVideoPageTotal = computed(() => {
  return Math.max(1, Number(activeVideoState.value.pageTotal || 1))
})

const collectionList = computed(() => {
  return Array.isArray(collectionState.list) ? collectionState.list : []
})

const collectionLoading = computed(() => {
  return Boolean(collectionState.loading)
})

const collectionError = computed(() => {
  return String(collectionState.error || '')
})

const collectionPageNo = computed(() => {
  return Math.max(1, Number(collectionState.pageNo || 1))
})

const collectionPageTotal = computed(() => {
  return Math.max(1, Number(collectionState.pageTotal || 1))
})

const collectionTotalCount = computed(() => {
  return Math.max(0, Number(collectionState.totalCount || 0))
})
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

function normalizePagination(payload) {
  const data = payload && typeof payload === 'object' ? payload : {}
  return {
    totalCount: Math.max(0, Number(data?.totalCount || 0)),
  }
}

function toBooleanFocus(value) {
  if (typeof value === 'boolean') {
    return value
  }
  if (typeof value === 'number') {
    return value === 1
  }
  const text = String(value || '').trim().toLowerCase()
  return text === '1' || text === 'true'
}

function toSexText(value) {
  const sex = Number(value)
  if (sex === 0) {
    return '\u5973'
  }
  if (sex === 1) {
    return '\u7537'
  }
  return '\u672a\u77e5'
}

function formatDateOnly(value) {
  if (!value) {
    return '--'
  }
  const raw = String(value).trim()
  if (!raw) {
    return '--'
  }
  if (/^\d{4}-\d{2}-\d{2}$/.test(raw)) {
    return raw
  }
  if (/^\d{4}-\d{2}-\d{2}T/.test(raw)) {
    return raw.slice(0, 10)
  }
  const date = new Date(raw)
  if (Number.isNaN(date.getTime())) {
    return raw
  }
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function formatDateTime(value) {
  if (!value) {
    return '--'
  }
  const raw = String(value).trim()
  if (!raw) {
    return '--'
  }
  if (/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/.test(raw)) {
    return raw
  }
  if (/^\d{4}-\d{2}-\d{2}T/.test(raw)) {
    return raw.replace('T', ' ').slice(0, 19)
  }
  const date = new Date(raw)
  if (Number.isNaN(date.getTime())) {
    return raw
  }
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  const second = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}:${second}`
}

function normalizeRelationItem(item) {
  const data = item && typeof item === 'object' ? item : {}
  return {
    userId: String(data.userId || '').trim(),
    focusUserId: String(data.focusUserId || '').trim(),
    focusTime: String(data.focusTime || '').trim(),
    otherNickName: String(data.otherNickName || '').trim(),
    otherUserId: String(data.otherUserId || '').trim(),
    otherPersonIntroduction: String(data.otherPersonIntroduction || '').trim(),
    otherAvatar: String(data.otherAvatar || '').trim(),
    focusType: Number(data.focusType || 0),
  }
}

function normalizeVideoItem(item) {
  const data = item && typeof item === 'object' ? item : {}
  return {
    videoId: String(data.videoId || '').trim(),
    videoCover: String(data.videoCover || '').trim(),
    videoName: String(data.videoName || '').trim(),
    userId: String(data.userId || '').trim(),
    lastUpdateTime: String(data.lastUpdateTime || '').trim(),
    duration: Number(data.duration || 0),
    playCount: Number(data.playCount || 0),
    likeCount: Number(data.likeCount || 0),
  }
}

function normalizeCollectionItem(item) {
  const data = item && typeof item === 'object' ? item : {}
  return {
    actionId: Number(data.actionId || 0),
    videoId: String(data.videoId || '').trim(),
    videoUserId: String(data.videoUserId || '').trim(),
    actionTime: String(data.actionTime || '').trim(),
    videoCover: String(data.videoCover || '').trim(),
    videoName: String(data.videoName || '').trim(),
  }
}

function resetRelationPanelState() {
  relationPanelRequestId += 1
  relationPanelLoading.value = false
  relationPanelError.value = ''
  relationPanelPageNo.value = 1
  relationPanelPageTotal.value = 1
  relationPanelTotalCount.value = 0
  relationPanelList.value = []
}

function resetHomeVideoState() {
  homeVideoRequestId += 1
  homeVideoState.loading = false
  homeVideoState.error = ''
  homeVideoState.list = []
  homeVideoState.pageNo = 1
  homeVideoState.pageSize = 10
  homeVideoState.pageTotal = 1
  homeVideoState.totalCount = 0
}

function resetPostVideoState() {
  postVideoRequestId += 1
  postVideoState.loading = false
  postVideoState.error = ''
  postVideoState.list = []
  postVideoState.pageNo = 1
  postVideoState.pageSize = 15
  postVideoState.pageTotal = 1
  postVideoState.totalCount = 0
}

function resetCollectionState() {
  collectionRequestId += 1
  collectionState.loading = false
  collectionState.error = ''
  collectionState.list = []
  collectionState.pageNo = 1
  collectionState.pageSize = 15
  collectionState.pageTotal = 1
  collectionState.totalCount = 0
}

function resetPostOrderType() {
  postOrderType.value = 0
}

function resetAppliedVideoKeyword() {
  appliedVideoKeyword.value = ''
  tabSearchKeyword.value = ''
}

function getAppliedVideoKeyword() {
  return String(appliedVideoKeyword.value || '').trim()
}

function formatMetric(value) {
  const num = Number(value)
  if (!Number.isFinite(num) || num < 0) {
    return '0'
  }
  return String(Math.floor(num))
}

function normalizeCount(value) {
  const num = Number(value)
  if (!Number.isFinite(num) || num < 0) {
    return 0
  }
  return Math.floor(num)
}

function formatDuration(value) {
  const totalSeconds = Number(value)
  if (!Number.isFinite(totalSeconds) || totalSeconds < 0) {
    return '--:--'
  }
  const seconds = Math.floor(totalSeconds % 60)
  const minutes = Math.floor((totalSeconds / 60) % 60)
  const hours = Math.floor(totalSeconds / 3600)
  const minuteText = String(minutes).padStart(2, '0')
  const secondText = String(seconds).padStart(2, '0')

  if (hours > 0) {
    return `${String(hours).padStart(2, '0')}:${minuteText}:${secondText}`
  }
  return `${minuteText}:${secondText}`
}

function formatMetricNullable(value) {
  if (value === null || value === undefined || value === '') {
    return '--'
  }
  return formatMetric(value)
}

function syncEditForm() {
  editForm.nickName = String(userProfile.value?.nickName || '')
  editForm.avatar = String(userProfile.value?.avatar || '')
  editForm.sex = Number(userProfile.value?.sex ?? 2)
  editForm.birthday = formatDateOnly(userProfile.value?.birthday).replace('--', '')
  editForm.school = String(userProfile.value?.school || '')
  editForm.personIntroduction = String(userProfile.value?.personIntroduction || '')
  editForm.noticeInfo = String(userProfile.value?.noticeInfo || '')
}

function updateOverviewFromUserProfile() {
  overviewState.focusCount = Math.max(0, Number(userProfile.value?.focusCount || 0))
  overviewState.fansCount = Math.max(0, Number(userProfile.value?.fansCount || 0))
}

function resetOverviewState() {
  overviewState.focusCount = 0
  overviewState.fansCount = 0
}

function normalizeTopSearchKeyword(value) {
  return String(value || '').trim()
}

function clearTopSearchHotTimer() {
  if (!topSearchHotTimer) {
    return
  }
  clearTimeout(topSearchHotTimer)
  topSearchHotTimer = null
}

function clearTopSearchHideTimer() {
  if (!topSearchHideTimer) {
    return
  }
  clearTimeout(topSearchHideTimer)
  topSearchHideTimer = null
}

function clearTopSearchHotState() {
  topSearchHotRequestId += 1
  topSearchHotLoading.value = false
  topSearchHotKeywords.value = []
}

function normalizeTopSearchKeywordList(list) {
  if (!Array.isArray(list)) {
    return []
  }
  const keywordSet = new Set()
  const keywordResult = []
  list.forEach((item) => {
    let keywordText = ''
    if (typeof item === 'string') {
      keywordText = item
    } else if (item && typeof item === 'object') {
      keywordText = String(
        item.keyword ??
        item.searchKeyword ??
        item.keywordName ??
        item.name ??
        item.text ??
        '',
      )
    }
    const normalized = normalizeTopSearchKeyword(keywordText)
    if (!normalized || keywordSet.has(normalized)) {
      return
    }
    keywordSet.add(normalized)
    keywordResult.push(normalized)
  })
  return keywordResult.slice(0, 10)
}

async function loadTopSearchHotKeywords(keyword = '') {
  const targetKeyword = normalizeTopSearchKeyword(keyword)

  const requestId = ++topSearchHotRequestId
  topSearchHotLoading.value = true
  try {
    const data = await getSearchKeywordTop({
      keyword: targetKeyword,
    })
    if (requestId !== topSearchHotRequestId) {
      return
    }
    const list = Array.isArray(data) ? data : Array.isArray(data?.list) ? data.list : []
    topSearchHotKeywords.value = normalizeTopSearchKeywordList(list)
  } catch (_error) {
    if (requestId !== topSearchHotRequestId) {
      return
    }
    topSearchHotKeywords.value = []
  } finally {
    if (requestId === topSearchHotRequestId) {
      topSearchHotLoading.value = false
    }
  }
}

function queueLoadTopSearchHotKeywords(keyword = '') {
  const targetKeyword = normalizeTopSearchKeyword(keyword)
  clearTopSearchHotTimer()
  topSearchHotTimer = setTimeout(() => {
    loadTopSearchHotKeywords(targetKeyword)
  }, 260)
}

function handleTopSearchFocus() {
  topSearchFocused.value = true
  clearTopSearchHideTimer()
  if (topSearchHotKeywords.value.length === 0 && !topSearchHotLoading.value) {
    queueLoadTopSearchHotKeywords(topSearchKeyword.value)
  }
}

function handleTopSearchBlur() {
  clearTopSearchHideTimer()
  topSearchHideTimer = setTimeout(() => {
    topSearchFocused.value = false
  }, 120)
}

function handleTopSearchMouseEnter() {
  topSearchHovered.value = true
  clearTopSearchHideTimer()
  if (topSearchHotKeywords.value.length === 0 && !topSearchHotLoading.value) {
    queueLoadTopSearchHotKeywords(topSearchKeyword.value)
  }
}

function handleTopSearchMouseLeave() {
  topSearchHovered.value = false
}

function handleTopSearchClear() {
  topSearchKeyword.value = ''
}

function handleTopSearchSubmit() {
  const keyword = normalizeTopSearchKeyword(topSearchKeyword.value)
  topSearchKeyword.value = keyword
  topSearchFocused.value = false
  clearTopSearchHideTimer()

  if (!keyword) {
    router.push('/')
    return
  }

  router.push({
    path: '/',
    query: {
      keyword,
      orderType: '0',
      pageNo: '1',
    },
  })
}

function handlePickTopSearchKeyword(keyword) {
  const normalized = normalizeTopSearchKeyword(keyword)
  if (!normalized) {
    return
  }
  topSearchKeyword.value = normalized
  topSearchHovered.value = false
  handleTopSearchSubmit()
}

function goHome() {
  router.push('/')
}

function goCreatorCenter() {
  router.push('/creator')
}

function goCreatePage() {
  router.push({
    path: '/creator',
    query: {
      tab: 'create',
    },
  })
}

function goVideoPlay(videoId) {
  const targetVideoId = String(videoId || '').trim()
  if (!targetVideoId) {
    return
  }
  router.push(`/video/${targetVideoId}`)
}

function openMyCenter() {
  if (!isLoggedIn.value) {
    authStore.openAuthDialog('login')
    return
  }
  router.push('/user-center')
}

function handleHeaderUserClick() {
  if (!isLoggedIn.value) {
    authStore.openAuthDialog('login')
    return
  }
  openMyCenter()
}

function openRelationUserCenter(userId) {
  const targetId = String(userId || '').trim()
  if (!targetId) {
    return
  }
  if (targetId === currentUserId.value) {
    router.push('/user-center')
    return
  }
  router.push({
    path: '/user-center',
    query: {
      userId: targetId,
    },
  })
}

function setActiveTab(tabKey) {
  if (!['home', 'video', 'collection'].includes(tabKey)) {
    return
  }
  activeTab.value = tabKey
  if (tabKey === 'home' && homeVideoState.list.length === 0) {
    loadHomeTabVideos(1)
  }
  if (tabKey === 'video' && postVideoState.list.length === 0) {
    loadPostTabVideos(1)
  }
  if (tabKey === 'collection' && collectionState.list.length === 0) {
    loadCollectionTab(1)
  }
}

async function loadRelationPanelPage(pageNo = 1, relationType = relationActiveType.value) {
  const requestId = ++relationPanelRequestId
  relationPanelLoading.value = true
  relationPanelError.value = ''
  try {
    const relationApi = relationType === 'fans' ? loadFansList : loadFocusList
    const payload = await relationApi({ pageNo })
    if (requestId !== relationPanelRequestId) {
      return
    }
    const data = payload && typeof payload === 'object' ? payload : {}
    const list = Array.isArray(data.list) ? data.list : []
    relationPanelList.value = list.map((item) => normalizeRelationItem(item))
    relationPanelPageNo.value = Math.max(1, Number(data.pageNo || pageNo))
    relationPanelPageTotal.value = Math.max(1, Number(data.pageTotal || 1))
    relationPanelTotalCount.value = Math.max(0, Number(data.totalCount || relationPanelList.value.length))
  } catch (_error) {
    if (requestId !== relationPanelRequestId) {
      return
    }
    relationPanelError.value = '\u5217\u8868\u52a0\u8f7d\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5'
    relationPanelList.value = []
    relationPanelPageNo.value = 1
    relationPanelPageTotal.value = 1
    relationPanelTotalCount.value = 0
  } finally {
    if (requestId === relationPanelRequestId) {
      relationPanelLoading.value = false
    }
  }
}

async function loadHomeTabVideos(pageNo = 1) {
  const userId = String(targetUserId.value || '').trim()
  if (!userId) {
    resetHomeVideoState()
    return
  }
  const videoName = getAppliedVideoKeyword()
  const requestId = ++homeVideoRequestId
  homeVideoState.loading = true
  homeVideoState.error = ''
  try {
    const payload = await loadHomeVideoList({
      userId,
      type: 1,
      orderType: 0,
      pageNo,
      ...(videoName ? { videoName } : {}),
    })
    if (requestId !== homeVideoRequestId) {
      return
    }
    const data = payload && typeof payload === 'object' ? payload : {}
    const list = Array.isArray(data.list) ? data.list : []
    homeVideoState.list = list.map((item) => normalizeVideoItem(item))
    homeVideoState.pageNo = Math.max(1, Number(data.pageNo || pageNo))
    homeVideoState.pageSize = Math.max(1, Number(data.pageSize || homeVideoState.pageSize))
    homeVideoState.pageTotal = Math.max(1, Number(data.pageTotal || 1))
    homeVideoState.totalCount = Math.max(0, Number(data.totalCount || homeVideoState.list.length))
  } catch (_error) {
    if (requestId !== homeVideoRequestId) {
      return
    }
    homeVideoState.error = '\u89c6\u9891\u52a0\u8f7d\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5'
    homeVideoState.list = []
    homeVideoState.pageNo = 1
    homeVideoState.pageTotal = 1
    homeVideoState.totalCount = 0
  } finally {
    if (requestId === homeVideoRequestId) {
      homeVideoState.loading = false
    }
  }
}

async function loadPostTabVideos(pageNo = 1) {
  const userId = String(targetUserId.value || '').trim()
  if (!userId) {
    resetPostVideoState()
    return
  }
  const videoName = getAppliedVideoKeyword()
  const requestId = ++postVideoRequestId
  postVideoState.loading = true
  postVideoState.error = ''
  try {
    const payload = await loadHomeVideoList({
      userId,
      orderType: postOrderType.value,
      pageNo,
      ...(videoName ? { videoName } : {}),
    })
    if (requestId !== postVideoRequestId) {
      return
    }
    const data = payload && typeof payload === 'object' ? payload : {}
    const list = Array.isArray(data.list) ? data.list : []
    postVideoState.list = list.map((item) => normalizeVideoItem(item))
    postVideoState.pageNo = Math.max(1, Number(data.pageNo || pageNo))
    postVideoState.pageSize = Math.max(1, Number(data.pageSize || postVideoState.pageSize))
    postVideoState.pageTotal = Math.max(1, Number(data.pageTotal || 1))
    postVideoState.totalCount = Math.max(0, Number(data.totalCount || postVideoState.list.length))
  } catch (_error) {
    if (requestId !== postVideoRequestId) {
      return
    }
    postVideoState.error = '\u89c6\u9891\u52a0\u8f7d\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5'
    postVideoState.list = []
    postVideoState.pageNo = 1
    postVideoState.pageTotal = 1
    postVideoState.totalCount = 0
  } finally {
    if (requestId === postVideoRequestId) {
      postVideoState.loading = false
    }
  }
}

async function loadCollectionTab(pageNo = 1) {
  const userId = String(targetUserId.value || '').trim()
  if (!userId) {
    resetCollectionState()
    return
  }
  const requestId = ++collectionRequestId
  collectionState.loading = true
  collectionState.error = ''
  try {
    const payload = await loadUserCollection({
      userId,
      pageNo,
    })
    if (requestId !== collectionRequestId) {
      return
    }
    const data = payload && typeof payload === 'object' ? payload : {}
    const list = Array.isArray(data.list) ? data.list : []
    collectionState.list = list.map((item) => normalizeCollectionItem(item))
    collectionState.pageNo = Math.max(1, Number(data.pageNo || pageNo))
    collectionState.pageSize = Math.max(1, Number(data.pageSize || collectionState.pageSize))
    collectionState.pageTotal = Math.max(1, Number(data.pageTotal || 1))
    collectionState.totalCount = Math.max(0, Number(data.totalCount || collectionState.list.length))
  } catch (_error) {
    if (requestId !== collectionRequestId) {
      return
    }
    collectionState.error = '\u6536\u85cf\u5217\u8868\u52a0\u8f7d\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5'
    collectionState.list = []
    collectionState.pageNo = 1
    collectionState.pageTotal = 1
    collectionState.totalCount = 0
  } finally {
    if (requestId === collectionRequestId) {
      collectionState.loading = false
    }
  }
}

function switchToRelationTab(type) {
  if (!isViewingSelf.value) {
    ElMessage.warning('\u4ec5\u53ef\u67e5\u770b\u81ea\u5df1\u7684\u5173\u6ce8\u548c\u7c89\u4e1d\u5217\u8868')
    return
  }
  if (!isLoggedIn.value) {
    authStore.openAuthDialog('login')
    return
  }
  const relationType = type === 'fans' ? 'fans' : 'focus'
  activeTab.value = relationType
  loadRelationPanelPage(1, relationType)
}

function handleRelationPanelPageChange(nextPage) {
  const page = Math.max(1, Number(nextPage || 1))
  loadRelationPanelPage(page, relationActiveType.value)
}

function handleActiveVideoPageChange(nextPage) {
  const page = Math.max(1, Number(nextPage || 1))
  if (activeTab.value === 'video') {
    loadPostTabVideos(page)
    return
  }
  loadHomeTabVideos(page)
}

function handleCollectionPageChange(nextPage) {
  const page = Math.max(1, Number(nextPage || 1))
  loadCollectionTab(page)
}

function handleTabVideoSearch() {
  if (!canSearchVideo.value) {
    return
  }
  const keyword = String(tabSearchKeyword.value || '').trim()
  appliedVideoKeyword.value = keyword
  resetHomeVideoState()
  resetPostVideoState()

  if (activeTab.value === 'video') {
    loadPostTabVideos(1)
    return
  }
  loadHomeTabVideos(1)
}

function handlePostOrderTypeChange(nextOrderType) {
  const normalized = Number(nextOrderType)
  if (![0, 1, 2].includes(normalized)) {
    return
  }
  const changed = normalized !== postOrderType.value
  postOrderType.value = normalized
  if (!changed && postVideoState.list.length > 0 && !postVideoState.error) {
    return
  }
  if (activeTab.value === 'video') {
    loadPostTabVideos(1)
  }
}

function handleMetricCardClick(item) {
  const relationType = String(item?.relationType || '').trim()
  if (!relationType) {
    return
  }
  switchToRelationTab(relationType)
}

function openAvatarPicker() {
  if (!isViewingSelf.value || avatarUploading.value) {
    return
  }
  avatarInputRef.value?.click()
}

async function handleAvatarChange(event) {
  const file = event?.target?.files?.[0]
  if (event?.target) {
    event.target.value = ''
  }
  if (!file) {
    return
  }
  if (!String(file.type || '').startsWith('image/')) {
    ElMessage.warning('\u53ea\u80fd\u4e0a\u4f20\u56fe\u7247\u6587\u4ef6')
    return
  }

  avatarUploading.value = true
  try {
    const avatarPath = await uploadImage(file, false)
    editForm.avatar = String(avatarPath || '')
    ElMessage.success('\u5934\u50cf\u4e0a\u4f20\u6210\u529f')
  } catch (_error) {
    // Error message handled by request interceptor.
  } finally {
    avatarUploading.value = false
  }
}

async function handleToggleFollow() {
  const focusUserId = String(targetUserId.value || '').trim()
  if (!focusUserId || isViewingSelf.value || relationSubmitting.value) {
    return
  }
  if (!isLoggedIn.value) {
    authStore.openAuthDialog('login')
    return
  }

  relationSubmitting.value = true
  try {
    if (hasFocused.value) {
      await cancelFocusUser({ focusUserId })
      ElMessage.success('\u5df2\u53d6\u6d88\u5173\u6ce8')
    } else {
      await focusUser({ focusUserId })
      ElMessage.success('\u5173\u6ce8\u6210\u529f')
    }
    await loadProfileOnly()
  } catch (_error) {
    // Error message handled by request interceptor.
  } finally {
    relationSubmitting.value = false
  }
}

function openEdit() {
  if (!isViewingSelf.value) {
    return
  }
  syncEditForm()
  editVisible.value = true
}

function cancelEdit() {
  editVisible.value = false
  syncEditForm()
}

async function submitEdit() {
  if (!isViewingSelf.value || editSubmitting.value) {
    return
  }

  const payload = {
    nickName: String(editForm.nickName || '').trim(),
    avatar: String(editForm.avatar || '').trim(),
    sex: Number(editForm.sex),
    birthday: String(editForm.birthday || '').trim(),
    school: String(editForm.school || '').trim(),
    personIntroduction: String(editForm.personIntroduction || '').trim(),
    noticeInfo: String(editForm.noticeInfo || '').trim(),
  }

  if (
    !payload.nickName
    || !payload.avatar
    || !Number.isFinite(payload.sex)
    || !payload.birthday
    || !payload.school
    || !payload.personIntroduction
  ) {
    ElMessage.warning('\u8bf7\u5b8c\u6574\u586b\u5199\u5fc5\u586b\u4fe1\u606f')
    return
  }

  editSubmitting.value = true
  try {
    await updateUserInfo(payload)
    ElMessage.success('\u8d44\u6599\u4fdd\u5b58\u6210\u529f')
    editVisible.value = false
    await loadPageData()
  } catch (_error) {
    // Error message handled by request interceptor.
  } finally {
    editSubmitting.value = false
  }
}

async function loadProfileOnly() {
  const userId = String(targetUserId.value || '').trim()
  if (!userId) {
    userProfile.value = null
    return
  }

  const profile = await getUserInfo({ userId })
  userProfile.value = profile || null
  syncEditForm()
  updateOverviewFromUserProfile()

  if (isViewingSelf.value && isLoggedIn.value && userProfile.value) {
    authStore.setUserInfo({
      ...(authStore.userInfo || {}),
      ...userProfile.value,
      userId: currentUserId.value || userProfile.value.userId,
    })
  }
}

async function loadOverviewOnly() {
  const userId = String(targetUserId.value || '').trim()
  if (!userId) {
    resetOverviewState()
    return
  }

  updateOverviewFromUserProfile()

  if (isViewingSelf.value && isLoggedIn.value) {
    const settled = await Promise.allSettled([
      loadFocusList({ pageNo: 1 }),
      loadFansList({ pageNo: 1 }),
    ])
    const focusData = settled[0]?.status === 'fulfilled' ? normalizePagination(settled[0].value) : null
    const fansData = settled[1]?.status === 'fulfilled' ? normalizePagination(settled[1].value) : null
    overviewState.focusCount = focusData?.totalCount ?? overviewState.focusCount
    overviewState.fansCount = fansData?.totalCount ?? overviewState.fansCount
  }
}

async function loadPageData() {
  const userId = String(targetUserId.value || '').trim()
  if (!userId) {
    pageError.value = '\u8bf7\u5148\u767b\u5f55\u540e\u67e5\u770b\u4e2a\u4eba\u4e2d\u5fc3\uff0c\u6216\u4ece\u7528\u6237\u5934\u50cf\u5165\u53e3\u8fdb\u5165\u4e3b\u9875\u3002'
    userProfile.value = null
    resetOverviewState()
    resetAppliedVideoKeyword()
    resetHomeVideoState()
    resetPostVideoState()
    resetCollectionState()
    resetPostOrderType()
    return
  }

  pageLoading.value = true
  pageError.value = ''
  try {
    await loadProfileOnly()
    await loadOverviewOnly()
    await loadHomeTabVideos(1)
  } catch (_error) {
    pageError.value = '\u4e2a\u4eba\u4e2d\u5fc3\u6570\u636e\u52a0\u8f7d\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5'
    userProfile.value = null
    resetOverviewState()
    resetAppliedVideoKeyword()
    resetHomeVideoState()
    resetPostVideoState()
    resetCollectionState()
    resetPostOrderType()
  } finally {
    pageLoading.value = false
  }
}

watch(
  [targetUserId, () => authStore.isLoggedIn],
  () => {
    activeTab.value = 'home'
    editVisible.value = false
    resetAppliedVideoKeyword()
    resetRelationPanelState()
    resetHomeVideoState()
    resetPostVideoState()
    resetCollectionState()
    resetPostOrderType()
    loadPageData()
  },
  { immediate: true },
)

watch(
  () => topSearchKeyword.value,
  (value) => {
    queueLoadTopSearchHotKeywords(value)
  },
)

onMounted(() => {
  authStore.initAutoLogin()
})

onBeforeUnmount(() => {
  clearTopSearchHotTimer()
  clearTopSearchHideTimer()
  clearTopSearchHotState()
})
</script>
<template>
  <div class="user-center-page">
    <div class="top-strip-shell">
      <header class="panel top-strip">
        <p
          class="home-link"
          role="button"
          tabindex="0"
          @click="goHome"
          @keydown.enter="goHome"
        >
          返回首页
        </p>

        <div
          class="top-search"
          @mouseenter="handleTopSearchMouseEnter"
          @mouseleave="handleTopSearchMouseLeave"
        >
          <el-input
            v-model="topSearchKeyword"
            clearable
            placeholder="搜索视频、标签"
            size="large"
            @focus="handleTopSearchFocus"
            @blur="handleTopSearchBlur"
            @keyup.enter="handleTopSearchSubmit"
            @clear="handleTopSearchClear"
          >
            <template #prefix>
              <IconFont name="icon-sousuo" />
            </template>
          </el-input>

          <button class="top-search-btn" type="button" @click="handleTopSearchSubmit">
            搜索
          </button>

          <div v-if="showTopSearchHotPanel" class="top-search-suggest-panel">
            <p class="top-search-suggest-title">{{ topSearchHotLoading ? '热词更新中...' : '热门搜索' }}</p>
            <div class="top-search-suggest-list">
              <button
                v-for="(keyword, index) in topSearchHotKeywords"
                :key="`${keyword}-${index}`"
                type="button"
                class="top-search-suggest-item"
                @mousedown.prevent
                @click="handlePickTopSearchKeyword(keyword)"
              >
                {{ keyword }}
              </button>
            </div>
          </div>
        </div>

        <div class="top-actions">
          <button class="account-chip" type="button" @click="handleHeaderUserClick">
            <el-avatar :src="headerDisplayAvatar" :size="34">
              <IconFont name="icon-morentouxiang" size="16px" />
            </el-avatar>
            <span>{{ headerDisplayName }}</span>
          </button>

          <el-button class="action-btn" type="default" @click="goCreatorCenter">
            创作者中心
          </el-button>

          <el-button class="action-btn publish-btn" type="primary" @click="goCreatePage">
            投稿
          </el-button>
        </div>
      </header>
    </div>

    <div class="sub-strip-shell">
      <section class="panel profile-strip" v-loading="pageLoading">
        <el-alert
          v-if="pageError"
          type="error"
          show-icon
          :closable="false"
          :title="pageError"
        />

        <template v-else-if="userProfile">
          <div class="profile-main">
            <div class="profile-user">
              <el-avatar :src="displayAvatar" :size="78">
                <IconFont name="icon-morentouxiang" size="36px" />
              </el-avatar>

              <div class="profile-copy">
                <div class="name-row">
                  <h1>{{ displayName }}</h1>
                  <span class="sex-tag" :class="`sex-${profileSexType}`">
                    <el-icon class="sex-icon">
                      <Female v-if="profileSexType === 'female'" />
                      <Male v-else-if="profileSexType === 'male'" />
                      <User v-else />
                    </el-icon>
                    <span>{{ displaySex }}</span>
                  </span>

                  <button
                    v-if="isViewingSelf"
                    type="button"
                    class="edit-trigger"
                    @click="openEdit"
                  >
                    <el-icon><EditPen /></el-icon>
                  </button>
                </div>

                <p class="profile-id">用户ID：{{ displayProfileId }}</p>
              </div>
            </div>

            <div class="profile-action">
              <button
                v-if="showFollowAction"
                type="button"
                class="follow-btn"
                :class="{ active: hasFocused }"
                :disabled="relationSubmitting"
                @click="handleToggleFollow"
              >
                {{ relationSubmitting ? '处理中...' : hasFocused ? '已关注' : '关注' }}
              </button>
              <span v-else class="self-badge">我的主页</span>
            </div>
          </div>
        </template>
      </section>

      <section v-if="!pageError && userProfile" class="panel switch-strip">
        <div class="switch-tabs">
          <button
            type="button"
            class="switch-tab"
            :class="{ active: activeTab === 'home' }"
            @click="setActiveTab('home')"
          >
            主页
          </button>
          <button
            type="button"
            class="switch-tab"
            :class="{ active: activeTab === 'video' }"
            @click="setActiveTab('video')"
          >
            投稿
          </button>
          <button
            type="button"
            class="switch-tab"
            :class="{ active: activeTab === 'collection' }"
            @click="setActiveTab('collection')"
          >
            收藏
          </button>
        </div>

        <div class="switch-search">
          <el-input
            v-model="tabSearchKeyword"
            :disabled="!canSearchVideo"
            :placeholder="tabSearchPlaceholder"
            clearable
            @keyup.enter="handleTabVideoSearch"
            @clear="handleTabVideoSearch"
          >
            <template #prefix>
              <IconFont name="icon-sousuo" />
            </template>
          </el-input>
          <button
            type="button"
            class="tab-search-btn"
            :disabled="!canSearchVideo"
            @click="handleTabVideoSearch"
          >
            搜索
          </button>
        </div>

        <div class="switch-metrics">
          <article
            v-for="item in metricCards"
            :key="item.key"
            class="metric-card"
            :class="{ 'metric-card-clickable': item.clickable }"
            :role="item.clickable ? 'button' : undefined"
            :tabindex="item.clickable ? 0 : undefined"
            @click="handleMetricCardClick(item)"
            @keydown.enter.prevent="handleMetricCardClick(item)"
            @keydown.space.prevent="handleMetricCardClick(item)"
          >
            <p class="metric-label">{{ item.label }}</p>
            <p class="metric-value">{{ item.value }}</p>
          </article>
        </div>
      </section>

      <section
        v-if="!pageError && userProfile && activeTab === 'home'"
        class="panel intro-strip"
        :aria-label="introAriaText"
      >
        <p class="intro-label">{{ introLabelText }}</p>
        <p class="intro-text">{{ introDisplay }}</p>
      </section>

      <section
        v-if="!pageError && userProfile && activeTab === 'home'"
        class="panel home-info-strip"
        :aria-label="homeInfoAriaText"
      >
        <article class="home-info-notice-card">
          <h3>{{ noticeLabelText }}</h3>
          <p class="home-info-notice" :title="noticeTitle">{{ noticeDisplay }}</p>
        </article>

        <div class="home-info-meta">
          <article class="home-info-meta-card">
            <h3>{{ birthdayLabelText }}</h3>
            <p>{{ birthdayDisplay }}</p>
          </article>
          <article class="home-info-meta-card">
            <h3>{{ schoolLabelText }}</h3>
            <p>{{ schoolDisplay }}</p>
          </article>
        </div>
      </section>

      <section
        v-if="!pageError && userProfile && showVideoContent"
        class="panel content-strip video-content-strip"
        :aria-label="activeVideoTitle"
      >
        <div class="video-content-head">
          <h2>{{ activeVideoTitle }}</h2>
          <div
            v-if="activeTab === 'video'"
            class="post-order-tabs"
            aria-label="投稿排序"
          >
            <button
              v-for="item in postOrderOptions"
              :key="item.value"
              type="button"
              class="post-order-tab"
              :class="{ active: postOrderType === item.value }"
              :aria-pressed="postOrderType === item.value"
              @click="handlePostOrderTypeChange(item.value)"
            >
              {{ item.label }}
            </button>
          </div>
        </div>

        <div class="video-content-body" v-loading="activeVideoLoading">
          <el-alert
            v-if="activeVideoError"
            type="error"
            show-icon
            :closable="false"
            :title="activeVideoError"
          />

          <template v-else>
            <div v-if="activeVideoList.length > 0" class="user-video-grid">
              <article
                v-for="(row, index) in activeVideoList"
                :key="row.videoId || `${row.videoName || 'video'}-${index}`"
                class="user-video-card"
                role="button"
                tabindex="0"
                @click="goVideoPlay(row.videoId)"
                @keydown.enter="goVideoPlay(row.videoId)"
              >
                <div class="user-video-cover-wrap">
                  <img
                    v-if="row.videoCover"
                    class="user-video-cover"
                    :src="toResourceUrl(row.videoCover)"
                    alt="视频封面"
                  />
                  <div v-else class="user-video-cover user-video-cover-empty">暂无封面</div>

                  <div class="user-video-overlay">
                    <span class="overlay-item">播放 {{ normalizeCount(row.playCount) }}</span>
                    <span class="overlay-item">点赞 {{ normalizeCount(row.likeCount) }}</span>
                    <span class="overlay-item">{{ formatDuration(row.duration) }}</span>
                  </div>
                </div>

                <h3 class="user-video-title" :title="row.videoName || '未命名视频'">
                  {{ row.videoName || '未命名视频' }}
                </h3>

                <p class="user-video-meta">更新于 {{ formatDateTime(row.lastUpdateTime) }}</p>
              </article>
            </div>

            <el-empty v-else :description="activeVideoEmptyText" />
          </template>
        </div>

        <div class="video-content-footer">
          <p class="video-total" v-if="activeVideoTotalCount > 0">共 {{ activeVideoTotalCount }} 条</p>
          <el-pagination
            v-if="activeVideoPageTotal > 1"
            background
            layout="prev, pager, next"
            :current-page="activeVideoPageNo"
            :page-count="activeVideoPageTotal"
            @current-change="handleActiveVideoPageChange"
          />
        </div>
      </section>

      <section
        v-if="!pageError && userProfile && (activeTab === 'focus' || activeTab === 'fans') && isViewingSelf && isLoggedIn"
        class="panel content-strip relation-content-strip"
        :aria-label="relationPanelTitle"
      >
        <div class="relation-content-head">
          <h2>{{ relationPanelTitle }}</h2>
        </div>

        <div class="relation-content-body" v-loading="relationPanelLoading">
          <el-alert
            v-if="relationPanelError"
            type="error"
            show-icon
            :closable="false"
            :title="relationPanelError"
          />

          <template v-else>
            <div v-if="relationPanelList.length > 0" class="relation-list">
              <button
                v-for="item in relationPanelList"
                :key="`${item.otherUserId}-${item.focusTime}`"
                type="button"
                class="relation-item"
                @click="openRelationUserCenter(item.otherUserId)"
              >
                <el-avatar :src="toResourceUrl(item.otherAvatar)" :size="44">
                  <IconFont name="icon-morentouxiang" size="18px" />
                </el-avatar>

                <div class="relation-copy">
                  <div class="relation-name-row">
                    <p class="relation-name">{{ item.otherNickName || item.otherUserId || '未知用户' }}</p>
                    <span class="relation-tag" :class="{ mutual: item.focusType === 1 }">
                      {{ item.focusType === 1 ? '互关' : (activeTab === 'focus' ? '已关注' : '粉丝') }}
                    </span>
                  </div>

                  <p class="relation-intro">{{ item.otherPersonIntroduction || relationIntroFallbackText }}</p>
                  <p class="relation-time">{{ relationPanelTimeLabel }}: {{ formatDateTime(item.focusTime) }}</p>
                </div>
              </button>
            </div>

            <el-empty v-else :description="relationPanelEmptyText" />
          </template>
        </div>

        <div class="relation-content-footer">
          <p class="relation-total" v-if="relationPanelTotalCount > 0">{{ relationPanelTotalText }}</p>
          <el-pagination
            v-if="relationPanelPageTotal > 1"
            background
            layout="prev, pager, next"
            :current-page="relationPanelPageNo"
            :page-count="relationPanelPageTotal"
            @current-change="handleRelationPanelPageChange"
          />
        </div>
      </section>

      <section
        v-if="!pageError && userProfile && activeTab === 'collection'"
        class="panel content-strip collection-content-strip"
        :aria-label="collectionAriaText"
      >
        <div class="collection-content-head">
          <h2>收藏</h2>
        </div>

        <div class="collection-content-body" v-loading="collectionLoading">
          <el-alert
            v-if="collectionError"
            type="error"
            show-icon
            :closable="false"
            :title="collectionError"
          />

          <template v-else>
            <div v-if="collectionList.length > 0" class="collection-video-grid">
              <article
                v-for="(row, index) in collectionList"
                :key="row.actionId || row.videoId || `${row.videoName || 'collection'}-${index}`"
                class="collection-video-card"
                role="button"
                tabindex="0"
                @click="goVideoPlay(row.videoId)"
                @keydown.enter="goVideoPlay(row.videoId)"
              >
                <div class="collection-video-cover-wrap">
                  <img
                    v-if="row.videoCover"
                    class="collection-video-cover"
                    :src="toResourceUrl(row.videoCover)"
                    alt="收藏视频封面"
                  />
                  <div v-else class="collection-video-cover collection-video-cover-empty">暂无封面</div>
                </div>

                <div class="collection-video-copy">
                  <h3 class="collection-video-title" :title="row.videoName || '未命名视频'">
                    {{ row.videoName || '未命名视频' }}
                  </h3>
                  <p class="collection-video-time">收藏于 {{ formatDateTime(row.actionTime) }}</p>
                </div>
              </article>
            </div>

            <el-empty v-else :description="collectionEmptyText" />
          </template>
        </div>

        <div class="collection-content-footer">
          <p class="collection-total" v-if="collectionTotalCount > 0">共 {{ collectionTotalCount }} 条</p>
          <el-pagination
            v-if="collectionPageTotal > 1"
            background
            layout="prev, pager, next"
            :current-page="collectionPageNo"
            :page-count="collectionPageTotal"
            @current-change="handleCollectionPageChange"
          />
        </div>
      </section>
    </div>

    <el-dialog
      v-model="editVisible"
      width="640px"
      title="编辑个人资料"
      :close-on-click-modal="false"
      append-to-body
    >
      <el-form label-position="top" class="edit-form">
        <el-form-item label="昵称">
          <el-input v-model="editForm.nickName" maxlength="20" show-word-limit />
        </el-form-item>

        <el-form-item label="头像">
          <div class="avatar-edit-row">
            <el-avatar :src="toResourceUrl(editForm.avatar)" :size="46">
              <IconFont name="icon-morentouxiang" size="20px" />
            </el-avatar>
            <button type="button" class="upload-trigger" @click="openAvatarPicker">
              {{ avatarUploading ? '上传中...' : '上传头像' }}
            </button>
            <span class="avatar-tip">资源路径：{{ editForm.avatar || '--' }}</span>
          </div>
          <input
            ref="avatarInputRef"
            class="hidden-file"
            type="file"
            accept="image/*"
            @change="handleAvatarChange"
          />
        </el-form-item>

        <el-form-item label="性别">
          <el-select v-model="editForm.sex" placeholder="请选择">
            <el-option :value="0" label="女" />
            <el-option :value="1" label="男" />
            <el-option :value="2" label="未知" />
          </el-select>
        </el-form-item>

        <el-form-item label="生日">
          <el-date-picker
            v-model="editForm.birthday"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择生日"
          />
        </el-form-item>

        <el-form-item label="学校">
          <el-input v-model="editForm.school" maxlength="150" />
        </el-form-item>

        <el-form-item label="个人简介">
          <el-input
            v-model="editForm.personIntroduction"
            type="textarea"
            :rows="3"
            maxlength="80"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="公告">
          <el-input
            v-model="editForm.noticeInfo"
            type="textarea"
            :rows="3"
            maxlength="300"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="edit-actions">
          <el-button @click="cancelEdit">取消</el-button>
          <el-button type="primary" :loading="editSubmitting" @click="submitEdit">保存资料</el-button>
        </div>
      </template>
    </el-dialog>

    <AuthDialog />
  </div>
</template>

<style scoped>
.user-center-page {
  --page-bg-top: #f5fbff;
  --page-bg-bottom: #eef6ff;
  --panel-bg: rgba(255, 255, 255, 0.96);
  --panel-border: rgba(145, 177, 225, 0.32);
  --panel-shadow: 0 10px 26px rgba(31, 66, 118, 0.09);
  --title-color: #153866;
  --muted-color: #5e769a;
  --brand-color: #1d6fe7;
  --brand-light: #ecf4ff;
  --teal-color: #11a89d;
  min-height: 100vh;
  padding: 20px clamp(14px, 4vw, 42px) 30px;
  background:
    radial-gradient(920px 400px at 2% -16%, rgba(29, 111, 231, 0.13), transparent 70%),
    radial-gradient(760px 320px at 96% 8%, rgba(17, 168, 157, 0.1), transparent 70%),
    linear-gradient(180deg, var(--page-bg-top), var(--page-bg-bottom));
}

.panel {
  border-radius: 18px;
  border: 1px solid var(--panel-border);
  background: var(--panel-bg);
  box-shadow: var(--panel-shadow);
}

.top-strip-shell {
  width: min(100%, 1240px);
  margin: 0 auto;
  position: relative;
  z-index: 40;
}

.sub-strip-shell {
  width: min(100%, 1160px);
  margin: 0 auto;
  position: relative;
  z-index: 1;
}

.top-strip {
  min-height: 108px;
  padding: 20px;
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: 16px;
  position: relative;
  z-index: 40;
  overflow: visible;
}

.home-link {
  margin: 0;
  color: var(--title-color);
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 0.4px;
  cursor: pointer;
  user-select: none;
  transition: color 0.2s ease;
  justify-self: start;
}

.home-link:hover,
.home-link:focus-visible {
  color: var(--brand-color);
  outline: none;
}

.top-search {
  width: clamp(240px, 30vw, 360px);
  justify-self: center;
  position: relative;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.top-search :deep(.el-input) {
  flex: 1;
}

.top-search-btn {
  border: 1px solid #c9daf6;
  background: #f4f9ff;
  color: #2f63a9;
  border-radius: 999px;
  min-height: 40px;
  padding: 0 14px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: border-color 0.2s ease, background-color 0.2s ease;
}

.top-search-btn:hover {
  border-color: #95b8eb;
  background: #eaf3ff;
}

.top-search-btn:focus-visible {
  outline: 2px solid #8fb2ea;
  outline-offset: 2px;
}

.top-search-suggest-panel {
  position: absolute;
  left: 0;
  right: 0;
  top: calc(100% + 10px);
  z-index: 60;
  border-radius: 10px;
  border: 1px solid #d4e3fb;
  background: #ffffff;
  box-shadow: 0 10px 22px rgba(31, 66, 118, 0.12);
  padding: 8px;
}

.top-search-suggest-title {
  margin: 0;
  color: #5d769b;
  font-size: 11px;
}

.top-search-suggest-list {
  margin-top: 6px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.top-search-suggest-item {
  border: 1px solid #d3e2fb;
  border-radius: 999px;
  background: #f4f8ff;
  color: #2f5d99;
  min-height: 26px;
  padding: 0 9px;
  font-size: 11px;
  cursor: pointer;
  transition: border-color 0.2s ease, background-color 0.2s ease;
}

.top-search-suggest-item:hover {
  border-color: #95b8eb;
  background: #eaf3ff;
}

.top-actions {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  justify-self: end;
}

.account-chip {
  border: 1px solid #d4e4ff;
  background: #ffffff;
  color: var(--title-color);
  border-radius: 999px;
  min-height: 42px;
  padding: 0 12px 0 5px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: border-color 0.2s ease, background-color 0.2s ease;
}

.account-chip:hover {
  border-color: #8db5f3;
  background: #f4f9ff;
}

.action-btn {
  min-width: 92px;
}

.publish-btn {
  border: none;
  background: linear-gradient(135deg, #1d6fe7, #11a89d);
}

.profile-strip {
  margin-top: 14px;
  padding: 20px;
}

.profile-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.profile-user {
  display: inline-flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.profile-copy {
  min-width: 0;
}

.name-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.name-row h1 {
  margin: 0;
  color: var(--title-color);
  font-size: 26px;
  line-height: 1.2;
  word-break: break-word;
}

.sex-tag {
  border-radius: 999px;
  padding: 4px 9px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 600;
}

.sex-male {
  background: #eaf3ff;
  color: #316cc5;
}

.sex-female {
  background: #fff0f8;
  color: #bf4e89;
}

.sex-unknown {
  background: #eef2f9;
  color: #637798;
}

.sex-icon {
  font-size: 14px;
}

.edit-trigger {
  border: 1px solid #c7dafc;
  background: #f4f9ff;
  color: #2e5eaa;
  border-radius: 999px;
  width: 30px;
  height: 30px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: border-color 0.2s ease, background-color 0.2s ease;
}

.edit-trigger:hover {
  border-color: #8cb3ed;
  background: #eaf4ff;
}

.profile-id {
  margin: 6px 0 0;
  color: var(--muted-color);
  font-size: 13px;
}

.profile-action {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
}

.follow-btn {
  border: 1px solid #68a6ec;
  background: #edf6ff;
  color: #1d68d5;
  border-radius: 999px;
  min-height: 36px;
  padding: 0 18px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: border-color 0.2s ease, background-color 0.2s ease;
}

.follow-btn:hover {
  border-color: #4f98e8;
  background: #e3f1ff;
}

.follow-btn.active {
  border-color: #cedcf0;
  background: #f5f8fc;
  color: #5b7096;
}

.follow-btn:disabled {
  opacity: 0.75;
  cursor: wait;
}

.self-badge {
  border-radius: 999px;
  padding: 7px 14px;
  background: #f2f7ff;
  color: #6781aa;
  font-size: 12px;
}

.switch-strip {
  margin-top: 14px;
  padding: 16px;
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: 14px;
}

.switch-tabs {
  display: inline-flex;
  align-items: center;
  gap: 18px;
  justify-self: start;
}

.switch-tab {
  border: none;
  border-bottom: 2px solid transparent;
  background: transparent;
  color: #6e83a8;
  min-width: 0;
  min-height: 0;
  border-radius: 0;
  padding: 6px 0 9px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: color 0.2s ease, border-bottom-color 0.2s ease;
}

.switch-tab:hover {
  color: #2a5890;
}

.switch-tab.active {
  color: #1d6fe7;
  border-bottom-color: #1d6fe7;
}

.switch-tab:focus-visible {
  outline: none;
  color: #1d6fe7;
  border-bottom-color: #1d6fe7;
}

.switch-search {
  width: clamp(200px, 26vw, 320px);
  display: flex;
  align-items: center;
  gap: 8px;
  justify-self: center;
}

.switch-search :deep(.el-input) {
  flex: 1;
}

.tab-search-btn {
  border: 1px solid #c9daf6;
  background: #f4f9ff;
  color: #2f63a9;
  border-radius: 999px;
  min-height: 34px;
  padding: 0 13px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: border-color 0.2s ease, background-color 0.2s ease, color 0.2s ease;
}

.tab-search-btn:hover {
  border-color: #95b8eb;
  background: #eaf3ff;
}

.tab-search-btn:focus-visible {
  outline: 2px solid #8fb2ea;
  outline-offset: 2px;
}

.tab-search-btn:disabled {
  opacity: 0.66;
  cursor: not-allowed;
}

.switch-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(66px, 1fr));
  gap: 10px;
  justify-self: end;
}

.metric-card {
  border-radius: 11px;
  background: #f8fbff;
  border: 1px solid #d8e7fb;
  padding: 8px 10px;
  text-align: center;
}

.metric-card-clickable {
  cursor: pointer;
  transition: border-color 0.2s ease, background-color 0.2s ease, transform 0.2s ease;
}

.metric-card-clickable:hover {
  border-color: #9abff3;
  background: #eef5ff;
}

.metric-card-clickable:focus-visible {
  outline: 2px solid #89ade8;
  outline-offset: 2px;
}

.metric-card-clickable:active {
  transform: translateY(1px);
}

.metric-label {
  margin: 0;
  color: #6481ab;
  font-size: 12px;
}

.metric-value {
  margin: 6px 0 0;
  color: #173b6a;
  font-size: 18px;
  font-weight: 700;
}

.intro-strip {
  margin-top: 14px;
  padding: 16px 18px;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: 12px;
  background: linear-gradient(90deg, rgba(29, 111, 231, 0.08), rgba(17, 168, 157, 0.05) 45%, #f9fcff 100%);
}

.intro-label {
  margin: 0;
  border-radius: 999px;
  padding: 5px 11px;
  background: #eaf2ff;
  color: #24569f;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.3px;
}

.intro-text {
  margin: 0;
  color: #27486f;
  font-size: 14px;
  line-height: 1.7;
  word-break: break-word;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.home-info-strip {
  margin-top: 14px;
  padding: 18px;
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(240px, 1fr);
  gap: 12px;
  align-items: stretch;
  background:
    radial-gradient(520px 200px at 6% 0%, rgba(29, 111, 231, 0.09), transparent 72%),
    #fafdff;
}

.home-info-notice-card,
.home-info-meta-card {
  border-radius: 14px;
  border: 1px solid #d7e6fb;
  background: #ffffff;
  box-shadow: 0 7px 18px rgba(28, 86, 159, 0.08);
}

.home-info-notice-card {
  padding: 16px 16px 14px;
}

.home-info-notice-card h3,
.home-info-meta-card h3 {
  margin: 0;
  color: #2a5e9f;
  font-size: 13px;
  font-weight: 700;
}

.home-info-notice {
  margin: 10px 0 0;
  color: #20476f;
  font-size: 14px;
  line-height: 1.75;
  min-height: 76px;
  word-break: break-word;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.home-info-meta {
  display: grid;
  grid-template-rows: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.home-info-meta-card {
  padding: 14px 14px 12px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
}

.home-info-meta-card p {
  margin: 0;
  color: #1f4268;
  font-size: 16px;
  font-weight: 700;
  line-height: 1.5;
}

.content-strip {
  margin-top: 14px;
  padding: 26px 24px;
  min-height: 220px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
  background:
    radial-gradient(440px 160px at 9% 0%, rgba(29, 111, 231, 0.08), transparent 72%),
    #fbfdff;
}

.content-strip h2 {
  margin: 0;
  color: #1e477d;
  font-size: 22px;
  font-weight: 700;
}

.content-strip p {
  margin: 0;
  color: #5d789f;
  font-size: 14px;
  line-height: 1.7;
}

.video-content-strip {
  justify-content: flex-start;
  min-height: 320px;
  padding: 20px;
  gap: 14px;
}

.video-content-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}

.video-content-head h2 {
  margin: 0;
}

.post-order-tabs {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.post-order-tab {
  border: 1px solid #c8d9f5;
  background: #f5f9ff;
  color: #2b578d;
  border-radius: 999px;
  min-height: 30px;
  padding: 0 12px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: border-color 0.2s ease, background-color 0.2s ease, color 0.2s ease;
}

.post-order-tab:hover {
  border-color: #95b7ea;
  background: #eaf3ff;
}

.post-order-tab.active {
  border-color: #1d6fe7;
  background: #1d6fe7;
  color: #ffffff;
}

.post-order-tab:focus-visible {
  outline: 2px solid #8fb2ea;
  outline-offset: 2px;
}

.video-content-body {
  min-height: 220px;
}

.user-video-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.user-video-card {
  border-radius: 14px;
  border: 1px solid #d8e6fb;
  background: #ffffff;
  padding: 10px;
  cursor: pointer;
  transition: border-color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;
}

.user-video-card:hover {
  border-color: #9bbef0;
  transform: translateY(-1px);
  box-shadow: 0 10px 20px rgba(32, 71, 126, 0.1);
}

.user-video-card:focus-visible {
  outline: 2px solid #89ade8;
  outline-offset: 2px;
}

.user-video-cover-wrap {
  position: relative;
  border-radius: 10px;
  overflow: hidden;
  aspect-ratio: 16 / 9;
  background: linear-gradient(135deg, #dce9ff, #eef5ff);
}

.user-video-cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.user-video-cover-empty {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6b84aa;
  font-size: 13px;
}

.user-video-overlay {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 10px;
  background: linear-gradient(180deg, transparent, rgba(19, 37, 62, 0.78));
}

.user-video-overlay .overlay-item {
  color: #f2f7ff;
  font-size: 11px;
  line-height: 1.2;
}

.user-video-title {
  margin: 10px 0 0;
  color: #173b6a;
  font-size: 14px;
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 40px;
}

.user-video-meta {
  margin: 8px 0 0;
  color: #6d83a7;
  font-size: 12px;
}

.video-content-footer {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.video-total {
  margin: 0;
  color: #5f7598;
  font-size: 12px;
}

.collection-content-strip {
  justify-content: flex-start;
  min-height: 300px;
  padding: 20px;
  gap: 14px;
}

.collection-content-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.collection-content-head h2 {
  margin: 0;
}

.collection-content-body {
  min-height: 220px;
}

.collection-video-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.collection-video-card {
  border-radius: 14px;
  border: 1px solid #d8e6fb;
  background: #ffffff;
  padding: 10px;
  display: grid;
  grid-template-columns: 148px minmax(0, 1fr);
  gap: 12px;
  cursor: pointer;
  transition: border-color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;
}

.collection-video-card:hover {
  border-color: #9bbef0;
  transform: translateY(-1px);
  box-shadow: 0 10px 20px rgba(32, 71, 126, 0.1);
}

.collection-video-card:focus-visible {
  outline: 2px solid #89ade8;
  outline-offset: 2px;
}

.collection-video-cover-wrap {
  border-radius: 10px;
  overflow: hidden;
  aspect-ratio: 16 / 9;
  background: linear-gradient(135deg, #dce9ff, #eef5ff);
}

.collection-video-cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.collection-video-cover-empty {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6b84aa;
  font-size: 13px;
}

.collection-video-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
}

.collection-video-title {
  margin: 0;
  color: #173b6a;
  font-size: 14px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.collection-video-time {
  margin: 0;
  color: #6d83a7;
  font-size: 12px;
}

.collection-content-footer {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.collection-total {
  margin: 0;
  color: #5f7598;
  font-size: 12px;
}

.edit-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.edit-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.edit-form :deep(.el-form-item:last-child),
.edit-form :deep(.el-form-item:nth-last-child(2)) {
  grid-column: 1 / -1;
}

.avatar-edit-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.upload-trigger {
  border: 1px solid #c7dafc;
  background: #f4f9ff;
  color: #2f62ae;
  border-radius: 999px;
  min-height: 30px;
  padding: 0 12px;
  font-size: 12px;
  cursor: pointer;
  transition: border-color 0.2s ease, background-color 0.2s ease;
}

.upload-trigger:hover {
  border-color: #87b0ef;
  background: #eaf4ff;
}

.avatar-tip {
  color: #6880ae;
  font-size: 12px;
  word-break: break-all;
}

.hidden-file {
  display: none;
}

.edit-actions {
  display: inline-flex;
  justify-content: flex-end;
  width: 100%;
  gap: 8px;
}

.relation-content-strip {
  justify-content: flex-start;
  min-height: 260px;
  padding: 18px;
  gap: 12px;
}

.relation-content-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.relation-content-head h2 {
  margin: 0;
  color: #1e477d;
  font-size: 22px;
  font-weight: 700;
}

.relation-content-body {
  min-height: 240px;
}

.relation-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 420px;
  overflow: auto;
  padding-right: 2px;
}

.relation-item {
  appearance: none;
  border-radius: 12px;
  border: 1px solid #d7e6fb;
  background: #ffffff;
  padding: 10px 12px;
  width: 100%;
  display: flex;
  align-items: center;
  gap: 12px;
  text-align: left;
  font: inherit;
  color: inherit;
  cursor: pointer;
  transition: border-color 0.2s ease, background-color 0.2s ease;
}

.relation-item:hover {
  border-color: #9abcf0;
  background: #f8fbff;
}

.relation-item:focus-visible {
  outline: 2px solid #89ade8;
  outline-offset: 2px;
}

.relation-copy {
  min-width: 0;
  flex: 1;
}

.relation-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.relation-name {
  margin: 0;
  color: #1b426f;
  font-size: 14px;
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.relation-tag {
  border-radius: 999px;
  padding: 2px 8px;
  background: #eef5ff;
  color: #2d63a7;
  font-size: 11px;
  font-weight: 600;
}

.relation-tag.mutual {
  background: #e8f8f2;
  color: #1f8c6f;
}

.relation-intro {
  margin: 5px 0 0;
  color: #5f7598;
  font-size: 12px;
  line-height: 1.5;
  word-break: break-word;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.relation-time {
  margin: 6px 0 0;
  color: #7f93af;
  font-size: 11px;
}

.relation-content-footer {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.relation-total {
  margin: 0;
  color: #5f7598;
  font-size: 12px;
}

@media (max-width: 1200px) {
  .top-strip-shell {
    width: min(100%, 1160px);
  }

  .sub-strip-shell {
    width: min(100%, 1080px);
  }

  .top-strip {
    grid-template-columns: 1fr;
    align-items: stretch;
  }

  .top-search {
    width: min(100%, 360px);
    justify-self: center;
  }

  .top-actions {
    justify-content: flex-start;
    flex-wrap: wrap;
    justify-self: start;
  }

  .switch-strip {
    grid-template-columns: 1fr;
    align-items: stretch;
  }

  .switch-search {
    width: min(100%, 320px);
    justify-self: start;
  }

  .switch-tabs,
  .switch-metrics {
    justify-self: start;
  }

  .switch-metrics {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .home-info-strip {
    grid-template-columns: 1fr;
  }

  .home-info-meta {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    grid-template-rows: none;
  }
}

@media (max-width: 900px) {
  .profile-main {
    flex-direction: column;
    align-items: flex-start;
  }

  .profile-action {
    width: 100%;
    justify-content: flex-start;
  }

  .switch-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .user-video-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .collection-video-grid {
    grid-template-columns: 1fr;
  }

  .collection-video-card {
    grid-template-columns: 138px minmax(0, 1fr);
  }

  .intro-strip {
    grid-template-columns: 1fr;
    align-items: flex-start;
    gap: 8px;
  }

  .edit-form {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .user-center-page {
    padding: 14px;
  }

  .top-strip {
    min-height: 0;
    padding: 14px;
  }

  .profile-strip,
  .switch-strip,
  .intro-strip,
  .home-info-strip,
  .relation-content-strip,
  .content-strip {
    padding: 14px;
  }

  .name-row h1 {
    font-size: 22px;
  }

  .switch-tabs {
    width: 100%;
    justify-content: space-between;
    gap: 0;
  }

  .switch-tab {
    flex: 1 1 0;
    text-align: center;
    padding-bottom: 8px;
  }

  .top-search {
    flex-wrap: wrap;
  }

  .top-search-btn {
    width: 100%;
  }

  .top-search,
  .switch-search {
    width: 100%;
    max-width: none;
  }

  .action-btn {
    min-width: 84px;
  }

  .home-info-meta {
    grid-template-columns: 1fr;
  }

  .home-info-notice {
    min-height: 0;
  }

  .video-content-strip {
    padding: 14px;
  }

  .collection-content-strip {
    padding: 14px;
  }

  .post-order-tabs {
    width: 100%;
  }

  .post-order-tab {
    flex: 1 1 calc(33.333% - 6px);
    text-align: center;
  }

  .user-video-grid {
    grid-template-columns: 1fr;
  }

  .collection-video-card {
    grid-template-columns: 1fr;
    gap: 10px;
  }

  .collection-content-footer {
    justify-content: flex-start;
  }

  .relation-item {
    padding: 10px;
  }

  .video-content-footer,
  .relation-content-footer,
  .collection-content-footer {
    justify-content: flex-start;
  }
}
</style>





