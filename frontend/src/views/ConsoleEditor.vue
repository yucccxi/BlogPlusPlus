<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { authHeaders } from '@/admin'
import { renderMarkdown, onCodeCopy } from '@/markdown'

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => route.name === 'console-edit')

const form = ref({
  id: null,
  title: '',
  summary: '',
  content: '',
  published: false,
  pinned: false,
  tagsText: '',
})

const error = ref('')
const loading = ref(false)
const showSettings = ref(false)

// 已保存的表单快照，用于判断是否有未保存的更改
const savedForm = ref(null)

const dirty = computed(() => {
  if (!savedForm.value) return false
  const f = form.value
  const s = savedForm.value
  return (
    f.title !== s.title ||
    f.summary !== s.summary ||
    f.content !== s.content ||
    f.published !== s.published ||
    f.pinned !== s.pinned ||
    f.tagsText !== s.tagsText
  )
})

const previewHtml = computed(() => renderMarkdown(form.value.content))

async function loadPost(id) {
  loading.value = true
  try {
    const res = await fetch(`/api/admin/posts/${id}`, { headers: authHeaders() })
    if (res.status === 401) throw new Error('凭证无效')
    if (!res.ok) throw new Error('文章不存在')
    const p = await res.json()
    form.value = {
      id: p.id,
      title: p.title,
      summary: p.summary || '',
      content: p.content,
      published: p.published,
      pinned: p.pinned,
      tagsText: (p.tags || []).join(', '),
    }
    savedForm.value = { ...form.value }
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

async function save() {
  error.value = ''
  if (!form.value.title.trim() || !form.value.content.trim()) {
    error.value = '标题和内容不能为空'
    return
  }
  const body = {
    title: form.value.title,
    summary: form.value.summary,
    content: form.value.content,
    published: form.value.published,
    pinned: form.value.pinned,
    tags: form.value.tagsText
      .split(/[,，]/)
      .map((s) => s.trim())
      .filter(Boolean),
  }
  try {
    const res = await fetch(
      isEdit.value ? `/api/admin/posts/${form.value.id}` : '/api/admin/posts',
      {
        method: isEdit.value ? 'PUT' : 'POST',
        headers: authHeaders(),
        body: JSON.stringify(body),
      },
    )
    if (res.status === 401) throw new Error('凭证无效')
    if (!res.ok) throw new Error('保存失败')
    savedForm.value = { ...form.value }
    router.push('/console')
  } catch (e) {
    error.value = e.message
  }
}

function cancel() {
  router.push('/console')
}

onMounted(() => {
  if (isEdit.value) {
    loadPost(route.params.id)
  } else {
    savedForm.value = { ...form.value }
  }
})

// 离开页面时若有未保存的更改，提示用户
onBeforeRouteLeave(() => {
  if (dirty.value) {
    const ok = window.confirm('有未保存的更改，确定要离开吗？')
    if (!ok) return false
  }
})
</script>

<template>
  <div class="editor">
    <header class="page-header">
      <h1>{{ isEdit ? '编辑文章' : '写文章' }}</h1>
      <div class="header-actions">
        <button type="button" class="btn" @click="showSettings = true">设置</button>
        <button type="button" class="btn primary" @click="save">保存</button>
      </div>
    </header>

    <p v-if="error" class="hint error">{{ error }}</p>
    <p v-if="loading" class="hint">加载中...</p>

    <div v-else class="panes">
      <section class="pane pane-edit">
        <textarea
          v-model="form.content"
          class="markdown-input"
          placeholder="在此输入 Markdown 正文..."
          spellcheck="false"
        ></textarea>
      </section>

      <section class="pane pane-preview">
        <!-- eslint-disable-next-line vue/no-v-html -->
        <div class="markdown" v-html="previewHtml" @click="onCodeCopy"></div>
      </section>
    </div>

    <!-- 设置弹框 -->
    <div v-if="showSettings" class="modal-mask" @click.self="showSettings = false">
      <div class="modal">
        <h2 class="modal-title">文章设置</h2>

        <label class="field-label" for="title">标题</label>
        <input id="title" v-model="form.title" type="text" class="input" />

        <label class="field-label" for="summary">摘要</label>
        <input id="summary" v-model="form.summary" type="text" class="input" />

        <label class="field-label" for="tags">标签（逗号分隔）</label>
        <input id="tags" v-model="form.tagsText" type="text" class="input" placeholder="算法, 题解" />

        <div class="checks">
          <label class="check">
            <input v-model="form.published" type="checkbox" />
            发布
          </label>
          <label class="check">
            <input v-model="form.pinned" type="checkbox" />
            固定
          </label>
        </div>

        <div class="modal-actions">
          <button type="button" class="btn primary" @click="showSettings = false">确定</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.editor {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1rem;
}

.page-header h1 {
  margin: 0;
  font-size: 1.6rem;
}

.header-actions {
  display: flex;
  gap: 0.5rem;
}

.panes {
  display: flex;
  gap: 1rem;
  height: calc(100vh - 140px);
}

.pane {
  flex: 1;
  min-width: 0;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--sidebar-bg);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.pane-edit {
  padding: 0;
}

.pane-preview {
  overflow-y: auto;
  padding: 1.5rem;
}

.markdown-input {
  flex: 1;
  width: 100%;
  border: none;
  resize: none;
  padding: 1rem 1.25rem;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 0.9rem;
  line-height: 1.7;
  background: var(--sidebar-bg);
  color: var(--text);
  outline: none;
}

.hint {
  color: var(--text-muted);
  font-size: 0.9rem;
}

.hint.error {
  color: #d1242f;
}

/* markdown 预览排版 */
.markdown :deep(h1),
.markdown :deep(h2),
.markdown :deep(h3) {
  line-height: 1.35;
  margin-top: 1.5rem;
  margin-bottom: 0.75rem;
}

.markdown :deep(p) {
  margin: 0 0 1rem;
}

.markdown :deep(img) {
  max-width: 100%;
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

/* 弹框 */
.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.modal {
  width: 100%;
  max-width: 420px;
  background: var(--sidebar-bg);
  border-radius: 12px;
  padding: 1.5rem;
}

.modal-title {
  margin: 0 0 1rem;
  font-size: 1.1rem;
}

.field-label {
  display: block;
  margin: 0.75rem 0 0.35rem;
  font-size: 0.9rem;
  color: var(--text-muted);
}

.input {
  width: 100%;
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--border);
  border-radius: 6px;
  font-size: 0.95rem;
  font-family: inherit;
  background: #fff;
  color: var(--text);
}

.checks {
  display: flex;
  gap: 1.5rem;
  margin: 1rem 0;
}

.check {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.9rem;
  cursor: pointer;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
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
</style>
