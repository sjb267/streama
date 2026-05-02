<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import Hls from 'hls.js'
import Artplayer from 'artplayer'
import artplayerPluginDanmuku from 'artplayer-plugin-danmuku'
import { focusUser, getUserInfo as getAuthorUserInfo } from '@/api/home'
import { doUserAction, getSearchKeywordTop, getVideoInfo, loadVideoList, loadVideoPList } from '@/api/video'
import { loadDanmu, postDanmu } from '@/api/danmu'
import AuthDialog from '@/components/AuthDialog.vue'
import IconFont from '@/components/IconFont.vue'
import VideoCommentSection from '@/components/VideoCommentSection.vue'
import { useAuthStore } from '@/stores/auth'

const VIDEO_ACTION_CONFIGS = [
  { key: 'like', label: '点赞', icon: 'icon-dianzan', countField: 'likeCount', actionType: 2 },
  { key: 'collect', label: '收藏', icon: 'icon-shoucang1', countField: 'collectCount', actionType: 3 },
  { key: 'coin', label: '投币', icon: 'icon-dashang', countField: 'coinCount', actionType: 4 },
]

const DANMU_MAX_LENGTH = 200
const DANMU_MODE_SCROLL = 1
const DANMU_MODE_TOP = 2
const DANMU_MODE_BOTTOM = 3

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const searchKeyword = ref('')
const searchHotKeywordList = ref([])
const searchHotLoading = ref(false)
const searchInputFocused = ref(false)
const searchAreaHovered = ref(false)
const detailLoading = ref(false)
const detailError = ref('')
const videoInfo = ref({})
const videoInfoActionList = ref([])
const authorProfile = ref(null)
const authorProfileLoading = ref(false)
const focusSubmitting = ref(false)
const pList = ref([])
const relatedVideoList = ref([])
const relatedVideoPageNo = ref(1)
const relatedVideoPageSize = ref(15)
const relatedVideoHasMore = ref(true)
const relatedVideoLoading = ref(false)
const relatedVideoError = ref('')
const activeFileId = ref('')
const videoRef = ref(null)
const playerShellRef = ref(null)
const artPlayerRef = ref(null)
const danmuMountRef = ref(null)
const relatedVideoSentinelRef = ref(null)
const actionPendingMap = reactive({
  2: false,
  3: false,
  4: false,
})
const danmuLoading = ref(false)

let hlsPlayer = null
let currentManifestObjectUrl = ''
let streamLoadToken = 0
let artPlayer = null
let relatedVideoObserver = null
let relatedVideoLoadToken = 0
let pageDataLoadToken = 0
let manifestRequestController = null
let searchHotRequestToken = 0
let searchHotKeywordTimer = null
let searchHideHotTimer = null
const PLAY_DEBUG_ENABLED = Boolean(import.meta.env.DEV)

function debugPlay(event, payload = {}) {
  if (!PLAY_DEBUG_ENABLED) {
    return
  }
  console.debug(`[VideoPlay] ${event}`, payload)
}

const currentVideoId = computed(() => String(route.params.videoId || '').trim())
const currentPartIndex = computed(() => {
  const rawValue = String(route.query?.p || route.query?.fileIndex || '').trim()
  const numericValue = Number(rawValue)
  if (!Number.isInteger(numericValue) || numericValue <= 0) {
    return 0
  }
  return numericValue
})
const currentPartFileId = computed(() => String(route.query?.fileId || '').trim())
const publisherUserId = computed(() => String(videoInfo.value?.userId || '').trim())
const displayTitle = computed(() => String(videoInfo.value?.videoName || '未命名视频'))
const displayAuthorName = computed(() => String(
  authorProfile.value?.nickName ||
  authorProfile.value?.nickname ||
  videoInfo.value?.nickName ||
  videoInfo.value?.nickname ||
  'UP主',
))
const displayUpdateTime = computed(() => formatDate(videoInfo.value?.lastUpdateTime))
const displayTagList = computed(() => normalizeTagList(videoInfo.value?.tags))
const authorAvatar = computed(() => toResourceUrl(videoInfo.value?.avatar || ''))

const isLoggedIn = computed(() => authStore.isLoggedIn)
const displayNickName = computed(() => authStore.userInfo?.nickName || '未登录')
const resolvedAvatar = computed(() => toResourceUrl(authStore.userInfo?.avatar || ''))
const currentUserId = computed(() => String(authStore.userInfo?.userId || '').trim())
const isSelfAuthor = computed(() => {
  return Boolean(currentUserId.value) && currentUserId.value === publisherUserId.value
})
const hasFocusedAuthor = computed(() => {
  const focusValue = authorProfile.value?.haveFocus
  if (typeof focusValue === 'boolean') {
    return focusValue
  }
  if (typeof focusValue === 'number') {
    return focusValue === 1
  }
  const focusText = String(focusValue || '').trim().toLowerCase()
  return focusText === '1' || focusText === 'true'
})
const showAuthorFocusEntry = computed(() => {
  return Boolean(publisherUserId.value) && !isSelfAuthor.value
})
const showSearchHotPanel = computed(() => {
  return (
    (searchInputFocused.value || searchAreaHovered.value) &&
    (searchHotLoading.value || searchHotKeywordList.value.length > 0)
  )
})

const activeVideoActionTypeSet = computed(() => {
  const activeSet = new Set()
  const loginUserId = String(currentUserId.value || '').trim()
  if (!isLoggedIn.value || !loginUserId) {
    return activeSet
  }

  videoInfoActionList.value.forEach((item) => {
    const actionType = Number(item?.actionType ?? -1)
    const actionCount = Number(item?.actionCount ?? 0)
    const actionUserId = String(item?.userId || '').trim()
    if (!actionUserId || actionUserId !== loginUserId) {
      return
    }
    if ([2, 3, 4].includes(actionType) && actionCount > 0) {
      activeSet.add(actionType)
    }
  })
  return activeSet
})

const videoActionButtons = computed(() => {
  return VIDEO_ACTION_CONFIGS.map((item) => ({
    ...item,
    count: normalizeCount(videoInfo.value?.[item.countField]),
    active: activeVideoActionTypeSet.value.has(item.actionType),
    loading: Boolean(actionPendingMap[item.actionType]),
  }))
})
const isDanmuClosed = computed(() => String(videoInfo.value?.interaction || '').includes('1'))

function toResourceUrl(path) {
  return `/file/getResource?sourceName=${path}`
}

function normalizeCount(value) {
  const numericValue = Number(value)
  if (!Number.isFinite(numericValue) || numericValue < 0) {
    return 0
  }
  return Math.floor(numericValue)
}

function formatDate(value) {
  if (!value) {
    return '--'
  }
  const rawText = String(value).trim()
  if (!rawText) {
    return '--'
  }
  if (/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/.test(rawText) || /^\d{4}-\d{2}-\d{2}$/.test(rawText)) {
    return rawText
  }
  if (/^\d{4}-\d{2}-\d{2}T/.test(rawText)) {
    return rawText.replace('T', ' ').slice(0, 19)
  }

  const parsedDate = new Date(rawText)
  if (Number.isNaN(parsedDate.getTime())) {
    return rawText
  }
  const year = parsedDate.getFullYear()
  const month = String(parsedDate.getMonth() + 1).padStart(2, '0')
  const day = String(parsedDate.getDate()).padStart(2, '0')
  const hours = String(parsedDate.getHours()).padStart(2, '0')
  const minutes = String(parsedDate.getMinutes()).padStart(2, '0')
  const seconds = String(parsedDate.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
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

function normalizeTagList(value) {
  if (Array.isArray(value)) {
    return Array.from(new Set(
      value
        .map((item) => String(item || '').trim())
        .filter(Boolean),
    ))
  }

  const rawText = String(value || '').trim()
  if (!rawText) {
    return []
  }

  return Array.from(new Set(
    rawText
      .split(/[,\uff0c\u3001|]/)
      .map((item) => item.trim())
      .filter(Boolean),
  ))
}

function normalizeSearchKeyword(value) {
  return String(value || '').trim()
}

function normalizeSearchKeywordList(list = []) {
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
    const normalized = normalizeSearchKeyword(keywordText)
    if (!normalized || keywordSet.has(normalized)) {
      return
    }
    keywordSet.add(normalized)
    keywordResult.push(normalized)
  })
  return keywordResult.slice(0, 10)
}

