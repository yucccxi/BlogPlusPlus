<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { RouterLink, RouterView } from 'vue-router'
import { adminStore, setAdminKey } from '@/admin'

const route = useRoute()

const keyInput = ref(adminStore.key)
const authorized = ref(false)
const verifying = ref(false)
const error = ref('')

async function verify() {
  if (!keyInput.value.trim()) {
    error.value = '请输入访问凭证'
    return
  }
  error.value = ''
  verifying.value = true
  try {
    const res = await fetch('/api/admin/verify', {
      headers: { 'X-Admin-Key': keyInput.value },
    })
    if (res.ok) {
      setAdminKey(keyInput.value)
      authorized.value = true
    } else if (res.status === 401) {
      error.value = '凭证无效'
    } else {
      error.value = '验证失败'
    }
  } catch {
    error.value = '无法连接服务器'
  } finally {
    verifying.value = false
  }
}

onMounted(() => {
  if (adminStore.key) {
    verify()
  }
})
</script>

<template>
  <!-- 未通过凭证校验时，渲染输入凭证界面 -->
  <div v-if="!authorized" class="gate">
    <div class="gate-card">
      <h1 class="gate-title">Blog++ 后台</h1>
      <p class="gate-sub">请输入访问凭证以继续</p>
      <input
        v-model="keyInput"
        type="password"
        placeholder="blog.admin.key"
        class="gate-input"
        @keyup.enter="verify"
      />
      <p v-if="error" class="gate-error">{{ error }}</p>
      <button type="button" class="gate-btn" :disabled="verifying" @click="verify">
        {{ verifying ? '验证中...' : '进入后台' }}
      </button>
    </div>
  </div>

  <!-- 通过校验后，渲染后台布局 -->
  <div v-else class="layout">
    <aside class="sidebar">
      <div class="brand">
        <span class="brand-text">Blog++ 后台</span>
      </div>

      <nav class="nav">
        <RouterLink
          to="/console"
          class="nav-link"
          :class="{ active: route.name === 'console-overview' }"
        >
          概览
        </RouterLink>
        <RouterLink
          to="/console/new"
          class="nav-link"
          :class="{ active: ['console-new', 'console-edit'].includes(route.name) }"
        >
          写文章
        </RouterLink>
      </nav>

      <div class="sidebar-footer">
        <RouterLink to="/" class="back-link">返回首页</RouterLink>
      </div>
    </aside>

    <main class="content">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
/* 凭证门禁 */
.gate {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem;
}

.gate-card {
  width: 100%;
  max-width: 360px;
  background: var(--sidebar-bg);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 2rem;
}

.gate-title {
  margin: 0 0 0.25rem;
  font-size: 1.5rem;
}

.gate-sub {
  margin: 0 0 1.25rem;
  color: var(--text-muted);
  font-size: 0.9rem;
}

.gate-input {
  width: 100%;
  padding: 0.6rem 0.75rem;
  border: 1px solid var(--border);
  border-radius: 6px;
  font-size: 0.95rem;
  font-family: inherit;
  background: #fff;
  color: var(--text);
}

.gate-error {
  margin: 0.75rem 0 0;
  color: #d1242f;
  font-size: 0.85rem;
}

.gate-btn {
  margin-top: 1rem;
  width: 100%;
  padding: 0.6rem 1rem;
  border: 1px solid var(--accent);
  border-radius: 6px;
  background: var(--accent);
  color: #fff;
  font-size: 0.95rem;
  cursor: pointer;
  transition: opacity 0.15s ease;
}

.gate-btn:hover {
  opacity: 0.9;
}

.gate-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 后台布局 */
.layout {
  display: flex;
  height: 100%;
}

.sidebar {
  width: var(--sidebar-width);
  flex-shrink: 0;
  background: var(--sidebar-bg);
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  padding: 1.5rem 1rem;
  position: sticky;
  top: 0;
  height: 100vh;
}

.brand {
  padding: 0.25rem 0.5rem 1.5rem;
  font-size: 1.35rem;
  font-weight: 700;
}

.brand-text {
  color: var(--text);
}

.nav {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.nav-link {
  display: block;
  padding: 0.5rem 0.75rem;
  border-radius: 6px;
  color: var(--text-muted);
  font-size: 0.95rem;
}

.nav-link:hover {
  background: var(--bg);
  color: var(--text);
  text-decoration: none;
}

.nav-link.active {
  background: var(--bg);
  color: var(--accent);
  font-weight: 600;
}

.sidebar-footer {
  margin-top: auto;
  padding: 0.5rem;
  font-size: 0.85rem;
}

.back-link {
  color: var(--text-muted);
}

.back-link:hover {
  color: var(--accent);
}

.content {
  flex: 1;
  min-width: 0;
  overflow-y: auto;
  padding: 2rem 3rem;
}
</style>
