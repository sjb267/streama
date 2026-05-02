<script setup>
import { reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { loadVideoList } from '@/api/videoInfo'
import {
  formatDateTime,
  formatDuration,
  getVideoStatusMeta,
  normalizeCount,
  normalizeText,
  toAdminFileResourceUrl,
} from '@/utils/videoAudit'

const DEFAULT_STATUS = '2'
const DEFAULT_PAGE_NO = 1
const DEFAULT_PAGE_SIZE = 15

const statusTabs = [
  { key: 'all', label: '全部', value: '' },
  { key: 'transcoding', label: '转码中', value: '0' },
  { key: 'transcode-fail', label: '转码失败', value: '1' },
  { key: 'pending', label: '待审核', value: '2' },
  { key: 'pass', label: '已通过', value: '3' },
  { key: 'reject', label: '已驳回', value: '4' },
]

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const loadError = ref('')
const activeStatus = ref(DEFAULT_STATUS)
const postList = ref([])

const pagination = reactive({
  pageNo: DEFAULT_PAGE_NO,
  pageSize: DEFAULT_PAGE_SIZE,
  totalCount: 0,
})

watch(
  () => route.query,
  () => {
    syncStateFromRoute()
    loadPostList()
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

function syncStateFromRoute() {
  activeStatus.value = normalizeText(route.query.status)
  pagination.pageNo = normalizePositiveInteger(route.query.pageNo, DEFAULT_PAGE_NO)
  pagination.pageSize = normalizePositiveInteger(route.query.pageSize, DEFAULT_PAGE_SIZE)

  if (route.query.status === undefined) {
    activeStatus.value = DEFAULT_STATUS
  }
}

function buildListQuery(overrides = {}) {
  const status = overrides.status ?? activeStatus.value
  const pageNo = normalizePositiveInteger(overrides.pageNo, pagination.pageNo)
  const pageSize = normalizePositiveInteger(overrides.pageSize, pagination.pageSize)

  return {
    status,
    pageNo: String(pageNo),
    pageSize: String(pageSize),
  }
}

function updateRouteQuery(overrides = {}) {
  router.replace({
    name: 'video-manage',
    query: buildListQuery(overrides),
  })
}

async function loadPostList() {
  loading.value = true
  loadError.value = ''

  try {
    const data = await loadVideoList({
      pageNo: pagination.pageNo,
      pageSize: pagination.pageSize,
      status: activeStatus.value === '' ? undefined : Number(activeStatus.value),
    })

    const list = Array.isArray(data?.list) ? data.list : []
    postList.value = list.map((row) => normalizePostRow(row))
    pagination.totalCount = Number(data?.totalCount || 0)
    pagination.pageNo = Number(data?.pageNo || pagination.pageNo)
    pagination.pageSize = Number(data?.pageSize || pagination.pageSize)
  } catch (error) {
    const message = normalizeText(error?.message || error?.info || error?.payload?.info)
    loadError.value = message || '加载稿件失败'
    postList.value = []
  } finally {
    loading.value = false
  }
}

function normalizePostRow(row = {}) {
  return {
    ...row,
    videoId: normalizeText(row.videoId),
    videoName: normalizeText(row.videoName) || '未命名稿件',
    userId: normalizeText(row.userId),
    nickName: normalizeText(row.nickName) || normalizeText(row.userId) || '未知作者',
    videoCoverUrl: toAdminFileResourceUrl(row.videoCover),
    avatarUrl: toAdminFileResourceUrl(row.avatar),
    statusMeta: getVideoStatusMeta(row.status, row.statusName),
    lastUpdateTimeText: formatDateTime(row.lastUpdateTime),
    createTimeText: formatDateTime(row.createTime),
    durationText: formatDuration(row.duration),
    playCount: normalizeCount(row.playCount),
    commentCount: normalizeCount(row.commentCount),
    likeCount: normalizeCount(row.likeCount),
  }
}

function handleStatusChange(nextStatus) {
  if (nextStatus === activeStatus.value) {
    return
  }
  updateRouteQuery({
    status: nextStatus,
    pageNo: DEFAULT_PAGE_NO,
  })
}

function handlePageChange(pageNo) {
  updateRouteQuery({ pageNo })
}

function handlePageSizeChange(pageSize) {
  updateRouteQuery({
    pageNo: DEFAULT_PAGE_NO,
    pageSize,
  })
}

function openDetail(row) {
  if (!row?.videoId) {
    return
  }

  router.push({
    name: 'video-audit-detail',
    params: {
      videoId: row.videoId,
    },
    query: buildListQuery(),
  })
}
</script>

<template>
  <div class="audit-list-page">
    <div class="content-head">
      <div>
        <h2>稿件管理</h2>
        <p>在列表中快速筛选稿件，进入详情页查看分文件内容、AI 审核结果和播放预览。</p>
      </div>
    </div>

    <div class="status-tabs">
      <button
        v-for="tab in statusTabs"
        :key="tab.key"
        type="button"
        class="status-tab"
        :class="{ active: activeStatus === tab.value }"
        @click="handleStatusChange(tab.value)"
      >
        {{ tab.label }}
      </button>
    </div>

    <el-alert
      v-if="loadError"
      type="error"
      show-icon
      :closable="false"
      :title="loadError"
    />

    <div v-loading="loading" class="list-wrap">
      <div v-if="postList.length > 0" class="post-list">
        <article v-for="row in postList" :key="row.videoId" class="post-row">
          <div class="cover-wrap">
            <img
              v-if="row.videoCoverUrl"
              :src="row.videoCoverUrl"
              alt="稿件封面"
              class="post-cover"
            />
            <div v-else class="post-cover empty">暂无封面</div>
          </div>

          <div class="post-main">
            <div class="title-row">
              <div class="title-copy">
                <h3>{{ row.videoName }}</h3>
                <p>作者：{{ row.nickName }} | 用户 ID：{{ row.userId || '--' }}</p>
              </div>

              <span class="meta-pill" :class="row.statusMeta.tone">
                {{ row.statusMeta.label }}
              </span>
            </div>

            <div class="meta-row">
              <span>投稿时间 {{ row.createTimeText }}</span>
              <span>更新时间 {{ row.lastUpdateTimeText }}</span>
              <span>时长 {{ row.durationText }}</span>
            </div>

            <div class="meta-row secondary">
              <span>播放 {{ row.playCount }}</span>
              <span>评论 {{ row.commentCount }}</span>
              <span>点赞 {{ row.likeCount }}</span>
            </div>
          </div>

          <div class="post-actions">
            <el-button type="primary" plain @click="openDetail(row)">
              详情
            </el-button>
          </div>
        </article>
      </div>

      <div v-else class="empty-state">
        当前筛选条件下暂无稿件。
      </div>
    </div>

    <div class="pager-wrap">
      <el-pagination
        background
        layout="total, sizes, prev, pager, next"
        :current-page="pagination.pageNo"
        :page-size="pagination.pageSize"
        :page-sizes="[10, 15, 20, 30]"
        :total="pagination.totalCount"
        @current-change="handlePageChange"
        @size-change="handlePageSizeChange"
      />
    </div>
  </div>
</template>

<style scoped>
.audit-list-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.content-head h2 {
  margin: 0;
  color: #1f2a44;
  font-size: 24px;
}

.content-head p {
  margin: 8px 0 0;
  color: #6f7f9f;
  font-size: 14px;
  line-height: 1.7;
}

.status-tabs {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding-bottom: 2px;
}

.status-tab {
  min-width: 112px;
  height: 40px;
  border: 1px solid #d8e1f9;
  border-radius: 12px;
  background: #f7f9ff;
  color: #34486d;
  font-size: 13px;
  cursor: pointer;
  transition: border-color 0.2s ease, background-color 0.2s ease;
}

.status-tab:hover {
  border-color: #91a7f8;
}

.status-tab.active {
  border-color: #5d76ff;
  background: linear-gradient(140deg, rgba(93, 118, 255, 0.14), rgba(31, 201, 162, 0.1));
  color: #243456;
}

.list-wrap {
  min-height: 320px;
}

.post-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.post-row {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr) auto;
  gap: 16px;
  align-items: center;
  border: 1px solid #dbe4fb;
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(246, 249, 255, 0.98));
  box-shadow: 0 12px 28px rgba(56, 84, 144, 0.08);
  padding: 16px;
}

.cover-wrap {
  min-width: 0;
}

.post-cover {
  width: 100%;
  aspect-ratio: 16 / 9;
  border-radius: 14px;
  border: 1px solid #dbe4fb;
  background: #eef3ff;
  object-fit: cover;
}

.post-cover.empty {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6f7f9f;
  font-size: 13px;
}

.post-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.title-copy {
  min-width: 0;
}

.title-copy h3 {
  margin: 0;
  color: #1f2a44;
  font-size: 18px;
}

.title-copy p,
.meta-row,
.empty-state {
  margin: 0;
  color: #6f7f9f;
  font-size: 13px;
  line-height: 1.6;
}

.meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 18px;
}

.meta-row.secondary {
  color: #50688e;
}

.post-actions {
  display: flex;
  align-items: center;
}

.pager-wrap {
  display: flex;
  justify-content: flex-end;
}

.meta-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid transparent;
  border-radius: 999px;
  padding: 7px 12px;
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
  white-space: nowrap;
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

.empty-state {
  min-height: 260px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px dashed #dbe4fb;
  border-radius: 16px;
  background: rgba(247, 250, 255, 0.75);
}

@media (max-width: 980px) {
  .post-row {
    grid-template-columns: 1fr;
  }

  .title-row {
    flex-direction: column;
    align-items: stretch;
  }

  .post-actions {
    justify-content: flex-start;
  }
}
</style>