function clearSearchHotTimer() {
  if (!searchHotKeywordTimer) {
    return
  }
  clearTimeout(searchHotKeywordTimer)
  searchHotKeywordTimer = null
}

function clearSearchHideHotTimer() {
  if (!searchHideHotTimer) {
    return
  }
  clearTimeout(searchHideHotTimer)
  searchHideHotTimer = null
}

function clearSearchHotState() {
  searchHotRequestToken += 1
  searchHotLoading.value = false
  searchHotKeywordList.value = []
}

async function loadSearchHotKeywords(keyword = '') {
  const targetKeyword = normalizeSearchKeyword(keyword)
  const requestToken = ++searchHotRequestToken
  searchHotLoading.value = true
  try {
    const data = await getSearchKeywordTop({
      keyword: targetKeyword,
    })
    if (requestToken !== searchHotRequestToken) {
      return
    }
    const list = Array.isArray(data) ? data : Array.isArray(data?.list) ? data.list : []
    searchHotKeywordList.value = normalizeSearchKeywordList(list)
  } catch (_error) {
    if (requestToken !== searchHotRequestToken) {
      return
    }
    searchHotKeywordList.value = []
  } finally {
    if (requestToken === searchHotRequestToken) {
      searchHotLoading.value = false
    }
  }
}

function queueLoadSearchHotKeywords(keyword = '') {
  const targetKeyword = normalizeSearchKeyword(keyword)
  clearSearchHotTimer()
  searchHotKeywordTimer = setTimeout(() => {
    loadSearchHotKeywords(targetKeyword)
  }, 260)
}

function normalizeRelatedVideoList(list = []) {
  if (!Array.isArray(list)) {
    return []
  }

  return list
    .map((item) => ({
      ...item,
      videoId: String(item?.videoId || '').trim(),
      videoCover: String(item?.videoCover || '').trim(),
      videoName: String(item?.videoName || '').trim(),
      userId: String(item?.userId || '').trim(),
      nickName: String(item?.nickName || item?.nickname || '').trim(),
      createTime: item?.createTime,
      lastUpdateTime: item?.lastUpdateTime,
      duration: Number(item?.duration || 0),
    }))
    .filter((item) => item.videoId)
}

function mergeRelatedVideoList(currentList = [], incomingList = []) {
  const mergedList = [...currentList]
  const seenVideoIds = new Set(mergedList.map((item) => String(item?.videoId || '').trim()))

  incomingList.forEach((item) => {
    const targetVideoId = String(item?.videoId || '').trim()
    if (!targetVideoId || seenVideoIds.has(targetVideoId)) {
      return
    }
    seenVideoIds.add(targetVideoId)
    mergedList.push(item)
  })

  return mergedList
}

function getStreamUrl(fileId) {
  const id = String(fileId || '').trim()
  if (!id) {
    return ''
  }
  return `/file/videoResource/${encodeURIComponent(id)}`
}

function getTsBaseUrl(fileId) {
  const id = String(fileId || '').trim()
  if (!id) {
    return ''
  }
  return `${window.location.origin}/file/videoResource/${encodeURIComponent(id)}/`
}

function isAbsoluteUri(value) {
  return /^(?:[a-z][a-z0-9+.-]*:)?\/\//i.test(value) || value.startsWith('/') || value.startsWith('data:')
}

function canResolveUri(value) {
  const source = String(value || '').trim()
  if (!source) {
    return false
  }
  const baseUrl = typeof window === 'undefined' ? 'http://localhost' : window.location.origin
  try {
    new URL(source, baseUrl)
    return true
  } catch (_error) {
    return false
  }
}

function resolveManifestUri(fileId, uri) {
  const source = String(uri || '').trim()
  if (!source) {
    return source
  }
  if (isAbsoluteUri(source)) {
    return source
  }
  const baseUrl = getTsBaseUrl(fileId)
  if (!baseUrl) {
    return source
  }
  return new URL(source, baseUrl).toString()
}

function extractManifestSegmentUris(manifestText = '') {
  return String(manifestText || '')
    .split(/\r?\n/)
    .map((line) => String(line || '').trim())
    .filter((line) => line && !line.startsWith('#'))
}

function validateRewrittenManifest(fileId, manifestText, firstSegmentUrl) {
  const segmentUris = extractManifestSegmentUris(manifestText)
  if (segmentUris.length === 0) {
    return {
      valid: false,
      reason: 'segment_missing',
      segmentCount: 0,
    }
  }

  const validSegmentCount = segmentUris.filter((uri) => {
    const resolved = resolveManifestUri(fileId, uri)
    return canResolveUri(resolved)
  }).length

  if (validSegmentCount === 0) {
    return {
      valid: false,
      reason: 'segment_invalid',
      segmentCount: 0,
    }
  }

  if (!canResolveUri(firstSegmentUrl)) {
    return {
      valid: false,
      reason: 'first_segment_invalid',
      segmentCount: validSegmentCount,
    }
  }

  return {
    valid: true,
    reason: '',
    segmentCount: validSegmentCount,
  }
}

function rewriteManifestContent(fileId, manifestText) {
  const source = String(manifestText || '')
  const lines = source.split(/\r?\n/)
  let firstSegmentUrl = ''

  const rewrittenLines = lines.map((line) => {
    const raw = String(line || '')
    const trimmed = raw.trim()
    if (!trimmed) {
      return raw
    }

    if (trimmed.startsWith('#')) {
      if (trimmed.includes('URI=')) {
        return raw.replace(/URI=("([^"]*)"|'([^']*)')/g, (_match, quoted, dqValue, sqValue) => {
          const original = dqValue ?? sqValue ?? ''
          const nextUri = resolveManifestUri(fileId, original)
          const quote = quoted.startsWith("'") ? "'" : '"'
          return `URI=${quote}${nextUri}${quote}`
        })
      }
      return raw
    }

    const rewritten = resolveManifestUri(fileId, trimmed)
    if (!firstSegmentUrl) {
      firstSegmentUrl = rewritten
    }
    return rewritten
  })

  return {
    content: rewrittenLines.join('\n'),
    firstSegmentUrl,
  }
}

function normalizePList(list = []) {
  return list
    .map((item) => ({
      ...item,
      fileId: String(item?.fileId || '').trim(),
      fileIndex: Number(item?.fileIndex || 0),
      fileName: String(item?.fileName || '').trim(),
    }))
    .filter((item) => item.fileId)
    .sort((a, b) => {
      const aIndex = Number(a.fileIndex || 0)
      const bIndex = Number(b.fileIndex || 0)
      if (aIndex === bIndex) {
        return a.fileId.localeCompare(b.fileId)
      }
      return aIndex - bIndex
    })
}

function normalizeVideoActionList(list = []) {
  if (!Array.isArray(list)) {
    return []
  }
  return list
    .map((item) => {
      const actionType = Number(item?.actionType ?? -1)
      const actionCount = Number(item?.actionCount ?? 0)
      return {
        ...item,
        userId: String(item?.userId || '').trim(),
        actionType,
        actionCount: Number.isFinite(actionCount) ? actionCount : 0,
      }
    })
    .filter((item) => [2, 3, 4].includes(item.actionType))
}

function syncDetailInfo(detailData = {}) {
  videoInfo.value = detailData?.videoInfo || {}
  const rawActionList = Array.isArray(detailData?.videoInfoFiles) ? detailData.videoInfoFiles : []
  videoInfoActionList.value = normalizeVideoActionList(rawActionList)
}

function getPDisplayName(item, index) {
  if (item?.fileName) {
    return item.fileName
  }
  return `P${Number(item?.fileIndex || index + 1)}`
}

