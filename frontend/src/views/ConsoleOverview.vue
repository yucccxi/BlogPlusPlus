<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { toast } from 'vue3-toastify'
import { adminStore, authHeaders } from '@/admin'

const router = useRouter()
const posts = ref([])
const loading = ref(true)

const publishedPage = ref(1)
const draftPage = ref(1)
const PUBLISHED_PAGE_SIZE = 8
const DRAFT_PAGE_SIZE = 8

const publishedPosts = computed(() =>
  posts.value
    .filter((p) => p.published)
    .sort((a, b) => new Date(b.publishedAt || 0) - new Date(a.publishedAt || 0)),
)
const draftPosts = computed(() =>
  posts.value
    .filter((p) => !p.published)
    .sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0)),
)

const tagCount = computed(() => {
  const set = new Set()
  for (const p of posts.value) {
    for (const t of p.tags || []) set.add(t)
  }
  return set.size
})

// 贡献图：统计已发布文章每天的发布次数
const contributionMap = computed(() => {
  const map = {}
  for (const p of publishedPosts.value) {
    if (!p.publishedAt) continue
    const d = new Date(p.publishedAt)
    const key = `${d.getFullYear()}-${d.getMonth()}-${d.getDate()}`
    map[key] = (map[key] || 0) + 1
  }
  return map
})

// 生成最近 53 周（含本周）的贡献格子，每列 7 天（周日~周六，最上面是 Sunday）
const contributionWeeks = computed(() => {
  const map = contributionMap.value
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const start = new Date(today)
  start.setDate(today.getDate() - today.getDay() - 52 * 7)

  const weeks = []
  const cursor = new Date(start)
  for (let w = 0; w < 53; w++) {
    const week = []
    for (let d = 0; d < 7; d++) {
      const key = `${cursor.getFullYear()}-${cursor.getMonth()}-${cursor.getDate()}`
      week.push({
        date: new Date(cursor),
        count: map[key] || 0,
        future: cursor > today,
      })
      cursor.setDate(cursor.getDate() + 1)
    }
    weeks.push(week)
  }
  return weeks
})

const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']

// 每个自然月起始列 -> 月份缩写，用于顶部月份标签
const monthLabels = computed(() => {
  const weeks = contributionWeeks.value
  const labels = new Map()
  let prev = -1
  weeks.forEach((week, i) => {
    const m = week[0].date.getMonth()
    if (m !== prev) {
      labels.set(i, MONTHS[m])
      prev = m
    }
  })
  return labels
})

function contributionLevel(count) {
  if (count === 0) return 0
  if (count === 1) return 1
  if (count <= 3) return 2
  return 3
}

