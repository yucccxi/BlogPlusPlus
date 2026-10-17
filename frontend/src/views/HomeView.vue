<script setup>
import { ref, onMounted, computed } from 'vue'
import { toast } from 'vue3-toastify'

const posts = ref([])
const pinned = ref([])
const postsLoading = ref(true)
const pinnedLoading = ref(true)
const page = ref(1)
const size = 10
const total = ref(0)

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size)))

async function loadPosts() {
  postsLoading.value = true
  try {
    const res = await fetch(`/api/posts?page=${page.value}&size=${size}`)
    if (!res.ok) throw new Error('加载文章失败')
    const data = await res.json()
    posts.value = data.items || []
    total.value = data.total || 0
  } catch (e) {
    toast.error(e.message || '加载文章失败')
  } finally {
    postsLoading.value = false
  }
}

async function loadPinned() {
  pinnedLoading.value = true
  try {
    const res = await fetch('/api/posts/pinned')
    if (res.ok) pinned.value = await res.json()
  } catch {
    pinned.value = []
  } finally {
    pinnedLoading.value = false
  }
}

onMounted(() => {
  loadPosts()
  loadPinned()
})

function goPage(p) {
  if (p < 1 || p > totalPages.value || p === page.value) return
  page.value = p
  loadPosts()
}

function formatDate(value) {
  if (!value) return ''
  return new Date(value).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  })
}
</script>

<template>
  <div class="home">
    <div class="main-col">
      <div v-if="postsLoading" class="post-list">
        <div v-for="n in 4" :key="n" class="post-card skeleton-card">
          <div class="skeleton-line skeleton-title"></div>
          <div class="skeleton-line skeleton-text"></div>
          <div class="skeleton-line skeleton-text short"></div>
        </div>
      </div>

      <p v-else-if="!posts.length" class="hint">暂无文章</p>

      <div v-else class="post-list">
        <RouterLink
          v-for="post in posts"
          :key="post.id"
          :to="`/posts/${post.slug}`"
          class="post-card"
        >
          <h2 class="post-title">{{ post.title }}</h2>
          <p v-if="post.summary" class="post-summary">{{ post.summary }}</p>
          <div class="post-footer">
            <time v-if="post.publishedAt" class="post-date">{{ formatDate(post.publishedAt) }}</time>
            <span v-if="post.tags && post.tags.length" class="tag-list">
              <span v-for="tag in post.tags" :key="tag" class="tag">{{ tag }}</span>
            </span>
          </div>
        </RouterLink>
      </div>

      <nav v-if="totalPages > 1" class="pagination">
        <button
          type="button"
          class="page-btn"
          :disabled="page <= 1"
          @click="goPage(page - 1)"
        >
          上一页
        </button>
        <span class="page-info">{{ page }} / {{ totalPages }}</span>
        <button
          type="button"
          class="page-btn"
          :disabled="page >= totalPages"
          @click="goPage(page + 1)"
        >
          下一页
        </button>
      </nav>
    </div>

    <aside v-if="pinnedLoading || pinned.length" class="side-col">
      <h2 class="side-title">已固定</h2>
      <ul v-if="pinnedLoading" class="pinned-list">
        <li v-for="n in 3" :key="n" class="skeleton-item"></li>
      </ul>
      <ul v-else class="pinned-list">
        <li v-for="post in pinned" :key="post.id">
          <RouterLink :to="`/posts/${post.slug}`" class="pinned-link">
            {{ post.title }}
          </RouterLink>
        </li>
      </ul>
    </aside>
  </div>
</template>

<style scoped>
.home {
  display: flex;
  gap: 2.5rem;
  max-width: 1000px;
  margin: 0 auto;
  align-items: flex-start;
}

.main-col {
  flex: 1;
  min-width: 0;
}

.side-col {
  width: 240px;
  flex-shrink: 0;
  position: sticky;
  top: 2.5rem;
  background: var(--sidebar-bg);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 1.25rem 1.5rem;
}

.page-header {
  margin-bottom: 2rem;
}

.page-header h1 {
  margin: 0 0 0.25rem;
  font-size: 1.9rem;
}

.subtitle {
  margin: 0;
  color: var(--text-muted);
}

.hint {
  color: var(--text-muted);
}

.post-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.post-card {
  display: block;
  background: var(--sidebar-bg);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 1.25rem 1.5rem;
  color: var(--text);
  transition:
    border-color 0.15s ease,
    box-shadow 0.15s ease;
}

.post-card:hover {
  border-color: var(--accent);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  text-decoration: none;
}

.post-title {
  margin: 0 0 0.5rem;
  font-size: 1.15rem;
}

.post-summary {
  margin: 0 0 0.5rem;
  color: var(--text-muted);
  font-size: 0.92rem;
}

.post-footer {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.post-date {
  color: var(--text-muted);
  font-size: 0.82rem;
}

.tag-list {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 0.4rem;
}

.tag {
  font-size: 0.75rem;
  padding: 0.1rem 0.55rem;
  border-radius: 999px;
  background: #eef0f3;
  color: var(--text-muted);
}

.side-title {
  margin: 0 0 0.75rem;
  font-size: 0.85rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--text-muted);
}

.pinned-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.pinned-link {
  display: block;
  color: var(--text);
  font-size: 0.92rem;
  line-height: 1.4;
  padding: 0.35rem 0;
}

.pinned-link:hover {
  color: var(--accent);
}

.skeleton-item {
  height: 1rem;
  border-radius: 4px;
  background: linear-gradient(90deg, #eef0f3 25%, #f7f7f8 50%, #eef0f3 75%);
  background-size: 200% 100%;
  animation: shimmer 1.2s infinite;
}

.skeleton-item:nth-child(2) {
  width: 80%;
}

.skeleton-item:nth-child(3) {
  width: 60%;
}

.skeleton-line {
  border-radius: 4px;
  background: linear-gradient(90deg, #eef0f3 25%, #f7f7f8 50%, #eef0f3 75%);
  background-size: 200% 100%;
  animation: shimmer 1.2s infinite;
}

.skeleton-card {
  cursor: default;
}

.skeleton-title {
  height: 1.1rem;
  width: 40%;
  margin-bottom: 0.75rem;
}

.skeleton-text {
  height: 0.85rem;
  margin-bottom: 0.5rem;
}

.skeleton-text.short {
  width: 70%;
}

@keyframes shimmer {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  margin-top: 1.5rem;
}

.page-btn {
  padding: 0.4rem 1rem;
  border: 1px solid var(--border);
  border-radius: 6px;
  background: var(--sidebar-bg);
  color: var(--text);
  font-size: 0.88rem;
  cursor: pointer;
  transition: all 0.15s ease;
}

.page-btn:hover:not(:disabled) {
  border-color: var(--accent);
  color: var(--accent);
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-info {
  color: var(--text-muted);
  font-size: 0.88rem;
}

/* 手机端：单列堆叠，置顶栏目移到文章下方 */
@media (max-width: 768px) {
  .home {
    flex-direction: column;
    align-items: stretch;
    gap: 1.5rem;
  }

  .side-col {
    width: 100%;
    position: static;
  }

  .page-header h1 {
    font-size: 1.5rem;
  }

  .post-card {
    padding: 1rem 1.1rem;
  }

  .post-title {
    font-size: 1.05rem;
  }
}
</style>