function resolveFileIdByRoutePart(list = []) {
  const sourceList = Array.isArray(list) ? list : []
  const routeFileId = String(currentPartFileId.value || '').trim()
  if (routeFileId) {
    const matchedByFileId = sourceList.find((item) => item.fileId === routeFileId)
    if (matchedByFileId) {
      return matchedByFileId.fileId
    }
  }

  const routePartIndex = Number(currentPartIndex.value || 0)
  if (routePartIndex > 0) {
    const matchedByPartIndex = sourceList.find(
      (item) => Number(item?.fileIndex || 0) === routePartIndex,
    )
    if (matchedByPartIndex) {
      return matchedByPartIndex.fileId
    }
  }

  return ''
}

function syncActivePartFromRoute(list = [], options = {}) {
  const sourceList = Array.isArray(list) ? list : []
  if (sourceList.length === 0) {
    return false
  }

  const allowFallback = options?.allowFallback !== false
  const routeMatchedFileId = resolveFileIdByRoutePart(sourceList)
  const currentSelectedFileId = String(activeFileId.value || '').trim()
  const currentFileStillExists = Boolean(currentSelectedFileId)
    && sourceList.some((item) => item.fileId === currentSelectedFileId)

  const nextFileId = routeMatchedFileId
    || (allowFallback ? (currentFileStillExists ? currentSelectedFileId : (sourceList[0]?.fileId || '')) : '')
  if (!nextFileId) {
    return false
  }

  const forceAttach = Boolean(routeMatchedFileId) || !currentFileStillExists
  selectPartFile(nextFileId, {
    force: forceAttach,
    reason: routeMatchedFileId ? 'route-part' : 'route-sync',
  })
  return true
}

function resolveVideoPartRoute(videoId = '', part = {}) {
  const targetVideoId = String(videoId || '').trim()
  if (!targetVideoId) {
    return null
  }

  const targetFileId = String(part?.fileId || '').trim()
  const targetFileIndex = Number(part?.fileIndex || 0)
  const query = {}

  if (targetFileIndex > 0) {
    query.p = String(targetFileIndex)
  }
  if (targetFileId) {
    query.fileId = targetFileId
  }

  return router.resolve({
    path: `/video/${targetVideoId}`,
    query,
  })
}

function resetRelatedVideoListState() {
  relatedVideoLoadToken += 1
  relatedVideoList.value = []
  relatedVideoPageNo.value = 1
  relatedVideoPageSize.value = 15
  relatedVideoHasMore.value = true
  relatedVideoLoading.value = false
  relatedVideoError.value = ''
}

async function loadRelatedVideoList() {
  if (relatedVideoLoading.value || !relatedVideoHasMore.value) {
    return
  }

  const requestToken = relatedVideoLoadToken
  const requestPageNo = Math.max(1, Number(relatedVideoPageNo.value || 1))
  const requestPageSize = Math.max(1, Number(relatedVideoPageSize.value || 15))

  relatedVideoLoading.value = true
  relatedVideoError.value = ''

  try {
    const data = await loadVideoList({
      pageNo: requestPageNo,
      pageSize: requestPageSize,
    })
    if (requestToken !== relatedVideoLoadToken) {
      return
    }

    const incomingList = normalizeRelatedVideoList(data?.list)
    relatedVideoList.value = mergeRelatedVideoList(relatedVideoList.value, incomingList)

    const responsePageNo = Math.max(1, Number(data?.pageNo || requestPageNo))
    const responsePageSize = Math.max(1, Number(data?.pageSize || requestPageSize))
    const responsePageTotal = Math.max(0, Number(data?.pageTotal || 0))
    const responseTotalCount = Math.max(0, Number(data?.totalCount || 0))

    relatedVideoPageNo.value = responsePageNo + 1
    relatedVideoPageSize.value = responsePageSize

    if (responsePageTotal > 0) {
      relatedVideoHasMore.value = responsePageNo < responsePageTotal
    } else if (responseTotalCount > 0) {
      relatedVideoHasMore.value = relatedVideoList.value.length < responseTotalCount
    } else {
      relatedVideoHasMore.value = incomingList.length >= responsePageSize
    }

    if (incomingList.length === 0) {
      relatedVideoHasMore.value = false
    }
  } catch (_error) {
    if (requestToken !== relatedVideoLoadToken) {
      return
    }
    relatedVideoError.value = '视频列表加载失败，请稍后重试'
  } finally {
    if (requestToken === relatedVideoLoadToken) {
      relatedVideoLoading.value = false
    }
  }
}

async function resetAndLoadRelatedVideoList() {
  resetRelatedVideoListState()
  await loadRelatedVideoList()
}

function destroyRelatedVideoObserver() {
  if (!relatedVideoObserver) {
    return
  }
  relatedVideoObserver.disconnect()
  relatedVideoObserver = null
}

function initRelatedVideoObserver() {
  destroyRelatedVideoObserver()
  if (typeof window === 'undefined' || typeof window.IntersectionObserver !== 'function') {
    return
  }

  relatedVideoObserver = new IntersectionObserver(
    (entries) => {
      if (entries.some((entry) => entry.isIntersecting)) {
        loadRelatedVideoList()
      }
    },
    {
      root: null,
      rootMargin: '0px 0px 240px 0px',
      threshold: 0.1,
    },
  )

  if (relatedVideoSentinelRef.value) {
    relatedVideoObserver.observe(relatedVideoSentinelRef.value)
  }
}

function normalizeDanmuMode(mode) {
  const numericMode = Number(mode)
  if ([DANMU_MODE_SCROLL, DANMU_MODE_TOP, DANMU_MODE_BOTTOM].includes(numericMode)) {
    return numericMode
  }
  return DANMU_MODE_SCROLL
}

function toPluginDanmuMode(mode) {
  const normalized = normalizeDanmuMode(mode)
  if (normalized === DANMU_MODE_TOP) {
    return 1
  }
  if (normalized === DANMU_MODE_BOTTOM) {
    return 2
  }
  return 0
}

function toBackendDanmuMode(mode) {
  const numericMode = Number(mode)
  if (numericMode === 1) {
    return DANMU_MODE_TOP
  }
  if (numericMode === 2) {
    return DANMU_MODE_BOTTOM
  }
  return DANMU_MODE_SCROLL
}

function getDanmuPlugin() {
  return artPlayer?.plugins?.artplayerPluginDanmuku || null
}

function resetDanmuRuntime() {
  const plugin = getDanmuPlugin()
  if (!plugin) {
    return
  }
  try {
    plugin.load([])
  } catch (_error) {
    // Ignore plugin reset errors.
  }
}

function destroyArtPlayer() {
  if (!artPlayer) {
    return
  }
  artPlayer.destroy(false)
  artPlayer = null
  videoRef.value = null
}

function createArtPlayer(url = '') {
  if (artPlayer || !artPlayerRef.value) {
    return
  }

  artPlayer = new Artplayer({
    container: artPlayerRef.value,
    url,
    type: 'm3u8',
    autoplay: true,
    pip: true,
    setting: true,
    fullscreen: true,
    fullscreenWeb: true,
    mutex: true,
    customType: {
      m3u8: (video, sourceUrl, art) => {
        if (Hls.isSupported()) {
          if (art.hls && typeof art.hls.destroy === 'function') {
            art.hls.destroy()
          }
          const hls = new Hls()
          hls.loadSource(sourceUrl)
          hls.attachMedia(video)
          art.hls = hls
          return
        }

        if (video.canPlayType('application/vnd.apple.mpegurl')) {
          video.src = sourceUrl
          return
        }

        ElMessage.error('当前浏览器不支持 m3u8 播放')
      },
    },
    plugins: [
      artplayerPluginDanmuku({
        danmuku: [],
        mount: danmuMountRef.value || undefined,
        width: 0,
        theme: 'light',
        emitter: !isDanmuClosed.value,
        visible: !isDanmuClosed.value,
        maxLength: DANMU_MAX_LENGTH,
        beforeEmit: async (danmu) => {
          if (isDanmuClosed.value) {
            return false
          }
          if (!isLoggedIn.value) {
            openLoginDialog()
            return false
          }

          const videoId = currentVideoId.value
          const fileId = activeFileId.value
          if (!videoId || !fileId) {
            return false
          }

          const text = String(danmu?.text || '').trim()
          if (!text) {
            return false
          }

          try {
            await postDanmu({
              videoId,
              fileId,
              text: text.slice(0, DANMU_MAX_LENGTH),
              mode: toBackendDanmuMode(danmu?.mode),
              color: String(danmu?.color || '#FFFFFF').trim() || '#FFFFFF',
              time: Math.max(0, Math.floor(Number(artPlayer?.currentTime || 0))),
            })
            return true
          } catch (_error) {
            return false
          }
        },
      }),
    ],
  })

  videoRef.value = artPlayer.video
}

