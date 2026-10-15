<script setup>
import { ref, onMounted } from 'vue'
import { authHeaders } from '@/admin'

const type = ref('image')
const files = ref([])
const error = ref('')
const message = ref('')
const loading = ref(false)
const uploading = ref(false)
const progress = ref(0)

const fileInput = ref(null)

async function load() {
  error.value = ''
  loading.value = true
  try {
    const res = await fetch(`/api/admin/files?type=${type.value}`, {
      headers: authHeaders(),
    })
    if (res.status === 401) throw new Error('凭证无效')
    if (!res.ok) throw new Error('加载失败')
    files.value = await res.json()
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function switchType(t) {
  type.value = t
  message.value = ''
  error.value = ''
  load()
}

function pickFile() {
  fileInput.value?.click()
}

function onFileChange(e) {
  const file = e.target.files?.[0]
  e.target.value = ''
  if (!file) return
  error.value = ''
  message.value = ''
  uploadFile(file)
}

function uploadFile(file) {
  return new Promise((resolve) => {
    const xhr = new XMLHttpRequest()
    const fd = new FormData()
    fd.append('file', file)

    uploading.value = true
    progress.value = 0

    xhr.upload.onprogress = (event) => {
      if (event.lengthComputable) {
        progress.value = Math.round((event.loaded / event.total) * 100)
      }
    }

    xhr.onload = async () => {
      uploading.value = false
      if (xhr.status === 401) {
        error.value = '凭证无效'
      } else if (xhr.status === 413) {
        error.value = '文件超过 128MB 限制'
      } else if (xhr.status >= 200 && xhr.status < 300) {
        const item = JSON.parse(xhr.responseText)
        message.value = `已上传：${item.url}`
        progress.value = 100
        await load()
      } else {
        error.value = '上传失败'
      }
      resolve()
    }

    xhr.onerror = () => {
      uploading.value = false
      error.value = '上传失败'
      resolve()
    }

    xhr.open('POST', `/api/admin/files?type=${type.value}`)
    xhr.setRequestHeader('X-Admin-Key', authHeaders()['X-Admin-Key'])
    xhr.send(fd)
  })
}

async function copyUrl(item) {
  try {
    await navigator.clipboard.writeText(item.url)
    message.value = `已复制链接：${item.url}`
  } catch {
    message.value = item.url
  }
}

async function remove(item) {
  if (!confirm(`确认删除「${item.name}」？`)) return
  error.value = ''
  message.value = ''
  try {
    const res = await fetch(`/api/admin/files/${type.value}/${item.name}`, {
      method: 'DELETE',
      headers: authHeaders(),
    })
    if (res.status === 401) throw new Error('凭证无效')
    if (!res.ok) throw new Error('删除失败')
    message.value = '已删除'
    await load()
  } catch (err) {
    error.value = err.message
  }
}

function formatSize(bytes) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

onMounted(load)
</script>

<template>
  <div class="files">
    <header class="page-header">
      <h1>文件管理</h1>
      <button type="button" class="btn primary" :disabled="uploading" @click="pickFile">
        {{ uploading ? '上传中...' : type === 'image' ? '上传图片' : '上传文件' }}
      </button>
      <input
        ref="fileInput"
        type="file"
        :accept="type === 'image' ? 'image/*' : undefined"
        class="hidden-input"
        @change="onFileChange"
      />
    </header>

    <div class="tabs">
      <button
        type="button"
        class="tab"
        :class="{ active: type === 'image' }"
        @click="switchType('image')"
      >
        图片
      </button>
      <button
        type="button"
        class="tab"
        :class="{ active: type === 'file' }"
        @click="switchType('file')"
      >
        普通文件
      </button>
    </div>

    <p v-if="error" class="hint error">{{ error }}</p>
    <p v-if="message" class="hint success">{{ message }}</p>

    <div v-if="uploading" class="progress-wrap">
      <div class="progress-bar">
        <div class="progress-fill" :style="{ width: progress + '%' }"></div>
      </div>
      <span class="progress-text">{{ progress }}%</span>
    </div>

    <p v-if="loading" class="hint">加载中...</p>
    <p v-else-if="!files.length" class="hint">暂无文件</p>

    <ul v-else class="file-list">
      <li v-for="item in files" :key="item.name" class="file-item">
        <div class="file-thumb">
          <img v-if="type === 'image'" :src="item.url" alt="" loading="lazy" />
          <span v-else class="file-icon">📄</span>
        </div>
        <div class="file-info">
          <span class="file-name">{{ item.name }}</span>
          <span class="file-meta">{{ formatSize(item.size) }}</span>
        </div>
        <div class="file-actions">
          <button type="button" class="btn small" @click="copyUrl(item)">复制链接</button>
          <button type="button" class="btn small danger" @click="remove(item)">删除</button>
        </div>
      </li>
    </ul>
  </div>
</template>

<style scoped>
.files {
  max-width: 820px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1.25rem;
}

.page-header h1 {
  margin: 0;
  font-size: 1.9rem;
}

.hidden-input {
  display: none;
}

.tabs {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1.25rem;
}

.tab {
  padding: 0.4rem 1rem;
  border: 1px solid var(--border);
  border-radius: 6px;
  background: #fff;
  color: var(--text-muted);
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.15s ease;
}

.tab.active {
  background: var(--accent);
  border-color: var(--accent);
  color: #fff;
}

.hint {
  color: var(--text-muted);
  font-size: 0.9rem;
}

.hint.error {
  color: #d1242f;
}

.hint.success {
  color: #1a7f37;
}

.progress-wrap {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.progress-bar {
  flex: 1;
  height: 8px;
  border-radius: 999px;
  background: #eef0f3;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: var(--accent);
  border-radius: 999px;
  transition: width 0.2s ease;
}

.progress-text {
  font-size: 0.82rem;
  color: var(--text-muted);
  min-width: 40px;
  text-align: right;
}

.file-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.75rem 1rem;
  background: var(--sidebar-bg);
  border: 1px solid var(--border);
  border-radius: 10px;
}

.file-thumb {
  width: 48px;
  height: 48px;
  flex-shrink: 0;
  border-radius: 6px;
  overflow: hidden;
  background: #eef0f3;
  display: flex;
  align-items: center;
  justify-content: center;
}

.file-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.file-icon {
  font-size: 1.4rem;
}

.file-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.file-name {
  font-size: 0.9rem;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  word-break: break-all;
}

.file-meta {
  font-size: 0.78rem;
  color: var(--text-muted);
}

.file-actions {
  display: flex;
  gap: 0.5rem;
  flex-shrink: 0;
}

.btn {
  padding: 0.5rem 1rem;
  border: 1px solid var(--border);
  border-radius: 6px;
  background: #fff;
  color: var(--text);
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn:hover {
  border-color: var(--accent);
  color: var(--accent);
}

.btn.primary {
  background: var(--accent);
  border-color: var(--accent);
  color: #fff;
}

.btn.primary:hover {
  opacity: 0.9;
}

.btn.small {
  padding: 0.3rem 0.75rem;
  font-size: 0.82rem;
}

.btn.danger:hover {
  border-color: #d1242f;
  color: #d1242f;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
