<script setup>
import { ref, watch, onBeforeUnmount } from 'vue'

const query = ref('')
const results = ref([])
const loading = ref(false)
const searched = ref(false)
let timer = null

watch(query, (val) => {
  clearTimeout(timer)
  const q = val.trim()
  if (!q) {
    results.value = []
    searched.value = false
    loading.value = false
    return
  }
  loading.value = true
  timer = setTimeout(async () => {
    try {
      const res = await fetch(`/api/search?q=${encodeURIComponent(q)}`)
      if (!res.ok) throw new Error('搜索失败')
      const data = await res.json()
      results.value = data.results || []
      searched.value = true
    } catch {
      results.value = []
      searched.value = true
    } finally {
      loading.value = false
    }
  }, 300)
})

onBeforeUnmount(() => clearTimeout(timer))

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
  <div class="search-page">
    <header class="page-header">
      <h1>搜索</h1>
      <p class="subtitle">搜索所有已发布文章的内容</p>
    </header>

    <div class="search-box">
      <input
        v-model="query"
        type="search"
        class="search-input"
        placeholder="输入关键词搜索文章内容..."
        autofocus
      />
    </div>

    <p v-if="loading" class="hint">搜索中...</p>
    <p v-else-if="searched && !results.length" class="hint">没有找到匹配的文章</p>

    <div v-if="results.length" class="results">
      <div v-for="r in results" :key="r.slug" class="result">
        <RouterLink :to="`/posts/${r.slug}`" class="result-title">{{ r.title }}</RouterLink>
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
  </div>
</template>

<style scoped>
.search-page {
  max-width: 760px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 1.5rem;
}

.page-header h1 {
  margin: 0 0 0.25rem;
  font-size: 1.9rem;
}

.subtitle {
  margin: 0;
  color: var(--text-muted);
}

.search-box {
  margin-bottom: 1.5rem;
}

.search-input {
  width: 100%;
  padding: 0.7rem 1rem;
  font-size: 1rem;
  color: var(--text);
  background: var(--sidebar-bg);
  border: 1px solid var(--border);
  border-radius: 8px;
  outline: none;
}

.search-input:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px rgba(47, 129, 247, 0.15);
}

.hint {
  color: var(--text-muted);
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

.snippet :deep(mark) {
  background: #ffe58f;
  color: var(--text);
  border-radius: 3px;
  padding: 0 2px;
}

/* 手机端 */
@media (max-width: 768px) {
  .page-header h1 {
    font-size: 1.5rem;
  }

  .result {
    padding: 1rem 1.1rem;
  }
}
</style>
