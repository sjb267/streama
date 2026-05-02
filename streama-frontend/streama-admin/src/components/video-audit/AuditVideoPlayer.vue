<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import Hls from 'hls.js'
import Artplayer from 'artplayer'
import { formatDuration, toFiniteNumber } from '@/utils/videoAudit'

const props = defineProps({
  fileId: {
    type: String,
    default: '',
  },
  markers: {
    type: Array,
    default: () => [],
  },
  activeMarkerKey: {
    type: String,
    default: '',
  },
  fallbackDuration: {
    type: Number,
    default: 0,
  },
})

const emit = defineEmits(['duration-change', 'time-change', 'marker-select', 'error'])

const playerHostRef = ref(null)
const trackRef = ref(null)
const playerLoading = ref(false)
const playerError = ref('')
const currentTime = ref(0)
const duration = ref(0)

let artPlayer = null
let trackedVideoElement = null
let manifestRequestController = null
let currentManifestObjectUrl = ''
let streamLoadToken = 0

const effectiveDuration = computed(() => {
  if (duration.value > 0) {
    return duration.value
  }
  const fallbackDuration = Number(props.fallbackDuration || 0)
  return Number.isFinite(fallbackDuration) && fallbackDuration > 0 ? fallbackDuration : 0
})

const progressPercent = computed(() => {
  if (effectiveDuration.value <= 0) {
    return 0
  }
  return clampPercent((currentTime.value / effectiveDuration.value) * 100)
})

const normalizedMarkers = computed(() => {
  const totalDuration = effectiveDuration.value
  if (!Array.isArray(props.markers) || props.markers.length === 0 || totalDuration <= 0) {
    return []
  }

  return props.markers
    .map((marker, index) => {
      const seekSeconds = toFiniteNumber(marker?.seekSeconds ?? marker?.startSeconds)
      if (seekSeconds === null) {
        return null
      }

      const startSeconds = toFiniteNumber(marker?.startSeconds) ?? seekSeconds
      const rawEndSeconds = toFiniteNumber(marker?.endSeconds)
      const endSeconds = rawEndSeconds !== null && rawEndSeconds > startSeconds ? rawEndSeconds : startSeconds
      const leftPercent = clampPercent((startSeconds / totalDuration) * 100)
      const widthPercent = Math.max(clampPercent((endSeconds / totalDuration) * 100) - leftPercent, 0.8)

      return {
        ...marker,
        markerKey: marker?.segmentKey || `marker-${index}`,
        leftPercent,
        widthPercent: Math.min(widthPercent, 100 - leftPercent),
        isPoint: endSeconds <= startSeconds,
      }
    })
    .filter(Boolean)
})

watch(
  () => props.fileId,
  (fileId) => {
    attachStream(fileId)
  },
  { immediate: true },
)

onBeforeUnmount(() => {
  clearStreamState(true)
})

function clampPercent(value) {
  const numericValue = Number(value)
  if (!Number.isFinite(numericValue)) {
    return 0
  }
  return Math.min(100, Math.max(0, numericValue))
}

function getStreamUrl(fileId) {
  const id = String(fileId || '').trim()
  if (!id) {
    return ''
  }
  return `/admin/file/videoResource/${encodeURIComponent(id)}`
}

function getTsBaseUrl(fileId) {
  const id = String(fileId || '').trim()
  if (!id) {
    return ''
  }
  return `${window.location.origin}/admin/file/videoResource/${encodeURIComponent(id)}/`
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
  } catch {
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
      segmentCount: 0,
    }
  }

  const validSegmentCount = segmentUris.filter((uri) => canResolveUri(resolveManifestUri(fileId, uri))).length
  if (validSegmentCount === 0 || !canResolveUri(firstSegmentUrl)) {
    return {
      valid: false,
      segmentCount: validSegmentCount,
    }
  }

  return {
    valid: true,
    segmentCount: validSegmentCount,
  }
}

