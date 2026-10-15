<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { renderMarkdown, onMarkdownClick } from '@/markdown'

const route = useRoute()
const post = ref(null)
const error = ref('')
const loading = ref(true)

const rendered = computed(() =>
  post.value ? renderMarkdown(post.value.content) : { html: '', headings: [] },
)

onMounted(async () => {
  try {
    const res = await fetch(`/api/posts/${route.params.slug}`)
    if (!res.ok) throw new Error('文章不存在')
    post.value = await res.json()
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
})

function scrollToHeading(id) {
  const el = document.getElementById(id)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
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
</script>

<template>
  <div class="post-page" @click="onMarkdownClick">
    <p v-if="loading" class="hint">加载中...</p>
    <p v-else-if="error" class="hint error">{{ error }}</p>

    <div v-else-if="post" class="post-layout">
      <article class="post">
        <header class="post-header">
          <h1 class="post-title">{{ post.title }}</h1>
          <div class="post-meta">
            <time v-if="post.publishedAt">{{ formatDate(post.publishedAt) }}</time>
            <span v-else>未发布</span>
            <span v-if="post.tags && post.tags.length" class="tag-list">
              <span v-for="tag in post.tags" :key="tag" class="tag">{{ tag }}</span>
            </span>
          </div>
        </header>

        <div class="post-body">
          <!-- eslint-disable-next-line vue/no-v-html -->
          <div class="markdown" v-html="rendered.html"></div>
        </div>
      </article>

      <aside v-if="rendered.headings.length" class="toc">
        <h2 class="toc-title">目录</h2>
        <ul class="toc-list">
          <li
            v-for="h in rendered.headings"
            :key="h.id"
            :class="['toc-item', 'toc-level-' + h.level]"
          >
            <a :href="'#' + h.id" @click.prevent="scrollToHeading(h.id)">{{ h.text }}</a>
          </li>
        </ul>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.post-page {
  max-width: 1200px;
  margin: 0 auto;
}

.post-layout {
  display: flex;
  align-items: flex-start;
  gap: 2.5rem;
}

.post {
  flex: 1;
  min-width: 0;
}

.toc {
  width: 240px;
  flex-shrink: 0;
  position: sticky;
  top: 3rem;
  max-height: calc(100vh - 6rem);
  overflow-y: auto;
  border-left: 1px solid var(--border);
  padding: 1rem 0 1rem 1.25rem;
}

.toc-title {
  margin: 0 0 0.75rem;
  font-size: 0.85rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--text-muted);
}

.toc-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.toc-item a {
  display: block;
  color: var(--text-muted);
  font-size: 0.88rem;
  line-height: 1.5;
  padding: 0.15rem 0;
  text-decoration: none;
}

.toc-item a:hover {
  color: var(--accent);
}

.toc-level-2 a {
  font-weight: 600;
  color: var(--text);
}

.toc-level-3 a {
  padding-left: 1rem;
}

.hint {
  color: var(--text-muted);
}

.hint.error {
  color: #d1242f;
}

.post-header {
  margin-bottom: 2rem;
  padding-bottom: 1.5rem;
  border-bottom: 1px solid var(--border);
}

.post-title {
  margin: 0 0 0.75rem;
  font-size: 2rem;
  line-height: 1.3;
}

.post-meta {
  color: var(--text-muted);
  font-size: 0.9rem;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.tag-list {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 0.4rem;
}

.tag {
  font-size: 0.78rem;
  padding: 0.1rem 0.55rem;
  border-radius: 999px;
  background: #eef0f3;
  color: var(--text-muted);
}

/* markdown 渲染内容排版 */
.markdown :deep(h1),
.markdown :deep(h2),
.markdown :deep(h3) {
  line-height: 1.35;
  margin-top: 1.75rem;
  margin-bottom: 0.75rem;
  scroll-margin-top: 1rem;
}

.markdown :deep(p) {
  margin: 0 0 1rem;
}

.markdown :deep(img) {
  max-width: 100%;
  max-height: 420px;
  object-fit: contain;
  cursor: zoom-in;
  border-radius: 6px;
}

.markdown :deep(a) {
  color: var(--accent);
}

.markdown :deep(blockquote) {
  margin: 1rem 0;
  padding: 0.25rem 1rem;
  border-left: 4px solid var(--border);
  color: var(--text-muted);
}

.markdown :deep(:not(pre) > code) {
  background: #eef0f3;
  color: #d63384;
  padding: 0.15em 0.4em;
  border-radius: 4px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 0.88em;
}

/* 代码块 */
.markdown :deep(.code-block) {
  margin: 1.25rem 0;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid #30363d;
}

.markdown :deep(.code-header) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.4rem 0.9rem;
  background: #161b22;
  color: #8b949e;
  font-size: 0.78rem;
}

.markdown :deep(.code-lang) {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  text-transform: lowercase;
}

.markdown :deep(.code-copy) {
  background: transparent;
  border: 1px solid #30363d;
  color: #c9d1d9;
  border-radius: 5px;
  padding: 0.15rem 0.6rem;
  font-size: 0.75rem;
  cursor: pointer;
  transition: all 0.15s ease;
}

.markdown :deep(.code-copy:hover) {
  border-color: #58a6ff;
  color: #58a6ff;
}

.markdown :deep(pre) {
  margin: 0;
  padding: 1rem 1.1rem;
  background: #0d1117;
  overflow-x: auto;
  font-size: 0.9rem;
  line-height: 1.6;
}

.markdown :deep(pre code) {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 0.9rem;
}
</style>
