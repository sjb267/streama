<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { loadAllCategory } from '@/api/category'
import { getSearchKeywordTop, loadVideoList, searchVideo } from '@/api/video'
import AuthDialog from '@/components/AuthDialog.vue'
import IconFont from '@/components/IconFont.vue'
import UserCoinBadge from '@/components/UserCoinBadge.vue'
import { useAuthStore } from '@/stores/auth'

const SEARCH_ORDER_TABS = Object.freeze([
  { label: '播放量', value: 0 },
  { label: '发布时间', value: 1 },
  { label: '弹幕量', value: 2 },
  { label: '收藏量', value: 3 },
])

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()

const searchKeyword = ref('')

const categories = ref([])
const categoryLoading = ref(false)
const categoryError = ref('')
const selectedParentCategoryId = ref('all')
const selectedChildCategoryId = ref('all')
const hoveredParentCategoryId = ref('')

const videoList = ref([])
const videoLoading = ref(false)
const videoError = ref('')
const videoPagination = ref({
  pageNo: 1,
  pageSize: 15,
  totalCount: 0,
})
let videoRequestToken = 0

const searchOrderType = ref(0)
const searchResultList = ref([])
const searchLoading = ref(false)
const searchError = ref('')
const searchPagination = ref({
  pageNo: 1,
  pageSize: 30,
  totalCount: 0,
})
let searchRequestToken = 0

const hotKeywordList = ref([])
const hotKeywordLoading = ref(false)
const searchInputFocused = ref(false)
const searchAreaHovered = ref(false)
let hotKeywordRequestToken = 0
let hotKeywordTimer = null
let hideHotKeywordTimer = null

const parentCategories = computed(() => {
  return categories.value.filter((item) => getParentId(item) === 0).slice(0, 20)
})
const isLoggedIn = computed(() => authStore.isLoggedIn)
const displayNickName = computed(() => authStore.userInfo?.nickName || '游客')
const resolvedAvatar = computed(() => toResourceUrl(authStore.userInfo?.avatar || ''))
const normalizedSearchKeyword = computed(() => normalizeKeyword(searchKeyword.value))
const isSearchMode = computed(() => Boolean(normalizedSearchKeyword.value))
const displayedVideoList = computed(() => (isSearchMode.value ? searchResultList.value : videoList.value))
const displayedVideoLoading = computed(() => (isSearchMode.value ? searchLoading.value : videoLoading.value))
const displayedVideoError = computed(() => (isSearchMode.value ? searchError.value : videoError.value))
const displayedEmptyText = computed(() => (isSearchMode.value ? '未找到相关视频' : '暂无视频数据'))
const showHotKeywordPanel = computed(() => {
  return (
    (searchInputFocused.value || searchAreaHovered.value) &&
    (hotKeywordLoading.value || hotKeywordList.value.length > 0)
  )
})

function readQueryValue(value) {
  return Array.isArray(value) ? value[0] : value
}

function normalizeKeyword(value) {
  return String(value || '').trim()
}

function parseRouteOrderType(value) {
  const numericValue = Number(readQueryValue(value))
  return [0, 1, 2, 3].includes(numericValue) ? numericValue : 0
}

function parseRoutePageNo(value) {
  const numericValue = Number(readQueryValue(value))
  return Number.isInteger(numericValue) && numericValue > 0 ? numericValue : 1
}

function parseRouteSearchState(query = route.query) {
  const keyword = normalizeKeyword(readQueryValue(query?.keyword))
  if (!keyword) {
    return {
      keyword: '',
      orderType: 0,
      pageNo: 1,
    }
  }
  return {
    keyword,
    orderType: parseRouteOrderType(query?.orderType),
    pageNo: parseRoutePageNo(query?.pageNo),
  }
}