function mountDanmuPanel() {
  const plugin = getDanmuPlugin()
  if (!plugin || !danmuMountRef.value) {
    return
  }
  plugin.mount(danmuMountRef.value)
}

async function setArtPlayerSource(url) {
  if (!url) {
    return
  }

  if (!artPlayer) {
    createArtPlayer(url)
    mountDanmuPanel()
    return
  }

  if (artPlayer.url === url) {
    return
  }

  destroyArtPlayer()
  await nextTick()
  createArtPlayer(url)
  mountDanmuPanel()
}

function clearActiveStreamState() {
  streamLoadToken += 1
  abortManifestRequest()
  resetDanmuRuntime()
  stopVideoElement()
  destroyHlsPlayer()
  revokeManifestObjectUrl()
}

function selectPartFile(fileId, options = {}) {
  const targetFileId = String(fileId || '').trim()
  const force = Boolean(options?.force)
  const reason = String(options?.reason || 'user')

  if (!targetFileId) {
    activeFileId.value = ''
    clearActiveStreamState()
    debugPlay('part:clear', {
      videoId: currentVideoId.value,
      reason,
    })
    return
  }

  if (!force && targetFileId === activeFileId.value) {
    debugPlay('part:skip-same', {
      videoId: currentVideoId.value,
      fileId: targetFileId,
      reason,
    })
    return
  }

  const previousFileId = activeFileId.value
  activeFileId.value = targetFileId
  debugPlay('part:active-change', {
    videoId: currentVideoId.value,
    fromFileId: previousFileId,
    fileId: targetFileId,
    reason,
  })
  attachStream(targetFileId)
}

async function reloadDanmu() {
  const videoId = currentVideoId.value
  const fileId = activeFileId.value

  if (!videoId || !fileId) {
    return
  }

  const plugin = getDanmuPlugin()
  if (!plugin) {
    return
  }

  danmuLoading.value = true
  try {
    const data = await loadDanmu({ videoId, fileId })
    const list = Array.isArray(data) ? data : []
    const pluginDanmuList = list
      .map((item) => ({
        text: String(item?.text || '').trim(),
        color: String(item?.color || '').trim() || '#FFFFFF',
        mode: toPluginDanmuMode(item?.mode),
        time: Math.max(0, Number(item?.time || 0)),
      }))
      .filter((item) => item.text)

    await plugin.load(pluginDanmuList)
    plugin.config({
      visible: !isDanmuClosed.value,
      emitter: !isDanmuClosed.value,
    })
    mountDanmuPanel()
  } catch (_error) {
    // Error message handled by request interceptor.
  } finally {
    danmuLoading.value = false
  }
}

function destroyHlsPlayer() {
  if (!hlsPlayer) {
    if (artPlayer?.hls && typeof artPlayer.hls.destroy === 'function') {
      artPlayer.hls.destroy()
      artPlayer.hls = null
    }
    return
  }
  hlsPlayer.destroy()
  hlsPlayer = null
}

function abortManifestRequest() {
  if (!manifestRequestController) {
    return
  }
  manifestRequestController.abort()
  manifestRequestController = null
}

function revokeManifestObjectUrl() {
  if (!currentManifestObjectUrl) {
    return
  }
  URL.revokeObjectURL(currentManifestObjectUrl)
  currentManifestObjectUrl = ''
}

function stopVideoElement() {
  const videoElement = videoRef.value
  if (!videoElement && !artPlayer) {
    return
  }
  if (videoElement) {
    videoElement.pause()
    videoElement.removeAttribute('src')
    videoElement.load()
  }
  if (artPlayer) {
    artPlayer.pause = true
  }
}

async function buildRewrittenManifestObjectUrl(fileId, signal) {
  const streamUrl = getStreamUrl(fileId)
  if (!streamUrl) {
    return {
      playUrl: '',
      useFallback: false,
      fallbackReason: '',
      segmentCount: 0,
      objectUrl: '',
      firstSegmentUrl: '',
      indexUrl: '',
    }
  }

  const response = await fetch(streamUrl, {
    method: 'GET',
    credentials: 'include',
    cache: 'no-store',
    signal,
  })
  if (!response.ok) {
    throw new Error(`索引请求失败: ${response.status}`)
  }

  const manifestText = await response.text()
  const rewritten = rewriteManifestContent(fileId, manifestText)
  const manifestValidation = validateRewrittenManifest(fileId, rewritten.content, rewritten.firstSegmentUrl)

  if (!manifestValidation.valid) {
    return {
      playUrl: streamUrl,
      useFallback: true,
      fallbackReason: manifestValidation.reason,
      segmentCount: manifestValidation.segmentCount,
      objectUrl: '',
      firstSegmentUrl: rewritten.firstSegmentUrl,
      indexUrl: streamUrl,
    }
  }

  const blob = new Blob([rewritten.content], { type: 'application/vnd.apple.mpegurl' })
  const objectUrl = URL.createObjectURL(blob)

  return {
    playUrl: objectUrl,
    useFallback: false,
    fallbackReason: '',
    segmentCount: manifestValidation.segmentCount,
    objectUrl,
    firstSegmentUrl: rewritten.firstSegmentUrl,
    indexUrl: streamUrl,
  }
}

async function attachStream(fileId) {
  if (!fileId) {
    streamLoadToken += 1
    abortManifestRequest()
    stopVideoElement()
    destroyHlsPlayer()
    revokeManifestObjectUrl()
    debugPlay('attach:clear', {
      videoId: currentVideoId.value,
    })
    return
  }

  const requestToken = ++streamLoadToken
  abortManifestRequest()
  const controller = new AbortController()
  manifestRequestController = controller
  const timeoutId = window.setTimeout(() => {
    controller.abort()
  }, 12000)
  const streamUrl = getStreamUrl(fileId)
  const debugContext = {
    videoId: currentVideoId.value,
    fileId: String(fileId || '').trim(),
    indexUrl: streamUrl,
    firstSegmentUrl: '',
    playUrl: streamUrl,
    segmentCount: 0,
    useFallback: false,
    fallbackReason: '',
  }
  debugPlay('attach:start', debugContext)

  if (!streamUrl) {
    debugPlay('attach:error', {
      ...debugContext,
      error: new Error('Invalid stream url'),
    })
    ElMessage.error('视频播放失败，请稍后重试')
    return
  }

  stopVideoElement()
  destroyHlsPlayer()
  revokeManifestObjectUrl()

  let rewrittenObjectUrl = ''
  let playbackUrl = streamUrl
  try {
    const rewritten = await buildRewrittenManifestObjectUrl(fileId, controller.signal)
    rewrittenObjectUrl = rewritten.objectUrl
    playbackUrl = rewritten.playUrl || streamUrl
    debugContext.indexUrl = rewritten.indexUrl || streamUrl
    debugContext.firstSegmentUrl = rewritten.firstSegmentUrl || ''
    debugContext.playUrl = playbackUrl
    debugContext.segmentCount = rewritten.segmentCount
    debugContext.useFallback = rewritten.useFallback
    debugContext.fallbackReason = rewritten.fallbackReason
    debugPlay('manifest:rewritten', debugContext)
  } catch (error) {
    const isAbortError = error?.name === 'AbortError'
    if (manifestRequestController === controller) {
      manifestRequestController = null
    }
    clearTimeout(timeoutId)
    if (isAbortError) {
      debugPlay('attach:aborted', debugContext)
      return
    }
    if (requestToken !== streamLoadToken) {
      return
    }
    debugPlay('attach:error', {
      ...debugContext,
      error,
    })
    console.error('[VideoPlay] Failed to load m3u8 index', {
      ...debugContext,
      error,
    })
    ElMessage.error('视频播放失败，请稍后重试')
    return
  }
  if (manifestRequestController === controller) {
    manifestRequestController = null
  }
  clearTimeout(timeoutId)

  if (requestToken !== streamLoadToken) {
    if (rewrittenObjectUrl) {
      URL.revokeObjectURL(rewrittenObjectUrl)
    }
    debugPlay('attach:stale', debugContext)
    return
  }

  currentManifestObjectUrl = rewrittenObjectUrl

  try {
    await setArtPlayerSource(playbackUrl)
    if (requestToken !== streamLoadToken) {
      debugPlay('attach:stale', debugContext)
      return
    }
    await reloadDanmu()
    debugPlay('attach:success', debugContext)
  } catch (_error) {
    debugPlay('attach:error', {
      ...debugContext,
      error: _error,
    })
    console.error('[VideoPlay] Failed to attach player source', debugContext)
    ElMessage.error('视频播放失败，请稍后重试')
  }
}

