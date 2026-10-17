<script setup>
import { ref } from 'vue'
import { toast } from 'vue3-toastify'

const query = ref('')
const searchedQuery = ref('')
const results = ref([])
const elapsedMs = ref(0)
const loading = ref(false)
const searched = ref(false)

async function doSearch() {
  const q = query.value.trim()
  if (!q) return
  if ([...q].length < 2) {
    toast.warn('搜索关键词至少需要 2 个字符')
    return
  }
  searchedQuery.value = q
  searched.value = true
  loading.value = true
  try {
    const res = await fetch(`/api/search?q=${encodeURIComponent(q)}`)
    if (!res.ok) throw new Error('搜索失败')
    const data = await res.json()
    results.value = data.results || []
    elapsedMs.value = data.elapsedMs ?? 0
  } catch {
    results.value = []
  } finally {
    loading.value = false
  }
}

function escapeHtml(s) {
  return s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

function formatDate(value) {
  if (!value) return ''
  return new Date(value).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  })
}

// 标题命中时高亮关键词
function highlightTitle(title) {
  const q = searchedQuery.value
  if (!q) return escapeHtml(title)
  const lower = title.toLowerCase()
  const lq = q.toLowerCase()
  const idx = lower.indexOf(lq)
  if (idx < 0) return escapeHtml(title)
  return (
    escapeHtml(title.slice(0, idx)) +
    `<mark>${escapeHtml(title.slice(idx, idx + q.length))}</mark>` +
    escapeHtml(title.slice(idx + q.length))
  )
}

// 根据片段中的匹配起止位置，插入 <mark> 高亮
function highlightSnippet(snippet) {
  const text = snippet.text
  const matches = snippet.matches || []
  const parts = []
  let last = 0
  for (const m of matches) {
    if (m.start > last) parts.push(escapeHtml(text.slice(last, m.start)))
    parts.push(`<mark>${escapeHtml(text.slice(m.start, m.end))}</mark>`)
    last = m.end
  }
  if (last < text.length) parts.push(escapeHtml(text.slice(last)))
  return parts.join('')
}
</script>

<template>
  <div class="search-page" :class="{ hero: !searched }">
    <!-- 首页模式：居中大搜索框 -->
    <div v-if="!searched" class="hero-mode">
      <h1 class="hero-logo">Blog++</h1>
      <form class="hero-form" @submit.prevent="doSearch">
        <input
          v-model="query"
          type="search"
          class="search-input"
          placeholder="输入关键词搜索文章"
          autofocus
        />
      </form>
    </div>

    <!-- 结果模式 -->
    <template v-else>
      <form class="result-form" @submit.prevent="doSearch">
        <input
          v-model="query"
          type="search"
          class="search-input"
          placeholder="输入关键词搜索文章"
        />
      </form>

      <p v-if="loading" class="hint">搜索中...</p>
      <p v-else-if="!results.length" class="hint">没有找到匹配「{{ searchedQuery }}」的文章</p>

      <p v-else class="hint result-count">
        找到 {{ results.length }} 条结果，用时 {{ elapsedMs }} ms
      </p>

      <div v-if="results.length" class="results">
        <div v-for="r in results" :key="r.slug" class="result">
          <RouterLink :to="`/posts/${r.slug}`" class="result-title">
            <!-- eslint-disable-next-line vue/no-v-html -->
            <span v-html="highlightTitle(r.title)"></span>
          </RouterLink>
          <div class="result-meta">
            <time v-if="r.publishedAt" class="result-date">{{ formatDate(r.publishedAt) }}</time>
            <span v-if="r.tags && r.tags.length" class="tag-list">
              <span v-for="tag in r.tags" :key="tag" class="tag">{{ tag }}</span>
            </span>
          </div>
          <p
            v-for="(s, i) in r.snippets"
            :key="i"
            class="snippet"
            v-html="highlightSnippet(s)"
          ></p>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.search-page {
  max-width: 760px;
  margin: 0 auto;
}

/* 首页模式 */
.hero-mode {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  text-align: center;
}

.hero-logo {
  margin: 0 0 2rem;
  font-size: 3rem;
  font-weight: 700;
  letter-spacing: 0.02em;
  color: var(--text);
}

.hero-form {
  display: flex;
  width: 100%;
  max-width: 640px;
}

.search-input {
  flex: 1;
  padding: 0.75rem 1.1rem;
  font-size: 1rem;
  color: var(--text);
  background: var(--sidebar-bg);
  border: 1px solid var(--border);
  border-radius: 24px;
  outline: none;
  transition: box-shadow 0.15s ease, border-color 0.15s ease;
}

.search-input:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px rgba(47, 129, 247, 0.15);
}

/* 结果模式 */
.result-form {
  display: flex;
  margin-bottom: 1.5rem;
}

.result-form .search-input {
  border-radius: 8px;
}

.hint {
  color: var(--text-muted);
}

.result-count {
  margin: 0 0 0.75rem;
  font-size: 0.85rem;
}

.results {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.result {
  background: var(--sidebar-bg);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 1.25rem 1.5rem;
}

.result-title {
  display: block;
  margin-bottom: 0.5rem;
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--text);
}

.result-title:hover {
  color: var(--accent);
}

.result-title :deep(mark),
.snippet :deep(mark) {
  background: #ffe58f;
  color: var(--text);
  border-radius: 3px;
  padding: 0 2px;
}

.result-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-bottom: 0.25rem;
}

.result-date {
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

.snippet {
  margin: 0.35rem 0 0;
  color: var(--text-muted);
  font-size: 0.92rem;
  line-height: 1.6;
  overflow-wrap: break-word;
}

/* 手机端 */
@media (max-width: 768px) {
  .hero-mode {
    min-height: 50vh;
  }

  .hero-logo {
    font-size: 2.25rem;
  }

  .result {
    padding: 1rem 1.1rem;
  }
}
</style>