function cellTitle(cell) {
  const d = cell.date
  const label = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(
    d.getDate(),
  ).padStart(2, '0')}`
  return cell.count ? `${label}：${cell.count} 篇` : label
}

const publishedTotalPages = computed(() =>
  Math.max(1, Math.ceil(publishedPosts.value.length / PUBLISHED_PAGE_SIZE)),
)
const draftTotalPages = computed(() =>
  Math.max(1, Math.ceil(draftPosts.value.length / DRAFT_PAGE_SIZE)),
)

const pagedPublished = computed(() => {
  const start = (publishedPage.value - 1) * PUBLISHED_PAGE_SIZE
  return publishedPosts.value.slice(start, start + PUBLISHED_PAGE_SIZE)
})
const pagedDraft = computed(() => {
  const start = (draftPage.value - 1) * DRAFT_PAGE_SIZE
  return draftPosts.value.slice(start, start + DRAFT_PAGE_SIZE)
})

async function load() {
  loading.value = true
  if (!adminStore.key) {
    posts.value = []
    loading.value = false
    return
  }
  try {
    const res = await fetch('/api/admin/posts', { headers: authHeaders() })
    if (res.status === 401) throw new Error('凭证无效')
    if (!res.ok) throw new Error('加载文章失败')
    posts.value = await res.json()
  } catch (e) {
    toast.error(e.message)
  } finally {
    loading.value = false
  }
}

function createPost() {
  router.push('/console/new')
}

function editPost(post) {
  router.push(`/console/edit/${post.id}`)
}

async function deletePost(post) {
  if (!confirm(`确认删除「${post.title}」？`)) return
  try {
    const res = await fetch(`/api/admin/posts/${post.id}`, {
      method: 'DELETE',
      headers: authHeaders(),
    })
    if (res.status === 401) throw new Error('凭证无效')
    if (!res.ok) throw new Error('删除失败')
    toast.success('已删除')
    await load()
  } catch (e) {
    toast.error(e.message)
  }
}

function formatDate(value) {
  if (!value) return ''
  return new Date(value).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  })
}

function goPublishedPage(p) {
  if (p < 1 || p > publishedTotalPages.value || p === publishedPage.value) return
  publishedPage.value = p
}

function goDraftPage(p) {
  if (p < 1 || p > draftTotalPages.value || p === draftPage.value) return
  draftPage.value = p
}

onMounted(load)
watch(() => adminStore.key, load)
</script>

<template>
  <div class="overview">
    <header class="page-header">
      <h1>概览</h1>
    </header>

    <p v-if="!adminStore.key" class="hint">请先在上方输入访问凭证</p>
    <p v-else-if="loading" class="hint">加载中...</p>

    <template v-else>
      <!-- 统计卡片 -->
      <section class="stats">
        <div class="stat-card">
          <span class="stat-num">{{ publishedPosts.length }}</span>
          <span class="stat-label">已发布</span>
        </div>
        <div class="stat-card">
          <span class="stat-num">{{ draftPosts.length }}</span>
          <span class="stat-label">未发布</span>
        </div>
        <div class="stat-card">
          <span class="stat-num">{{ tagCount }}</span>
          <span class="stat-label">标签</span>
        </div>
      </section>

      <!-- 贡献图 -->
      <section class="contribution">
        <h2 class="section-title">发布贡献</h2>
        <div class="contribution-chart">
          <div class="month-row">
            <div
              v-for="(week, wi) in contributionWeeks"
              :key="'m' + wi"
              class="month-cell"
            >
              <span v-if="monthLabels.has(wi)">{{ monthLabels.get(wi) }}</span>
            </div>
          </div>
          <div class="chart-body">
            <div class="weekday-col">
              <span v-for="(d, di) in 7" :key="'w' + di" class="weekday-label">
                {{ di === 1 ? 'Mon' : di === 3 ? 'Wed' : di === 5 ? 'Fri' : '' }}
              </span>
            </div>
            <div class="contribution-grid">
              <div v-for="(week, wi) in contributionWeeks" :key="wi" class="contribution-week">
                <div
                  v-for="(cell, di) in week"
                  :key="di"
                  class="contribution-cell"
                  :class="[cell.future ? 'future' : 'level-' + contributionLevel(cell.count)]"
                  :title="cellTitle(cell)"
                ></div>
              </div>
            </div>
          </div>
        </div>
        <div class="contribution-legend">
          <span class="legend-label">少</span>
          <span class="contribution-cell level-0"></span>
          <span class="contribution-cell level-1"></span>
          <span class="contribution-cell level-2"></span>
          <span class="contribution-cell level-3"></span>
          <span class="legend-label">多</span>
        </div>
      </section>

      <!-- 已发布列表 -->
      <section class="section">
        <h2 class="section-title">已发布（{{ publishedPosts.length }}）</h2>
        <p v-if="!publishedPosts.length" class="hint">暂无已发布文章</p>
        <template v-else>
          <Transition name="page-fade" mode="out-in">
            <ul class="post-list" :key="publishedPage">
              <li v-if="publishedPage === 1" class="post-item create-item">
                <button type="button" class="btn-add" title="新建文章" @click="createPost">+</button>
              </li>
              <li v-for="post in pagedPublished" :key="post.id" class="post-item">
                <div class="post-info">
                  <span class="post-title">{{ post.title }}</span>
                  <span class="post-slug">{{ post.slug }}</span>
                  <span v-if="post.publishedAt" class="post-date">{{ formatDate(post.publishedAt) }}</span>
                  <span v-if="post.pinned" class="badge">已固定</span>
                </div>
                <div class="post-actions">
                  <button type="button" class="btn small" @click="editPost(post)">编辑</button>
                  <button type="button" class="btn small danger" @click="deletePost(post)">删除</button>
                </div>
              </li>
            </ul>
          </Transition>
          <nav v-if="publishedTotalPages > 1" class="pagination">
            <button
              type="button"
              class="page-btn"
              :disabled="publishedPage <= 1"
              @click="goPublishedPage(publishedPage - 1)"
            >
              上一页
            </button>
            <span class="page-info">{{ publishedPage }} / {{ publishedTotalPages }}</span>
            <button
              type="button"
              class="page-btn"
              :disabled="publishedPage >= publishedTotalPages"
              @click="goPublishedPage(publishedPage + 1)"
            >
              下一页
            </button>
          </nav>
        </template>
      </section>

      <!-- 未发布列表 -->
      <section class="section">
        <h2 class="section-title">未发布（{{ draftPosts.length }}）</h2>
        <p v-if="!draftPosts.length" class="hint">暂无未发布文章</p>
        <template v-else>
          <Transition name="page-fade" mode="out-in">
            <ul class="post-list" :key="draftPage">
              <li v-for="post in pagedDraft" :key="post.id" class="post-item">
                <div class="post-info">
                  <span class="post-title">{{ post.title }}</span>
                  <span class="post-slug">{{ post.slug }}</span>
                  <span v-if="post.pinned" class="badge">已固定</span>
                </div>
                <div class="post-actions">
                  <button type="button" class="btn small" @click="editPost(post)">编辑</button>
                  <button type="button" class="btn small danger" @click="deletePost(post)">删除</button>
                </div>
              </li>
            </ul>
          </Transition>
          <nav v-if="draftTotalPages > 1" class="pagination">
            <button
              type="button"
              class="page-btn"
              :disabled="draftPage <= 1"
              @click="goDraftPage(draftPage - 1)"
            >
              上一页
            </button>
            <span class="page-info">{{ draftPage }} / {{ draftTotalPages }}</span>
            <button
              type="button"
              class="page-btn"
              :disabled="draftPage >= draftTotalPages"
              @click="goDraftPage(draftPage + 1)"
            >
              下一页
            </button>
          </nav>
        </template>
      </section>
    </template>
  </div>
</template>

<style scoped>
.overview {
  max-width: 820px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 1.5rem;
}

.page-header h1 {
  margin: 0;
  font-size: 1.9rem;
}

/* 统计卡片 */
.stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1rem;
  margin-bottom: 2rem;
}

.stat-card {
  background: var(--sidebar-bg);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 1.25rem 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.stat-num {
  font-size: 2rem;
  font-weight: 700;
  color: var(--text);
}

.stat-label {
  font-size: 0.85rem;
  color: var(--text-muted);
}

/* 贡献图 */
.contribution {
  margin-bottom: 2rem;
}

.contribution-chart {
  overflow-x: auto;
  padding-bottom: 0.5rem;
}

.month-row {
  display: flex;
  gap: 3px;
  margin-left: 22px;
  margin-bottom: 3px;
  height: 16px;
}

.month-cell {
  width: 11px;
  flex-shrink: 0;
  position: relative;
  font-size: 0.72rem;
  color: var(--text-muted);
}

.month-cell span {
  position: absolute;
  left: 0;
  top: 0;
  white-space: nowrap;
}

.chart-body {
  display: flex;
  gap: 3px;
}

.weekday-col {
  display: flex;
  flex-direction: column;
  gap: 3px;
  width: 22px;
  flex-shrink: 0;
}

.weekday-label {
  height: 11px;
  font-size: 0.7rem;
  color: var(--text-muted);
  line-height: 11px;
  white-space: nowrap;
}

.contribution-grid {
  display: flex;
  gap: 3px;
}

.contribution-week {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.contribution-cell {
  width: 11px;
  height: 11px;
  border-radius: 2px;
  background: #ebedf0;
}

.contribution-cell.future {
  background: transparent;
}

.contribution-cell.level-1 {
  background: #9be9a8;
}

.contribution-cell.level-2 {
  background: #40c463;
}

.contribution-cell.level-3 {
  background: #216e39;
}

.contribution-legend {
  display: flex;
  align-items: center;
  gap: 3px;
  justify-content: flex-end;
  margin-top: 0.5rem;
}

.legend-label {
  font-size: 0.75rem;
  color: var(--text-muted);
  margin: 0 0.25rem;
}

.section {
  margin-bottom: 2rem;
}

.section-title {
  margin: 0 0 1rem;
  font-size: 1.1rem;
}

.hint {
  color: var(--text-muted);
  font-size: 0.9rem;
}

.post-list {
  list-style: none;
  margin: 0;
  padding: 0;
  background: var(--sidebar-bg);
  border: 1px solid var(--border);
  border-radius: 10px;
  overflow: hidden;
}

/* 翻页过渡动画 */
.page-fade-enter-active,
.page-fade-leave-active {
  transition:
    opacity 0.2s ease,
    transform 0.2s ease;
}

.page-fade-enter-from {
  opacity: 0;
  transform: translateY(6px);
}

.page-fade-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

.post-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.85rem 1.25rem;
  border-bottom: 1px solid var(--border);
}

.post-item:last-child {
  border-bottom: none;
}

.create-item {
  background: var(--bg);
  justify-content: center;
}

.btn-add {
  width: 100%;
  padding: 0.4rem 0;
  border: none;
  border-radius: 6px;
  background: #dafbe1;
  color: #1a7f37;
  font-size: 1.6rem;
  line-height: 1;
  cursor: pointer;
  transition: background 0.15s ease;
}

.btn-add:hover {
  background: #aceebb;
}

.post-info {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  min-width: 0;
}

.post-title {
  font-size: 0.95rem;
  font-weight: 600;
}

.post-slug {
  color: var(--text-muted);
  font-size: 0.8rem;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
}

.post-date {
  color: var(--text-muted);
  font-size: 0.8rem;
}

.badge {
  font-size: 0.72rem;
  padding: 0.1rem 0.5rem;
  border-radius: 999px;
  background: #ddf4ff;
  color: #0969da;
}

.post-actions {
  display: flex;
  gap: 0.5rem;
  flex-shrink: 0;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  margin-top: 1rem;
}

.page-btn {
  padding: 0.3rem 0.9rem;
  border: 1px solid var(--border);
  border-radius: 6px;
  background: var(--sidebar-bg);
  color: var(--text);
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.15s ease;
}

.page-btn:hover:not(:disabled) {
  border-color: var(--accent);
  color: var(--accent);
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: default;
}

.page-info {
  font-size: 0.85rem;
  color: var(--text-muted);
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
</style>
