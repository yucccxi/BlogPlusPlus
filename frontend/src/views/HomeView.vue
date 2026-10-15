<script setup>
import { ref, onMounted } from 'vue'

const posts = ref([])
const pinned = ref([])
const error = ref('')
const loading = ref(true)

onMounted(async () => {
  try {
    const [postsRes, pinnedRes] = await Promise.all([
      fetch('/api/posts'),
      fetch('/api/posts/pinned'),
    ])
    if (!postsRes.ok) throw new Error('加载文章失败')
    posts.value = await postsRes.json()
    if (pinnedRes.ok) pinned.value = await pinnedRes.json()
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
})

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

      <p v-if="loading" class="hint">加载中...</p>
      <p v-else-if="error" class="hint error">{{ error }}</p>
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
    </div>

    <aside v-if="pinned.length" class="side-col">
      <h2 class="side-title">已固定</h2>
      <ul class="pinned-list">
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

.hint.error {
  color: #d1242f;
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