async function refreshVideoInfo() {
  const videoId = currentVideoId.value
  if (!videoId) {
    return
  }
  const detailData = await getVideoInfo({ videoId })
  syncDetailInfo(detailData)
}

async function loadPageData() {
  const videoId = currentVideoId.value
  if (!videoId) {
    detailError.value = '视频ID无效'
    return
  }

  const requestToken = ++pageDataLoadToken
  detailLoading.value = true
  detailError.value = ''

  try {
    const [detailData, pListData] = await Promise.all([
      getVideoInfo({ videoId }),
      loadVideoPList({ videoId }),
    ])
    if (requestToken !== pageDataLoadToken || videoId !== currentVideoId.value) {
      return
    }

    syncDetailInfo(detailData)

    const detailFiles = Array.isArray(detailData?.videoInfoFiles) ? detailData.videoInfoFiles : []
    const detailPFiles = detailFiles.filter((item) => String(item?.fileId || '').trim())
    const pFiles = Array.isArray(pListData) ? pListData : []
    const sourcePList = pFiles.length > 0 ? pFiles : detailPFiles

    pList.value = normalizePList(sourcePList)
    const synced = syncActivePartFromRoute(pList.value, {
      allowFallback: true,
    })
    debugPlay('part:auto-select', {
      videoId,
      fileId: activeFileId.value,
      partCount: pList.value.length,
      routePartIndex: currentPartIndex.value,
      routeFileId: currentPartFileId.value,
      synced,
    })
  } catch (_error) {
    if (requestToken !== pageDataLoadToken || videoId !== currentVideoId.value) {
      return
    }
    detailError.value = '视频信息加载失败，请稍后重试'
    videoInfo.value = {}
    videoInfoActionList.value = []
    pList.value = []
    selectPartFile('', {
      reason: 'load-error',
    })
  } finally {
    if (requestToken === pageDataLoadToken) {
      detailLoading.value = false
    }
  }
}

async function loadAuthorProfile() {
  const targetUserId = publisherUserId.value
  if (!targetUserId) {
    authorProfile.value = null
    return
  }

  authorProfileLoading.value = true
  try {
    const data = await getAuthorUserInfo({ userId: targetUserId })
    authorProfile.value = data || null
  } catch (_error) {
    authorProfile.value = null
  } finally {
    authorProfileLoading.value = false
  }
}

function openLoginDialog() {
  authStore.openAuthDialog('login')
}

function handleAvatarClick() {
  if (!isLoggedIn.value) {
    openLoginDialog()
  }
}

function handleSearchFocus() {
  searchInputFocused.value = true
  clearSearchHideHotTimer()
  if (searchHotKeywordList.value.length === 0 && !searchHotLoading.value) {
    queueLoadSearchHotKeywords(searchKeyword.value)
  }
}

function handleSearchBlur() {
  clearSearchHideHotTimer()
  searchHideHotTimer = setTimeout(() => {
    searchInputFocused.value = false
  }, 120)
}

function handleSearchAreaMouseEnter() {
  searchAreaHovered.value = true
  clearSearchHideHotTimer()
  if (searchHotKeywordList.value.length === 0 && !searchHotLoading.value) {
    queueLoadSearchHotKeywords(searchKeyword.value)
  }
}

function handleSearchAreaMouseLeave() {
  clearSearchHideHotTimer()
  searchHideHotTimer = setTimeout(() => {
    searchAreaHovered.value = false
  }, 120)
}