function rewriteManifestContent(fileId, manifestText) {
  const lines = String(manifestText || '').split(/\r?\n/)
  let firstSegmentUrl = ''

  const rewrittenLines = lines.map((line) => {
    const rawLine = String(line || '')
    const trimmedLine = rawLine.trim()
    if (!trimmedLine) {
      return rawLine
    }

    if (trimmedLine.startsWith('#')) {
      if (!trimmedLine.includes('URI=')) {
        return rawLine
      }
      return rawLine.replace(/URI=("([^"]*)"|'([^']*)')/g, (_match, quoted, doubleQuoteValue, singleQuoteValue) => {
        const original = doubleQuoteValue ?? singleQuoteValue ?? ''
        const nextUri = resolveManifestUri(fileId, original)
        const quote = quoted.startsWith("'") ? "'" : '"'
        return `URI=${quote}${nextUri}${quote}`
      })
    }

    const rewrittenUri = resolveManifestUri(fileId, trimmedLine)
    if (!firstSegmentUrl) {
      firstSegmentUrl = rewrittenUri
    }
    return rewrittenUri
  })

  return {
    content: rewrittenLines.join('\n'),
    firstSegmentUrl,
  }
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

function removeVideoListeners(videoElement) {
  if (!videoElement) {
    return
  }
  videoElement.removeEventListener('timeupdate', handleTimeUpdate)
  videoElement.removeEventListener('loadedmetadata', handleDurationChange)
  videoElement.removeEventListener('durationchange', handleDurationChange)
}

function bindVideoListeners(videoElement) {
  removeVideoListeners(trackedVideoElement)
  trackedVideoElement = videoElement
  if (!trackedVideoElement) {
    return
  }
  trackedVideoElement.addEventListener('timeupdate', handleTimeUpdate)
  trackedVideoElement.addEventListener('loadedmetadata', handleDurationChange)
  trackedVideoElement.addEventListener('durationchange', handleDurationChange)
}

function handleTimeUpdate() {
  if (!trackedVideoElement) {
    return
  }
  currentTime.value = Number(trackedVideoElement.currentTime || 0)
  emit('time-change', currentTime.value)
}

function handleDurationChange() {
  if (!trackedVideoElement) {
    return
  }
  const nextDuration = Number(trackedVideoElement.duration || 0)
  duration.value = Number.isFinite(nextDuration) && nextDuration > 0 ? nextDuration : 0
  emit('duration-change', duration.value || effectiveDuration.value || 0)
}

function destroyArtPlayer() {
  if (!artPlayer) {
    removeVideoListeners(trackedVideoElement)
    trackedVideoElement = null
    return
  }

  removeVideoListeners(trackedVideoElement)
  trackedVideoElement = null
  artPlayer.destroy(false)
  artPlayer = null
}

function stopVideoElement() {
  if (trackedVideoElement) {
    trackedVideoElement.pause()
    trackedVideoElement.removeAttribute('src')
    trackedVideoElement.load()
  }
  if (artPlayer && typeof artPlayer.pause === 'function') {
    artPlayer.pause()
  }
}

function createArtPlayer(url = '') {
  if (artPlayer || !playerHostRef.value) {
    return
  }

  artPlayer = new Artplayer({
    container: playerHostRef.value,
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

        playerError.value = '当前浏览器不支持 m3u8 播放。'
        emit('error', playerError.value)
      },
    },
  })

  bindVideoListeners(artPlayer.video)
}

async function setArtPlayerSource(url) {
  if (!url) {
    return
  }

  destroyArtPlayer()
  await nextTick()
  createArtPlayer(url)
}