function buildRouteQueryFromSearchState(baseQuery = route.query) {
  const nextQuery = { ...baseQuery }
  delete nextQuery.keyword
  delete nextQuery.orderType
  delete nextQuery.pageNo

  const keyword = normalizedSearchKeyword.value
  if (!keyword) {
    return nextQuery
  }

  nextQuery.keyword = keyword
  nextQuery.orderType = String(searchOrderType.value)
  nextQuery.pageNo = String(searchPagination.value.pageNo)
  return nextQuery
}

function isSameSearchQueryState(queryA, queryB) {
  const stateA = parseRouteSearchState(queryA)
  const stateB = parseRouteSearchState(queryB)
  return stateA.keyword === stateB.keyword && stateA.orderType === stateB.orderType && stateA.pageNo === stateB.pageNo
}

function updateRouteBySearchState({ push = false } = {}) {
  const nextQuery = buildRouteQueryFromSearchState(route.query)
  if (isSameSearchQueryState(nextQuery, route.query)) {
    if (isSearchMode.value) {
      loadSearchVideos()
    } else {
      loadVideos()
    }
    return
  }
  const navigate = push ? router.push : router.replace
  navigate({
    path: '/',
    query: nextQuery,
  }).catch(() => {})
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

function stripUnsafeHtml(value) {
  return String(value || '').replace(/<[^>]+>/g, '').replace(/\s+/g, ' ').trim()
}

function escapeHtml(value) {
  return String(value || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function escapeRegExp(value) {
  return String(value || '').replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

function highlightKeywordText(text, keyword) {
  const sourceText = String(text || '')
  const normalizedKeyword = normalizeKeyword(keyword)
  if (!normalizedKeyword) {
    return escapeHtml(sourceText)
  }

  const matcher = new RegExp(escapeRegExp(normalizedKeyword), 'ig')
  let result = ''
  let lastIndex = 0
  let matched = false
  let match = matcher.exec(sourceText)
  while (match) {
    matched = true
    result += escapeHtml(sourceText.slice(lastIndex, match.index))
    result += `<mark class="keyword-mark">${escapeHtml(match[0])}</mark>`
    lastIndex = match.index + match[0].length
    match = matcher.exec(sourceText)
  }

  if (!matched) {
    return escapeHtml(sourceText)
  }

  result += escapeHtml(sourceText.slice(lastIndex))
  return result
}

function getVideoDisplayTitle(item) {
  const rawTitle = item?.videoName ?? item?.title ?? ''
  const sanitizedTitle = stripUnsafeHtml(rawTitle)
  return sanitizedTitle || '未命名视频'
}

function getVideoTitleHtml(item) {
  return highlightKeywordText(getVideoDisplayTitle(item), isSearchMode.value ? normalizedSearchKeyword.value : '')
}

function normalizeVideoItem(item) {
  const data = item && typeof item === 'object' ? item : {}
  return {
    videoId: String(data.videoId || '').trim(),
    videoName: String(data.videoName || data.title || '').trim(),
    videoCover: String(data.videoCover || '').trim(),
    userId: String(data.userId || '').trim(),
    avatar: String(data.avatar || '').trim(),
    nickName: String(data.nickName || data.nickname || '').trim(),
    playCount: Number(data.playCount || 0),
    danmuCount: Number(data.danmuCount || 0),
    collectCount: Number(data.collectCount || 0),
    lastUpdateTime: String(data.lastUpdateTime || data.createTime || '').trim(),
  }
}

function normalizeVideoList(list) {
  if (!Array.isArray(list)) {
    return []
  }
  return list.map((item) => normalizeVideoItem(item)).filter((item) => item.videoId)
}

function normalizeKeywordSuggestionList(list) {
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
      keywordText = String(item.keyword ?? item.searchKeyword ?? item.keywordName ?? item.name ?? item.text ?? '')
    }

    const normalized = normalizeKeyword(keywordText)
    if (!normalized || keywordSet.has(normalized)) {
      return
    }
    keywordSet.add(normalized)
    keywordResult.push(normalized)
  })

  return keywordResult.slice(0, 10)
}

function getCategoryChildren(parentCategoryId) {
  const parent = categories.value.find((item) => String(item?.categoryId) === String(parentCategoryId))
  const fromChildren = parent?.children
  if (Array.isArray(fromChildren) && fromChildren.length > 0) {
    return fromChildren
  }

  const parentId = Number(parentCategoryId)
  if (!Number.isFinite(parentId) || parentId <= 0) {
    return []
  }
  return categories.value.filter((item) => getParentId(item) === parentId)
}

function isParentCategoryActive(categoryId) {
  return selectedParentCategoryId.value === String(categoryId)
}

function isChildCategoryActive(parentCategoryId, childCategoryId) {
  return selectedParentCategoryId.value === String(parentCategoryId) && selectedChildCategoryId.value === String(childCategoryId)
}

function setHoveredParentCategory(categoryId) {
  hoveredParentCategoryId.value = String(categoryId || '')
}

function clearHoveredParentCategory(categoryId) {
  const targetId = String(categoryId || '')
  if (hoveredParentCategoryId.value === targetId) {
    hoveredParentCategoryId.value = ''
  }
}

function isChildPopoverVisible(categoryId) {
  const targetId = String(categoryId || '')
  if (!targetId) {
    return false
  }
  return hoveredParentCategoryId.value === targetId || (hoveredParentCategoryId.value === '' && selectedParentCategoryId.value === targetId)
}

function openLoginDialog() {
  authStore.openAuthDialog('login')
}

function handleAvatarClick() {
  if (!isLoggedIn.value) {
    openLoginDialog()
  }
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

function goUserCenter() {
  router.push('/user-center')
}

function goUserProfile(userId) {
  const targetUserId = String(userId || '').trim()
  if (!targetUserId) {
    return
  }
  router.push({
    path: '/user-center',
    query: {
      userId: targetUserId,
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

async function handleSignOut() {
  await authStore.signOut()
  ElMessage.success('已退出登录')
  router.push('/')
}

function selectParentCategory(categoryId) {
  if (isSearchMode.value) {
    return
  }
  selectedParentCategoryId.value = String(categoryId || 'all')
  selectedChildCategoryId.value = 'all'
  videoPagination.value.pageNo = 1
  loadVideos()
}

function selectChildCategory(parentCategoryId, childCategoryId) {
  if (isSearchMode.value) {
    return
  }
  selectedParentCategoryId.value = String(parentCategoryId || 'all')
  selectedChildCategoryId.value = String(childCategoryId || 'all')
  videoPagination.value.pageNo = 1
  loadVideos()
}

function resetSearchState() {
  searchRequestToken += 1
  searchLoading.value = false
  searchError.value = ''
  searchResultList.value = []
  searchPagination.value.pageNo = 1
  searchPagination.value.pageSize = 30
  searchPagination.value.totalCount = 0
}

function clearHotKeywordTimer() {
  if (!hotKeywordTimer) {
    return
  }
  clearTimeout(hotKeywordTimer)
  hotKeywordTimer = null
}

function clearHideHotKeywordTimer() {
  if (!hideHotKeywordTimer) {
    return
  }
  clearTimeout(hideHotKeywordTimer)
  hideHotKeywordTimer = null
}

function clearHotKeywords() {
  hotKeywordRequestToken += 1
  hotKeywordLoading.value = false
  hotKeywordList.value = []
}

async function loadCategories() {
  categoryLoading.value = true
  categoryError.value = ''

  try {
    const data = await loadAllCategory()
    categories.value = Array.isArray(data) ? data : []
  } catch (_error) {
    categoryError.value = '分类加载失败，请稍后重试'
    categories.value = []
  } finally {
    categoryLoading.value = false
  }
}

async function loadVideos() {
  if (isSearchMode.value) {
    return
  }

  const requestToken = ++videoRequestToken
  videoLoading.value = true
  videoError.value = ''

  const params = {
    pageNo: videoPagination.value.pageNo,
    pageSize: videoPagination.value.pageSize,
  }

  if (selectedParentCategoryId.value !== 'all') {
    params.pCategoryId = Number(selectedParentCategoryId.value)
  }
  if (selectedChildCategoryId.value !== 'all') {
    params.categoryId = Number(selectedChildCategoryId.value)
  }

  try {
    const data = await loadVideoList(params)
    if (requestToken !== videoRequestToken) {
      return
    }
    videoList.value = normalizeVideoList(data?.list)
    videoPagination.value.totalCount = Math.max(0, Number(data?.totalCount || 0))
    videoPagination.value.pageNo = Math.max(1, Number(data?.pageNo || videoPagination.value.pageNo || 1))
    videoPagination.value.pageSize = Math.max(1, Number(data?.pageSize || videoPagination.value.pageSize || 15))
  } catch (_error) {
    if (requestToken !== videoRequestToken) {
      return
    }
    videoError.value = '视频加载失败，请稍后重试'
    videoList.value = []
    videoPagination.value.totalCount = 0
  } finally {
    if (requestToken === videoRequestToken) {
      videoLoading.value = false
    }
  }
}

async function loadSearchVideos() {
  const keyword = normalizedSearchKeyword.value
  if (!keyword) {
    resetSearchState()
    return
  }

  const requestToken = ++searchRequestToken
  searchLoading.value = true
  searchError.value = ''

  try {
    const data = await searchVideo({
      keyword,
      orderType: searchOrderType.value,
      pageNo: searchPagination.value.pageNo,
    })
    if (requestToken !== searchRequestToken) {
      return
    }

    const list = normalizeVideoList(data?.list)
    searchResultList.value = list
    searchPagination.value.totalCount = Math.max(0, Number(data?.totalCount || 0))
    searchPagination.value.pageNo = Math.max(1, Number(data?.pageNo || searchPagination.value.pageNo || 1))
    searchPagination.value.pageSize = Math.max(1, Number(data?.pageSize || searchPagination.value.pageSize || 30))

  } catch (_error) {
    if (requestToken !== searchRequestToken) {
      return
    }
    searchError.value = '搜索失败，请稍后重试'
    searchResultList.value = []
    searchPagination.value.totalCount = 0
  } finally {
    if (requestToken === searchRequestToken) {
      searchLoading.value = false
    }
  }
}

async function loadHotKeywords(keyword) {
  const targetKeyword = normalizeKeyword(keyword)

  const requestToken = ++hotKeywordRequestToken
  hotKeywordLoading.value = true

  try {
    const data = await getSearchKeywordTop({ keyword: targetKeyword })
    if (requestToken !== hotKeywordRequestToken) {
      return
    }
    const rawList = Array.isArray(data) ? data : Array.isArray(data?.list) ? data.list : []
    hotKeywordList.value = normalizeKeywordSuggestionList(rawList)
  } catch (_error) {
    if (requestToken !== hotKeywordRequestToken) {
      return
    }
    hotKeywordList.value = []
  } finally {
    if (requestToken === hotKeywordRequestToken) {
      hotKeywordLoading.value = false
    }
  }
}

function queueLoadHotKeywords(keyword) {
  const targetKeyword = normalizeKeyword(keyword)
  clearHotKeywordTimer()
  hotKeywordTimer = setTimeout(() => {
    loadHotKeywords(targetKeyword)
  }, 260)
}

function handleVideoPageNoChange(pageNo) {
  if (isSearchMode.value) {
    return
  }
  videoPagination.value.pageNo = pageNo
  loadVideos()
}

function handleVideoPageSizeChange(pageSize) {
  if (isSearchMode.value) {
    return
  }
  videoPagination.value.pageSize = pageSize
  videoPagination.value.pageNo = 1
  loadVideos()
}

function handleSearchPageNoChange(pageNo) {
  if (!isSearchMode.value) {
    return
  }
  searchPagination.value.pageNo = Math.max(1, Number(pageNo || 1))
  updateRouteBySearchState()
}

function handleSearchOrderChange(orderType) {
  const normalizedOrderType = Number(orderType)
  if (![0, 1, 2, 3].includes(normalizedOrderType)) {
    return
  }
  if (searchOrderType.value === normalizedOrderType && searchPagination.value.pageNo === 1) {
    return
  }
  searchOrderType.value = normalizedOrderType
  searchPagination.value.pageNo = 1
  updateRouteBySearchState()
}

function handleSearchSubmit() {
  searchKeyword.value = normalizeKeyword(searchKeyword.value)
  searchPagination.value.pageNo = 1
  if (!searchKeyword.value) {
    searchOrderType.value = 0
  }
  updateRouteBySearchState({ push: true })
}

function handleSearchClear() {
  searchKeyword.value = ''
  searchOrderType.value = 0
  searchPagination.value.pageNo = 1
  updateRouteBySearchState()
}

function handleSearchFocus() {
  searchInputFocused.value = true
  clearHideHotKeywordTimer()
  if (normalizedSearchKeyword.value && hotKeywordList.value.length === 0 && !hotKeywordLoading.value) {
    queueLoadHotKeywords(searchKeyword.value)
  }
}

function handleSearchBlur() {
  clearHideHotKeywordTimer()
  hideHotKeywordTimer = setTimeout(() => {
    searchInputFocused.value = false
  }, 120)
}

function handleSearchAreaMouseEnter() {
  searchAreaHovered.value = true
  clearHideHotKeywordTimer()
  if (hotKeywordList.value.length === 0 && !hotKeywordLoading.value) {
    queueLoadHotKeywords(searchKeyword.value)
  }
}

function handleSearchAreaMouseLeave() {
  searchAreaHovered.value = false
}

function handlePickHotKeyword(keyword) {
  const normalizedKeyword = normalizeKeyword(keyword)
  if (!normalizedKeyword) {
    return
  }
  searchKeyword.value = normalizedKeyword
  searchPagination.value.pageNo = 1
  searchInputFocused.value = false
  searchAreaHovered.value = false
  updateRouteBySearchState({ push: true })
}

watch(
  () => route.query,
  (nextQuery) => {
    const routeSearchState = parseRouteSearchState(nextQuery)
    searchKeyword.value = routeSearchState.keyword
    searchOrderType.value = routeSearchState.orderType
    searchPagination.value.pageNo = routeSearchState.pageNo

    if (routeSearchState.keyword) {
      loadSearchVideos()
      return
    }

    resetSearchState()
    loadVideos()
  },
  { immediate: true },
)

watch(
  () => searchKeyword.value,
  (value) => {
    queueLoadHotKeywords(value)
  },
)

onMounted(() => {
  authStore.initAutoLogin()
  loadCategories()
})

onBeforeUnmount(() => {
  clearHotKeywordTimer()
  clearHideHotKeywordTimer()
  clearHotKeywords()
  searchRequestToken += 1
  videoRequestToken += 1
})
</script>
<template>
  <div class="home-page">
    <header class="home-header panel">
      <div class="brand-area">
        <div class="brand-logo">
          <IconFont name="icon-iconfont1" size="24px" />
        </div>
        <div class="brand-copy">
          <p class="brand-name">Streama</p>
          <p class="brand-subtitle">视频分享平台</p>
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
          placeholder="搜索视频、作者、标签"
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

        <div v-if="showHotKeywordPanel" class="search-suggest-panel">
          <p class="suggest-title">{{ hotKeywordLoading ? '热词更新中...' : '热门搜索' }}</p>
          <div class="suggest-list">
            <button
              v-for="(keyword, index) in hotKeywordList"
              :key="`${keyword}-${index}`"
              type="button"
              class="suggest-item"
              @mousedown.prevent
              @click="handlePickHotKeyword(keyword)"
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
            <button class="user-menu-item" type="button" @click="goUserCenter">个人中心</button>
            <button class="user-menu-item danger" type="button" @click="handleSignOut">退出登录</button>
          </div>
        </el-popover>

        <button v-else class="avatar-trigger" type="button" @click="handleAvatarClick">
          <el-avatar :src="resolvedAvatar" :size="36">
            <IconFont name="icon-morentouxiang" size="18px" />
          </el-avatar>
          <span>{{ displayNickName }}</span>
        </button>

        <UserCoinBadge />

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

    <section v-if="!isSearchMode" class="panel category-panel">
      <div class="section-head">
        <h2>分类</h2>
      </div>

      <el-skeleton v-if="categoryLoading" :rows="2" animated />

      <el-alert
        v-else-if="categoryError"
        type="error"
        :closable="false"
        show-icon
        :title="categoryError"
      />

      <el-empty v-else-if="parentCategories.length === 0" description="暂无分类数据" />

      <div v-else class="category-block">
        <div class="category-group">
          <p class="group-title">主分类</p>
          <div class="tag-list parent-list">
            <button
              type="button"
              class="tag-item"
              :class="{ active: selectedParentCategoryId === 'all' }"
              @click="selectParentCategory('all')"
            >
              全部
            </button>

            <div
              v-for="item in parentCategories"
              :key="item.categoryId"
              class="parent-tag-wrap"
              @mouseenter="setHoveredParentCategory(item.categoryId)"
              @mouseleave="clearHoveredParentCategory(item.categoryId)"
            >
              <button
                type="button"
                class="tag-item"
                :class="{ active: isParentCategoryActive(item.categoryId) }"
                @click="selectParentCategory(item.categoryId)"
              >
                {{ item.categoryName }}
              </button>

              <div
                v-if="isChildPopoverVisible(item.categoryId) && getCategoryChildren(item.categoryId).length > 0"
                class="child-popover"
              >
                <button
                  type="button"
                  class="tag-item child-popover-item"
                  :class="{ active: selectedParentCategoryId === String(item.categoryId) && selectedChildCategoryId === 'all' }"
                  @click="selectParentCategory(item.categoryId)"
                >
                  全部
                </button>
                <button
                  v-for="child in getCategoryChildren(item.categoryId)"
                  :key="child.categoryId"
                  type="button"
                  class="tag-item child-popover-item"
                  :class="{ active: isChildCategoryActive(item.categoryId, child.categoryId) }"
                  @click="selectChildCategory(item.categoryId, child.categoryId)"
                >
                  {{ child.categoryName }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="panel video-panel">
      <div class="section-head">
        <div class="section-head-main">
          <h2>{{ isSearchMode ? '搜索结果' : '视频列表' }}</h2>
          <p v-if="isSearchMode">关键词：{{ normalizedSearchKeyword }}</p>
        </div>

        <div v-if="isSearchMode" class="search-order-tabs">
          <button
            v-for="item in SEARCH_ORDER_TABS"
            :key="item.value"
            type="button"
            class="search-order-tab"
            :class="{ active: searchOrderType === item.value }"
            @click="handleSearchOrderChange(item.value)"
          >
            {{ item.label }}
          </button>
        </div>
      </div>

      <el-skeleton v-if="displayedVideoLoading && displayedVideoList.length === 0" :rows="6" animated />

      <el-alert
        v-else-if="displayedVideoError"
        type="error"
        :closable="false"
        show-icon
        :title="displayedVideoError"
      />

      <el-empty v-else-if="displayedVideoList.length === 0" :description="displayedEmptyText" />

      <div v-else class="video-grid">
        <article
          v-for="(row, index) in displayedVideoList"
          :key="row.videoId || `${row.videoName || 'video'}-${index}`"
          class="video-card"
          role="button"
          tabindex="0"
          @click="goVideoPlay(row.videoId)"
          @keydown.enter="goVideoPlay(row.videoId)"
        >
          <div class="video-cover-wrap">
            <img
              v-if="row.videoCover"
              class="video-cover"
              :src="toResourceUrl(row.videoCover)"
              alt="视频封面"
            />
            <div v-else class="video-cover video-cover-empty">暂无封面</div>

            <div class="video-overlay">
              <span class="overlay-item">播放 {{ normalizeCount(row.playCount) }}</span>
              <span class="overlay-item">弹幕 {{ normalizeCount(row.danmuCount) }}</span>
              <span class="overlay-item">收藏 {{ normalizeCount(row.collectCount) }}</span>
            </div>
          </div>

          <h3
            class="video-title"
            :title="getVideoDisplayTitle(row)"
            v-html="getVideoTitleHtml(row)"
          ></h3>

          <div class="video-author">
            <button
              type="button"
              class="author-entry"
              @click.stop="goUserProfile(row.userId)"
            >
              <el-avatar :src="toResourceUrl(row.avatar)" :size="32">
                <IconFont name="icon-morentouxiang" size="16px" />
              </el-avatar>
              <div class="author-meta">
                <p class="author-name">{{ row.nickName || row.userId || '未知用户' }}</p>
                <p class="author-date">{{ formatDate(row.lastUpdateTime) }}</p>
              </div>
            </button>
          </div>
        </article>
      </div>

      <div v-if="isSearchMode" class="video-pager-wrap">
        <el-pagination
          :current-page="searchPagination.pageNo"
          :page-size="searchPagination.pageSize"
          layout="total, prev, pager, next, jumper"
          :total="searchPagination.totalCount"
          @current-change="handleSearchPageNoChange"
        />
      </div>

      <div v-else class="video-pager-wrap">
        <el-pagination
          :current-page="videoPagination.pageNo"
          :page-size="videoPagination.pageSize"
          :page-sizes="[15, 30, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="videoPagination.totalCount"
          @current-change="handleVideoPageNoChange"
          @size-change="handleVideoPageSizeChange"
        />
      </div>
    </section>
  </div>

  <AuthDialog />
</template>
<style scoped>
.home-page {
  min-height: 100vh;
  padding: 28px clamp(16px, 4vw, 56px) 36px;
  background:
    radial-gradient(1000px 420px at 5% -8%, rgba(111, 137, 255, 0.2), transparent 72%),
    radial-gradient(900px 360px at 92% 8%, rgba(26, 188, 156, 0.14), transparent 68%),
    linear-gradient(180deg, #f6f9ff 0%, #eef4ff 100%);
}

.panel {
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(132, 155, 214, 0.25);
  box-shadow: 0 10px 28px rgba(47, 72, 137, 0.08);
  backdrop-filter: blur(4px);
}

.home-header {
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

.brand-name {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: #1f2a44;
}

.brand-subtitle {
  margin: 3px 0 0;
  font-size: 12px;
  color: #7282a5;
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
  padding: 0 16px;
  font-size: 13px;
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

.category-panel,
.video-panel {
  margin-top: 18px;
  padding: 20px;
  position: relative;
  z-index: 1;
}

.section-head {
  margin-bottom: 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.section-head h2 {
  margin: 0;
  font-size: 18px;
  color: #1f2a44;
}

.section-head p {
  margin: 4px 0 0;
  color: #6f7f9f;
  font-size: 13px;
}

.search-order-tabs {
  display: inline-flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.search-order-tab {
  border: 1px solid #d0dcfb;
  border-radius: 999px;
  background: #f5f8ff;
  color: #345895;
  min-height: 30px;
  padding: 0 12px;
  font-size: 12px;
  cursor: pointer;
}

.search-order-tab.active {
  border-color: #4e73ff;
  background: #4e73ff;
  color: #ffffff;
}

.category-group {
  border: 1px solid #e2e8fa;
  background: #fbfcff;
  border-radius: 14px;
  padding: 12px;
  overflow: visible;
}

.group-title {
  margin: 0 0 10px;
  font-size: 13px;
  font-weight: 600;
  color: #516488;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.parent-list {
  position: relative;
  overflow: visible;
}

.parent-tag-wrap {
  position: relative;
}

.tag-item {
  padding: 8px 14px;
  border-radius: 999px;
  border: 1px solid #d7e0f8;
  background: #f7f9ff;
  color: #2c3c5a;
  font-size: 13px;
  cursor: pointer;
}

.tag-item.active {
  border-color: #5d76ff;
  background: #5d76ff;
  color: #ffffff;
}

.child-popover {
  position: absolute;
  left: 0;
  top: calc(100% + 8px);
  z-index: 20;
  min-width: 260px;
  max-width: min(560px, 80vw);
  padding: 10px;
  border-radius: 12px;
  border: 1px solid #d9e4ff;
  background: #ffffff;
  box-shadow: 0 12px 28px rgba(34, 60, 117, 0.14);
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.child-popover-item {
  padding: 6px 12px;
  font-size: 12px;
  background: #f8faff;
}

.video-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 16px;
}

.video-card {
  border: 1px solid #e2e9fc;
  border-radius: 14px;
  background: #fbfdff;
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  cursor: pointer;
}

.video-card:hover {
  border-color: #9db3ff;
  box-shadow: 0 12px 26px rgba(64, 89, 157, 0.12);
}

.video-cover-wrap {
  position: relative;
}

.video-cover {
  width: 100%;
  aspect-ratio: 16 / 9;
  border-radius: 10px;
  border: 1px solid #d8e3ff;
  background: #edf2ff;
  object-fit: cover;
}

.video-cover-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #7386aa;
  font-size: 13px;
}

.video-overlay {
  position: absolute;
  left: 8px;
  right: 8px;
  bottom: 8px;
  border-radius: 8px;
  padding: 6px 8px;
  background: rgba(20, 30, 52, 0.74);
  color: #f8fbff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.overlay-item {
  font-size: 12px;
  line-height: 1;
  white-space: nowrap;
}

.video-title {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #1f2f50;
  line-height: 1.45;
  min-height: 44px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.keyword-mark {
  padding: 0 2px;
  border-radius: 4px;
  background: #ffeaa9;
  color: #7a4f00;
}

.video-author {
  display: flex;
  align-items: center;
  gap: 8px;
}

.author-entry {
  border: none;
  background: transparent;
  padding: 0;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  text-align: left;
  cursor: pointer;
}

.author-entry:hover .author-name {
  color: #3e73ff;
}

.author-meta {
  min-width: 0;
}

.author-name {
  margin: 0;
  font-size: 13px;
  color: #2e4168;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.author-date {
  margin: 3px 0 0;
  font-size: 12px;
  color: #7385a9;
}

.video-pager-wrap {
  margin-top: 18px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 980px) {
  .home-header {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .action-area {
    justify-content: flex-start;
    flex-wrap: wrap;
  }

  .section-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .search-area {
    width: min(100%, 360px);
  }

  .parent-tag-wrap {
    width: 100%;
  }

  .parent-tag-wrap > .tag-item {
    width: 100%;
    display: inline-flex;
    justify-content: flex-start;
  }

  .child-popover {
    position: static;
    margin-top: 8px;
    max-width: 100%;
    box-shadow: none;
    border-style: dashed;
  }
}

@media (max-width: 600px) {
  .home-page {
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
    max-width: 88px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .action-area :deep(.el-button) {
    margin-left: 0;
  }

  .video-grid {
    grid-template-columns: 1fr;
  }
}
</style>