function handleSearchClear() {
  searchKeyword.value = ''
  queueLoadSearchHotKeywords('')
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

function handleSearchSubmit() {
  const keyword = normalizeSearchKeyword(searchKeyword.value)
  searchKeyword.value = keyword
  searchInputFocused.value = false
  searchAreaHovered.value = false
  clearSearchHideHotTimer()

  if (!keyword) {
    navigateWithPageRefresh('/')
    return
  }

  navigateWithPageRefresh({
    path: '/',
    query: {
      keyword,
      orderType: '0',
      pageNo: '1',
    },
  })
}

function handlePickSearchHotKeyword(keyword) {
  const normalized = normalizeSearchKeyword(keyword)
  if (!normalized) {
    return
  }
  searchKeyword.value = normalized
  handleSearchSubmit()
}

function navigateFromVideoPage(location) {
  navigateWithPageRefresh(location)
}

function goCreatorCenter() {
  navigateFromVideoPage('/creator')
}

function goCreatePage() {
  navigateFromVideoPage({
    path: '/creator',
    query: {
      tab: 'create',
    },
  })
}

function goUserCenter(userId = '') {
  const normalizedUserId = (typeof userId === 'string' || typeof userId === 'number')
    ? String(userId).trim()
    : ''
  if (!normalizedUserId) {
    navigateFromVideoPage('/user-center')
    return
  }
  navigateFromVideoPage({
    path: '/user-center',
    query: {
      userId: normalizedUserId,
    },
  })
}

function goPublisherCenter() {
  const targetUserId = String(publisherUserId.value || '').trim()
  if (!targetUserId) {
    return
  }
  goUserCenter(targetUserId)
}

function goVideoPlay(videoId = '') {
  const targetVideoId = String(videoId || '').trim()
  if (!targetVideoId) {
    return
  }
  navigateWithPageRefresh(`/video/${targetVideoId}`)
}

async function handleSignOut() {
  await authStore.signOut()
  ElMessage.success('已退出登录')
  navigateFromVideoPage('/')
}

async function handleFocusAuthor() {
  const focusUserId = publisherUserId.value
  if (!focusUserId || isSelfAuthor.value || hasFocusedAuthor.value || focusSubmitting.value) {
    return
  }

  if (!isLoggedIn.value) {
    openLoginDialog()
    return
  }

  focusSubmitting.value = true
  try {
    await focusUser({ focusUserId })
    authorProfile.value = {
      ...(authorProfile.value || {}),
      haveFocus: true,
    }
    ElMessage.success('关注成功')
    await loadAuthorProfile()
  } catch (_error) {
    // Error message handled by request interceptor.
  } finally {
    focusSubmitting.value = false
  }
}

async function handleVideoAction(actionType) {
  const targetActionType = Number(actionType)
  const videoId = currentVideoId.value
  if (!videoId || ![2, 3, 4].includes(targetActionType)) {
    return
  }

  if (!isLoggedIn.value) {
    openLoginDialog()
    return
  }

  if (actionPendingMap[targetActionType]) {
    return
  }

  actionPendingMap[targetActionType] = true
  try {
    await doUserAction({
      videoId,
      actionType: targetActionType,
      actionCount: 1,
      commentId: 0,
    })
    await refreshVideoInfo()
  } catch (_error) {
    // Error message handled by request interceptor.
  } finally {
    actionPendingMap[targetActionType] = false
  }
}

function handleSelectP(item) {
  const targetFileId = String(item?.fileId || '').trim()
  if (!targetFileId) {
    return
  }
  const targetFileIndex = Number(item?.fileIndex || 0)
  const targetRoute = resolveVideoPartRoute(currentVideoId.value, {
    fileId: targetFileId,
    fileIndex: targetFileIndex,
  })
  if (!targetRoute) {
    return
  }
  debugPlay('part:navigate-refresh', {
    videoId: currentVideoId.value,
    fromFileId: activeFileId.value,
    fromPartIndex: currentPartIndex.value,
    fromRouteFileId: currentPartFileId.value,
    toFileId: targetFileId,
    toPartIndex: targetFileIndex,
    href: targetRoute.href,
  })
  navigateWithPageRefresh(targetRoute.fullPath)
}

function goHome() {
  navigateFromVideoPage('/')
}

watch(
  [() => String(route.params.videoId || ''), currentPartIndex, currentPartFileId],
  async ([nextVideoId, nextPartIndex, nextFileId], [prevVideoId, prevPartIndex, prevFileId]) => {
    if (!nextVideoId) {
      return
    }

    const videoChanged = nextVideoId !== prevVideoId
    const partChanged = nextPartIndex !== prevPartIndex || nextFileId !== prevFileId

    if (videoChanged) {
      clearActiveStreamState()
      activeFileId.value = ''
      await loadPageData()
      resetAndLoadRelatedVideoList().catch(() => {})
      return
    }

    if (!partChanged) {
      return
    }

    const synced = syncActivePartFromRoute(pList.value, {
      allowFallback: true,
    })
    if (!synced) {
      await loadPageData()
    }
  },
)

watch(
  publisherUserId,
  () => {
    loadAuthorProfile().catch(() => {})
  },
  { immediate: true },
)

watch(
  () => authStore.isLoggedIn,
  (nextState, prevState) => {
    if (nextState === prevState) {
      return
    }
    refreshVideoInfo().catch(() => {})
    loadAuthorProfile().catch(() => {})
  },
)

watch(
  () => isDanmuClosed.value,
  (closed) => {
    const plugin = getDanmuPlugin()
    if (plugin) {
      plugin.config({
        visible: !closed,
        emitter: !closed,
      })
    }
    if (!closed) {
      reloadDanmu()
    }
  },
)

watch(
  () => searchKeyword.value,
  (value) => {
    queueLoadSearchHotKeywords(value)
  },
)
watch(danmuMountRef, () => {
  mountDanmuPanel()
})

watch(relatedVideoSentinelRef, (nextTarget, prevTarget) => {
  if (!relatedVideoObserver) {
    return
  }
  if (prevTarget) {
    relatedVideoObserver.unobserve(prevTarget)
  }
  if (nextTarget) {
    relatedVideoObserver.observe(nextTarget)
  }
})

onMounted(() => {
  authStore.initAutoLogin()
  initRelatedVideoObserver()
  loadPageData()
  resetAndLoadRelatedVideoList().catch(() => {})
})

onBeforeUnmount(() => {
  clearSearchHotTimer()
  clearSearchHideHotTimer()
  clearSearchHotState()
  clearActiveStreamState()
  relatedVideoLoadToken += 1
  pageDataLoadToken += 1
  destroyRelatedVideoObserver()
  destroyArtPlayer()
})
</script>

<template>
  <div class="video-play-page">
    <header class="play-header panel">
      <div class="brand-area">
        <div class="brand-logo">
          <IconFont name="icon-iconfont1" size="24px" />
        </div>
        <div class="brand-copy">
          <p
            class="brand-name brand-home-entry"
            role="link"
            tabindex="0"
            @click="goHome"
            @keydown.enter="goHome"
          >
            首页
          </p>
        </div>
      </div>

      <div
        class="search-area"
        @mouseenter="handleSearchAreaMouseEnter"
        @mouseleave="handleSearchAreaMouseLeave"
      >
        <el-input
          v-model="searchKeyword"
          class="search-input"
          clearable
          placeholder="搜索视频关键词"
          size="large"
          @focus="handleSearchFocus"
          @blur="handleSearchBlur"
          @keyup.enter="handleSearchSubmit"
          @clear="handleSearchClear"
        >
          <template #prefix>
            <IconFont name="icon-sousuo" />
          </template>
        </el-input>

        <button class="search-submit-btn" type="button" @click="handleSearchSubmit">
          搜索
        </button>

        <div v-if="showSearchHotPanel" class="search-suggest-panel">
          <p class="suggest-title">{{ searchHotLoading ? '热词更新中...' : '热门搜索' }}</p>
          <div class="suggest-list">
            <button
              v-for="(keyword, index) in searchHotKeywordList"
              :key="`${keyword}-${index}`"
              type="button"
              class="suggest-item"
              @mousedown.prevent
              @click="handlePickSearchHotKeyword(keyword)"
            >
              {{ keyword }}
            </button>
          </div>
        </div>
      </div>

      <div class="action-area">
        <el-popover v-if="isLoggedIn" trigger="hover" placement="bottom-end" :width="140">
          <template #reference>
            <button class="avatar-trigger" type="button">
              <el-avatar :src="resolvedAvatar" :size="36">
                <IconFont name="icon-morentouxiang" size="18px" />
              </el-avatar>
              <span>{{ displayNickName }}</span>
            </button>
          </template>
          <div class="user-menu">
            <button class="user-menu-item" type="button" @click="goUserCenter(currentUserId)">个人中心</button>
            <button class="user-menu-item danger" type="button" @click="handleSignOut">退出登录</button>
          </div>
        </el-popover>

        <button v-else class="avatar-trigger" type="button" @click="handleAvatarClick">
          <el-avatar :src="resolvedAvatar" :size="36">
            <IconFont name="icon-morentouxiang" size="18px" />
          </el-avatar>
          <span>{{ displayNickName }}</span>
        </button>

        <el-button class="action-btn" type="default" @click="goCreatorCenter">
          <IconFont name="icon-xiangmu" />
          创作中心
        </el-button>

        <el-button class="action-btn create-btn" type="primary" @click="goCreatePage">
          <IconFont name="icon-xinjian" />
          创建
        </el-button>
      </div>
    </header>

    <section class="panel play-panel" v-loading="detailLoading">
      <el-alert
        v-if="detailError"
        type="error"
        :closable="false"
        show-icon
        :title="detailError"
      />

      <div v-else class="play-layout">
        <div class="play-main">
          <div
            ref="playerShellRef"
            class="player-shell"
          >
            <div
              v-show="Boolean(activeFileId)"
              ref="artPlayerRef"
              class="art-player-host"
            />
            <div v-if="!activeFileId" class="player-empty">
              当前分P暂无可播放视频
            </div>
          </div>
          <section v-if="activeFileId" class="danmu-plugin-bar">
            <div ref="danmuMountRef" class="danmu-plugin-mount" />
            <span v-if="!isDanmuClosed" class="danmu-plugin-tip">
              {{ danmuLoading ? '弹幕加载中...' : '可在弹幕面板中发送弹幕、选择颜色并调整显示区域' }}
            </span>
            <el-alert
              v-else
              type="warning"
              show-icon
              :closable="false"
              title="弹幕已关闭"
            />
          </section>

          <h1 class="video-title">{{ displayTitle }}</h1>
          <div v-if="displayTagList.length > 0" class="video-tag-list">
            <span
              v-for="(tag, index) in displayTagList"
              :key="`${tag}-${index}`"
              class="video-tag-item"
            >
              #{{ tag }}
            </span>
          </div>

          <div class="video-owner-row">
            <div class="owner-info">
              <button
                type="button"
                class="owner-avatar-entry"
                @click="goPublisherCenter"
              >
                <el-avatar :src="authorAvatar" :size="40">
                  <IconFont name="icon-morentouxiang" size="18px" />
                </el-avatar>
              </button>
              <div class="owner-copy">
                <div class="owner-name-row">
                  <button
                    type="button"
                    class="owner-name-entry"
                    @click="goPublisherCenter"
                  >
                    {{ displayAuthorName }}
                  </button>
                  <button
                    v-if="showAuthorFocusEntry && !hasFocusedAuthor && !authorProfileLoading"
                    type="button"
                    class="author-focus-btn"
                    :disabled="focusSubmitting"
                    @click="handleFocusAuthor"
                  >
                    {{ focusSubmitting ? '关注中...' : '关注' }}
                  </button>
                  <span
                    v-else-if="showAuthorFocusEntry && hasFocusedAuthor && !authorProfileLoading"
                    class="author-focused-tag"
                  >
                    已关注
                  </span>
                </div>
                <p class="owner-time">更新时间：{{ displayUpdateTime }}</p>
              </div>
            </div>

            <div class="owner-actions">
              <span
                v-for="item in videoActionButtons"
                :key="item.key"
                class="video-action-item"
                :class="{ active: item.active, loading: item.loading }"
                :aria-label="`${item.label}${item.count}`"
                role="button"
                :tabindex="item.loading ? -1 : 0"
                @click="handleVideoAction(item.actionType)"
                @keydown.enter="handleVideoAction(item.actionType)"
                @keydown.space.prevent="handleVideoAction(item.actionType)"
              >
                <IconFont :name="item.icon" size="20px" />
                <span>{{ item.count }}</span>
              </span>
            </div>
          </div>

          <VideoCommentSection
            :video-id="currentVideoId"
            :video-user-id="publisherUserId"
            :current-user-id="currentUserId"
          />
        </div>

        <aside class="p-list-panel">
          <section class="p-section">
            <div class="p-head">
              <h2>分P列表</h2>
              <p>{{ pList.length }} 个分P</p>
            </div>

            <div v-if="pList.length > 0" class="p-list">
              <button
                v-for="(item, index) in pList"
                :key="item.fileId || index"
                type="button"
                class="p-item"
                :class="{ active: activeFileId === item.fileId }"
                @click="handleSelectP(item)"
              >
                <span class="p-index">P{{ Number(item.fileIndex || index + 1) }}</span>
                <span class="p-name">{{ getPDisplayName(item, index) }}</span>
                <span v-if="activeFileId === item.fileId" class="p-playing">播放中</span>
              </button>
            </div>
            <el-empty v-else description="暂无分P列表" :image-size="90" />
          </section>

          <section class="related-video-section">
            <div class="p-head">
              <h2>视频列表</h2>
              <p>{{ relatedVideoList.length }} 条</p>
            </div>

            <el-alert
              v-if="relatedVideoError && relatedVideoList.length === 0"
              type="error"
              :closable="false"
              show-icon
              :title="relatedVideoError"
            />

            <el-empty
              v-else-if="!relatedVideoLoading && relatedVideoList.length === 0"
              description="暂无视频列表"
              :image-size="90"
            />

            <div v-else class="related-video-list">
              <button
                v-for="(item, index) in relatedVideoList"
                :key="item.videoId || index"
                type="button"
                class="related-video-item"
                :class="{ active: item.videoId === currentVideoId }"
                @click="goVideoPlay(item.videoId)"
              >
                <div class="related-video-cover-wrap">
                  <img
                    v-if="item.videoCover"
                    class="related-video-cover"
                    :src="toResourceUrl(item.videoCover)"
                    alt="视频封面"
                    loading="lazy"
                  />
                  <div v-else class="related-video-cover related-video-cover-empty">暂无封面</div>
                  <span class="related-video-duration">{{ formatDuration(item.duration) }}</span>
                </div>

                <div class="related-video-content">
                  <p class="related-video-title" :title="item.videoName || '未命名视频'">
                    {{ item.videoName || '未命名视频' }}
                  </p>
                  <p class="related-video-meta">{{ item.nickName || item.userId || '未知用户' }}</p>
                  <p class="related-video-meta">发布时间：{{ formatDate(item.createTime || item.lastUpdateTime) }}</p>
                </div>
              </button>
            </div>

            <div v-if="relatedVideoLoading" class="related-video-loading" aria-live="polite">
              <span class="related-video-spinner" aria-hidden="true" />
              <span>加载中...</span>
            </div>
            <button
              v-if="relatedVideoError && relatedVideoList.length > 0 && !relatedVideoLoading"
              type="button"
              class="related-video-retry"
              @click="loadRelatedVideoList"
            >
              加载失败，点击重试
            </button>
            <p v-if="!relatedVideoHasMore && relatedVideoList.length > 0" class="related-video-end">
              已经到底了
            </p>
            <div ref="relatedVideoSentinelRef" class="related-video-sentinel" aria-hidden="true" />
          </section>
        </aside>
      </div>
    </section>

    <AuthDialog />
  </div>
</template>

<style scoped>
.video-play-page {
  min-height: 100vh;
  padding: 20px clamp(14px, 4vw, 44px) 28px;
  background:
    radial-gradient(860px 360px at 10% -12%, rgba(101, 122, 255, 0.16), transparent 72%),
    radial-gradient(820px 320px at 94% 10%, rgba(31, 201, 162, 0.12), transparent 70%),
    linear-gradient(180deg, #f6f9ff 0%, #edf4ff 100%);
}

.panel {
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(132, 155, 214, 0.25);
  box-shadow: 0 10px 28px rgba(47, 72, 137, 0.08);
}

.play-header {
  padding: 14px 18px;
  display: grid;
  align-items: center;
  gap: 14px;
  grid-template-columns: auto minmax(260px, 1fr) auto;
  position: relative;
  z-index: 40;
  overflow: visible;
}

.brand-area {
  display: flex;
  align-items: center;
  gap: 10px;
}

.brand-logo {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  color: #ffffff;
  background: linear-gradient(135deg, #5d76ff, #1fc9a2);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.brand-copy {
  display: flex;
  align-items: center;
}

.brand-name {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: #1f2a44;
}

.brand-home-entry {
  cursor: pointer;
  transition: color 0.2s ease;
}

.brand-home-entry:hover {
  color: #4b6eff;
}

.brand-home-entry:focus-visible {
  outline: 2px solid #5d76ff;
  outline-offset: 2px;
}

.search-area {
  position: relative;
  width: clamp(240px, 30vw, 360px);
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  justify-self: center;
}

.search-input {
  flex: 1;
}

.search-area :deep(.el-input__wrapper) {
  border-radius: 999px;
  background: #f7f9ff;
  box-shadow: inset 0 0 0 1px #d7e0f8;
}

.search-submit-btn {
  border: 1px solid #bfd0ff;
  border-radius: 999px;
  background: #f5f8ff;
  color: #365caa;
  min-height: 38px;
  padding: 0 14px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: border-color 0.2s ease, background-color 0.2s ease;
}

.search-submit-btn:hover {
  border-color: #86a5ef;
  background: #eaf1ff;
}

.search-submit-btn:focus-visible {
  outline: 2px solid #92aff0;
  outline-offset: 2px;
}

.search-suggest-panel {
  position: absolute;
  left: 0;
  right: 0;
  top: calc(100% + 10px);
  border: 1px solid #d9e4ff;
  border-radius: 10px;
  background: #ffffff;
  box-shadow: 0 12px 26px rgba(34, 60, 117, 0.14);
  padding: 8px;
  z-index: 60;
}

.suggest-title {
  margin: 0;
  color: #5f7399;
  font-size: 11px;
}

.suggest-list {
  margin-top: 6px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.suggest-item {
  border: 1px solid #d4e0fd;
  border-radius: 999px;
  background: #f5f8ff;
  color: #2c4f95;
  min-height: 26px;
  padding: 0 9px;
  font-size: 11px;
  cursor: pointer;
}

.suggest-item:hover {
  border-color: #86a4ed;
  background: #eaf2ff;
}

.action-area {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar-trigger {
  padding: 6px 8px 6px 6px;
  border: 1px solid #dce4f7;
  border-radius: 999px;
  background: #ffffff;
  color: #2b3e61;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: border-color 0.2s ease, transform 0.2s ease;
}

.avatar-trigger:hover {
  border-color: #92a8f8;
  transform: translateY(-1px);
}

.user-menu {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.user-menu-item {
  width: 100%;
  border: 1px solid #dce4f7;
  background: #ffffff;
  color: #2b3e61;
  border-radius: 8px;
  min-height: 34px;
  cursor: pointer;
  transition: border-color 0.2s ease, background-color 0.2s ease;
}

.user-menu-item:hover {
  border-color: #8aa0f7;
  background: #f4f7ff;
}

.user-menu-item.danger {
  color: #d64458;
  border-color: #f2c5cd;
}

.user-menu-item.danger:hover {
  background: #fff5f6;
  border-color: #ea909e;
}

.action-btn :deep(.iconfont-svg) {
  margin-right: 6px;
}

.create-btn:not(.is-disabled) {
  background: linear-gradient(135deg, #5d76ff, #3f92ff);
  border: none;
}

.play-panel {
  margin-top: 16px;
  padding: 16px;
  position: relative;
  z-index: 1;
}

.play-layout {
  display: grid;
  gap: 14px;
  grid-template-columns: minmax(0, 1fr) 310px;
}

.play-main {
  min-width: 0;
}

.player-shell {
  border-radius: 14px;
  border: 1px solid #d7e3fd;
  background: #0f172a;
  overflow: hidden;
  position: relative;
  isolation: isolate;
}

.art-player-host {
  width: 100%;
  aspect-ratio: 16 / 9;
  display: block;
}

.art-player-host :deep(.art-video-player) {
  width: 100%;
  height: 100%;
}

.art-player-host :deep(.art-controls) {
  z-index: 30;
}

.player-empty {
  aspect-ratio: 16 / 9;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #d4dbef;
  font-size: 14px;
}

.danmu-plugin-bar {
  margin-top: 12px;
  border: 1px solid #d6e2ff;
  border-radius: 12px;
  background: linear-gradient(180deg, #f9fbff, #f2f6ff);
  padding: 10px;
  display: grid;
  gap: 8px;
}

.danmu-plugin-mount {
  min-height: 36px;
}

.danmu-plugin-tip {
  color: #6d7ea0;
  font-size: 12px;
}


.video-title {
  margin: 14px 0 0;
  color: #1f2f50;
  font-size: 22px;
  line-height: 1.4;
}

.video-tag-list {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.video-tag-item {
  border: 1px solid #cfdcff;
  border-radius: 999px;
  padding: 4px 10px;
  background: #f1f6ff;
  color: #375fb6;
  font-size: 12px;
  line-height: 1;
}

.video-owner-row {
  margin-top: 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.owner-info {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.owner-avatar-entry {
  border: none;
  background: transparent;
  padding: 0;
  cursor: pointer;
}

.owner-avatar-entry:focus-visible {
  outline: 2px solid #5d76ff;
  outline-offset: 3px;
  border-radius: 999px;
}

.owner-copy {
  min-width: 0;
}

.owner-name-row {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.owner-name-entry {
  border: none;
  background: transparent;
  padding: 0;
  color: #2d4066;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
}

.owner-name-entry:hover {
  color: #3f76ff;
}

.owner-name-entry:focus-visible {
  outline: 2px solid #5d76ff;
  outline-offset: 3px;
  border-radius: 6px;
}

.author-focus-btn {
  border: 1px solid #8fa3ff;
  border-radius: 999px;
  background: #f4f7ff;
  color: #3f76ff;
  min-height: 24px;
  padding: 0 9px;
  font-size: 12px;
  cursor: pointer;
  transition: border-color 0.2s ease, background-color 0.2s ease;
}

.author-focus-btn:hover {
  border-color: #6f8dff;
  background: #eaf0ff;
}

.author-focus-btn:disabled {
  cursor: wait;
  opacity: 0.7;
}

.author-focused-tag {
  color: #6c7f9f;
  font-size: 12px;
  line-height: 1;
}

.owner-time {
  margin: 4px 0 0;
  color: #7385aa;
  font-size: 12px;
}

.owner-actions {
  display: inline-flex;
  align-items: center;
  gap: 22px;
}

.video-action-item {
  color: #8f9cb4;
  min-height: 32px;
  padding: 2px 4px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: color 0.2s ease;
}

.video-action-item:hover {
  color: #6e8fdf;
}

.video-action-item.active {
  color: #3f76ff;
}

.video-action-item.loading {
  cursor: default;
  opacity: 0.75;
  pointer-events: none;
}

.video-action-item:focus-visible {
  outline: 2px solid #5d76ff;
  outline-offset: 4px;
  border-radius: 4px;
}

.video-action-item span {
  font-size: 16px;
  font-weight: 700;
}

.p-list-panel {
  border: 1px solid #dce6ff;
  border-radius: 14px;
  background: linear-gradient(180deg, #fbfdff, #f4f8ff);
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.p-section,
.related-video-section {
  min-width: 0;
}

.p-head {
  margin-bottom: 10px;
}

.p-head h2 {
  margin: 0;
  color: #243456;
  font-size: 16px;
}

.p-head p {
  margin: 4px 0 0;
  color: #7385a9;
  font-size: 12px;
}

.p-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.p-item {
  border: 1px solid #d7e2fe;
  border-radius: 10px;
  background: #ffffff;
  padding: 8px 10px;
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.p-item:hover {
  border-color: #8da5ff;
  box-shadow: 0 6px 14px rgba(67, 92, 162, 0.12);
}

.p-item.active {
  border-color: #5d76ff;
  background: #eff3ff;
}

.p-index {
  color: #4f6290;
  font-size: 12px;
  font-weight: 700;
}

.p-name {
  color: #2c3f66;
  font-size: 13px;
  text-align: left;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.p-playing {
  color: #4564ff;
  font-size: 12px;
}

.related-video-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.related-video-item {
  border: 1px solid #d7e2fe;
  border-radius: 10px;
  background: #ffffff;
  padding: 8px;
  display: grid;
  grid-template-columns: 124px minmax(0, 1fr);
  gap: 10px;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.related-video-item:hover {
  border-color: #8da5ff;
  box-shadow: 0 6px 14px rgba(67, 92, 162, 0.12);
}

.related-video-item.active {
  border-color: #5d76ff;
  background: #eff3ff;
}

.related-video-cover-wrap {
  position: relative;
}

.related-video-cover {
  width: 100%;
  aspect-ratio: 16 / 9;
  border-radius: 8px;
  object-fit: cover;
  background: #e6edff;
}

.related-video-cover-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #7386aa;
  font-size: 12px;
}

.related-video-duration {
  position: absolute;
  right: 6px;
  bottom: 6px;
  border-radius: 999px;
  background: rgba(16, 24, 40, 0.72);
  padding: 2px 8px;
  color: #f5f7ff;
  font-size: 11px;
  line-height: 1;
}

.related-video-content {
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 6px;
}

.related-video-title {
  margin: 0;
  color: #243456;
  font-size: 13px;
  font-weight: 700;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.related-video-meta {
  margin: 0;
  color: #66789c;
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.related-video-loading {
  margin-top: 10px;
  color: #6378a8;
  font-size: 12px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.related-video-spinner {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 2px solid #c6d6ff;
  border-top-color: #4b72ff;
  animation: related-video-spin 0.8s linear infinite;
}

.related-video-retry {
  margin-top: 10px;
  border: 1px solid #bfd0ff;
  border-radius: 8px;
  background: #f7f9ff;
  color: #4367d9;
  min-height: 30px;
  padding: 0 10px;
  font-size: 12px;
  cursor: pointer;
  transition: border-color 0.2s ease, background-color 0.2s ease;
}

.related-video-retry:hover {
  border-color: #95afff;
  background: #edf2ff;
}

.related-video-end {
  margin: 10px 0 0;
  color: #7a8cae;
  font-size: 12px;
}

.related-video-sentinel {
  height: 1px;
}

@keyframes related-video-spin {
  from {
    transform: rotate(0deg);
  }

  to {
    transform: rotate(360deg);
  }
}

@media (prefers-reduced-motion: reduce) {
  .related-video-spinner {
    animation-duration: 1.6s;
  }
}

@media (max-width: 1080px) {
  .play-header {
    grid-template-columns: 1fr;
  }

  .search-area {
    width: min(100%, 360px);
  }

  .action-area {
    justify-content: flex-start;
    flex-wrap: wrap;
  }

  .play-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 700px) {
  .video-play-page {
    padding: 14px;
  }

  .play-header {
    padding: 14px;
  }

  .search-area {
    width: 100%;
    flex-wrap: wrap;
  }

  .search-submit-btn {
    width: 100%;
  }

  .avatar-trigger span {
    display: none;
  }

  .action-area :deep(.el-button) {
    padding-left: 12px;
    padding-right: 12px;
  }

  .video-title {
    font-size: 18px;
  }

  .related-video-item {
    grid-template-columns: 100px minmax(0, 1fr);
  }
}
</style>