async function buildRewrittenManifestObjectUrl(fileId, signal) {
  const streamUrl = getStreamUrl(fileId)
  if (!streamUrl) {
    return {
      playUrl: '',
      objectUrl: '',
    }
  }

  const response = await fetch(streamUrl, {
    method: 'GET',
    credentials: 'include',
    cache: 'no-store',
    signal,
  })

  if (!response.ok) {
    throw new Error(`Manifest request failed: ${response.status}`)
  }

  const manifestText = await response.text()
  const rewrittenManifest = rewriteManifestContent(fileId, manifestText)
  const manifestValidation = validateRewrittenManifest(fileId, rewrittenManifest.content, rewrittenManifest.firstSegmentUrl)

  if (!manifestValidation.valid) {
    return {
      playUrl: streamUrl,
      objectUrl: '',
    }
  }

  const blob = new Blob([rewrittenManifest.content], { type: 'application/vnd.apple.mpegurl' })
  const objectUrl = URL.createObjectURL(blob)
  return {
    playUrl: objectUrl,
    objectUrl,
  }
}

function clearStreamState(resetError = false) {
  streamLoadToken += 1
  abortManifestRequest()
  stopVideoElement()
  destroyArtPlayer()
  revokeManifestObjectUrl()
  currentTime.value = 0
  duration.value = 0
  playerLoading.value = false
  if (resetError) {
    playerError.value = ''
  }
}

async function attachStream(fileId) {
  const targetFileId = String(fileId || '').trim()
  playerError.value = ''

  if (!targetFileId) {
    clearStreamState(true)
    return
  }

  const streamUrl = getStreamUrl(targetFileId)
  if (!streamUrl) {
    playerError.value = '缺少可播放文件。'
    emit('error', playerError.value)
    return
  }

  const requestToken = ++streamLoadToken
  abortManifestRequest()
  stopVideoElement()
  destroyArtPlayer()
  revokeManifestObjectUrl()
  currentTime.value = 0
  duration.value = 0
  playerLoading.value = true

  const controller = new AbortController()
  manifestRequestController = controller
  const timeoutId = window.setTimeout(() => {
    controller.abort()
  }, 12000)

  let playbackUrl = streamUrl
  let rewrittenObjectUrl = ''

  try {
    const rewritten = await buildRewrittenManifestObjectUrl(targetFileId, controller.signal)
    playbackUrl = rewritten.playUrl || streamUrl
    rewrittenObjectUrl = rewritten.objectUrl || ''
  } catch (error) {
    clearTimeout(timeoutId)
    if (manifestRequestController === controller) {
      manifestRequestController = null
    }
    if (error?.name === 'AbortError' || requestToken !== streamLoadToken) {
      return
    }
    playerLoading.value = false
    playerError.value = '视频播放失败，请稍后重试。'
    emit('error', playerError.value)
    return
  }

  clearTimeout(timeoutId)
  if (manifestRequestController === controller) {
    manifestRequestController = null
  }

  if (requestToken !== streamLoadToken) {
    if (rewrittenObjectUrl) {
      URL.revokeObjectURL(rewrittenObjectUrl)
    }
    return
  }

  currentManifestObjectUrl = rewrittenObjectUrl

  try {
    await setArtPlayerSource(playbackUrl)
    if (requestToken !== streamLoadToken) {
      return
    }
    handleDurationChange()
    playerLoading.value = false
  } catch {
    playerLoading.value = false
    playerError.value = '视频播放失败，请稍后重试。'
    emit('error', playerError.value)
  }
}

function seekTo(seconds) {
  const targetSeconds = Number(seconds)
  if (!Number.isFinite(targetSeconds) || targetSeconds < 0) {
    return
  }

  if (trackedVideoElement) {
    trackedVideoElement.currentTime = targetSeconds
    currentTime.value = targetSeconds
    emit('time-change', targetSeconds)
    return
  }

  if (artPlayer) {
    artPlayer.currentTime = targetSeconds
    currentTime.value = targetSeconds
    emit('time-change', targetSeconds)
  }
}

function handleTrackClick(event) {
  if (!trackRef.value || effectiveDuration.value <= 0) {
    return
  }

  const rect = trackRef.value.getBoundingClientRect()
  const offsetX = event.clientX - rect.left
  const progress = rect.width > 0 ? offsetX / rect.width : 0
  seekTo(effectiveDuration.value * clampPercent(progress * 100) / 100)
}

