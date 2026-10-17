<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { toast } from 'vue3-toastify'
import { adminStore, authHeaders } from '@/admin'

const router = useRouter()
const posts = ref([])
const loading = ref(true)

const publishedPosts = computed(() => posts.value.filter((p) => p.published))
const draftPosts = computed(() => posts.value.filter((p) => !p.published))

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

onMounted(load)
watch(() => adminStore.key, load)
</script>

<template>
  <div class="overview">
    <header class="page-header">
      <h1>概览</h1>
      <button type="button" class="btn primary" @click="createPost">创建文章</button>
    </header>

    <p v-if="!adminStore.key" class="hint">请先在上方输入访问凭证</p>
    <p v-else-if="loading" class="hint">加载中...</p>

    <template v-else>
      <section class="section">
        <h2 class="section-title">已发布（{{ publishedPosts.length }}）</h2>
        <p v-if="!publishedPosts.length" class="hint">暂无已发布文章</p>
        <ul v-else class="post-list">
          <li v-for="post in publishedPosts" :key="post.id" class="post-item">
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
      </section>

      <section class="section">
        <h2 class="section-title">未发布（{{ draftPosts.length }}）</h2>
        <p v-if="!draftPosts.length" class="hint">暂无未发布文章</p>
        <ul v-else class="post-list">
          <li v-for="post in draftPosts" :key="post.id" class="post-item">
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
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1.5rem;
}

.page-header h1 {
  margin: 0;
  font-size: 1.9rem;
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