function handleMarkerClick(marker) {
  const seekSeconds = toFiniteNumber(marker?.seekSeconds)
  if (seekSeconds === null) {
    return
  }
  seekTo(seekSeconds)
  emit('marker-select', marker)
}

defineExpose({
  seekTo,
  reloadCurrent: () => attachStream(props.fileId),
})
</script>

<template>
  <div class="audit-player">
    <div class="player-shell">
      <div ref="playerHostRef" class="player-host" />

      <div v-if="!fileId" class="player-overlay empty">
        暂无可播放文件
      </div>

      <div v-else-if="playerLoading" class="player-overlay">
        视频加载中...
      </div>

      <div v-else-if="playerError" class="player-overlay error">
        {{ playerError }}
      </div>
    </div>

    <div class="timeline-panel">
      <div class="timeline-meta">
        <span>当前进度 {{ formatDuration(currentTime) }}</span>
        <span>总时长 {{ formatDuration(effectiveDuration) }}</span>
      </div>

      <div ref="trackRef" class="timeline-track" @click="handleTrackClick">
        <span class="timeline-progress" :style="{ width: `${progressPercent}%` }" />

        <button
          v-for="marker in normalizedMarkers"
          :key="marker.markerKey"
          type="button"
          class="timeline-marker"
          :class="{
            active: marker.markerKey === activeMarkerKey,
            point: marker.isPoint,
            risky: marker.isRisky,
            normal: !marker.isRisky,
          }"
          :style="{
            left: `${marker.leftPercent}%`,
            width: `${marker.widthPercent}%`,
          }"
          :title="marker.timeText || marker.riskTypeText || marker.riskType || '审核片段'"
          @click.stop="handleMarkerClick(marker)"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.audit-player {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.player-shell {
  position: relative;
  overflow: hidden;
  border: 1px solid #dbe4fb;
  border-radius: 18px;
  background: #0f172a;
}

.player-host {
  width: 100%;
  aspect-ratio: 16 / 9;
}

.player-host :deep(.art-video-player) {
  width: 100%;
  height: 100%;
}

.player-overlay {
  position: absolute;
  inset: 0;
  z-index: 5;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(15, 23, 42, 0.6);
  color: #eef4ff;
  font-size: 14px;
  backdrop-filter: blur(2px);
}

.player-overlay.error {
  background: rgba(127, 29, 29, 0.58);
}

.player-overlay.empty {
  background: rgba(15, 23, 42, 0.82);
}

.timeline-panel {
  border: 1px solid #dbe4fb;
  border-radius: 16px;
  background: linear-gradient(180deg, #fbfdff, #f5f8ff);
  padding: 12px 14px 14px;
}

.timeline-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #62779f;
  font-size: 12px;
}

.timeline-track {
  position: relative;
  overflow: hidden;
  margin-top: 10px;
  height: 14px;
  border-radius: 999px;
  background: #e2eafc;
  cursor: pointer;
}

.timeline-progress {
  position: absolute;
  inset: 0 auto 0 0;
  border-radius: inherit;
  background: linear-gradient(90deg, #5d76ff, #7ca3ff);
}

.timeline-marker {
  position: absolute;
  top: 2px;
  bottom: 2px;
  border: none;
  border-radius: 999px;
  background: rgba(220, 38, 38, 0.88);
  cursor: pointer;
  transform: translateX(0);
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.38);
}

.timeline-marker.normal {
  background: rgba(24, 121, 78, 0.86);
}

.timeline-marker.risky {
  background: rgba(220, 38, 38, 0.88);
}

.timeline-marker.point {
  min-width: 6px;
}

.timeline-marker.active {
  background: #991b1b;
  box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.88);
}

.timeline-marker.normal.active {
  background: #18794e;
}

@media (max-width: 720px) {
  .timeline-meta {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
