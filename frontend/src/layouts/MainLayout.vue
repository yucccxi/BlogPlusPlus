<script setup>
import { ref, onMounted } from 'vue'
import { RouterLink, RouterView } from 'vue-router'

const beian = ref(null)

onMounted(async () => {
  try {
    const res = await fetch('/api/site/beian')
    if (res.ok) beian.value = await res.json()
  } catch {
    beian.value = null
  }
})
</script>

<template>
  <div class="layout">
    <aside class="sidebar">
      <div class="brand">
        <RouterLink to="/" class="brand-link">Blog++</RouterLink>
      </div>

      <nav class="nav">
        <RouterLink to="/" class="nav-link" exact-active-class="active">首页</RouterLink>
        <RouterLink to="/search" class="nav-link" active-class="active">搜索</RouterLink>
      </nav>

      <div class="sidebar-footer">
        <p>By yuchenxi</p>
        <div v-if="beian && (beian.icp || beian.gongan)" class="footer-beian">
          <div v-if="beian.icp" class="beian-row">
            <a :href="beian.icpUrl || '#'" target="_blank" rel="noopener">{{ beian.icp }}</a>
          </div>
          <div v-if="beian.gongan" class="beian-row">
            <img class="gongan-icon" src="/gongan.png" alt="" />
            <a :href="beian.gonganUrl || '#'" target="_blank" rel="noopener">{{ beian.gongan }}</a>
          </div>
        </div>
      </div>
    </aside>

    <main class="content">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
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

.brand-link {
  color: var(--text);
}

.brand-link:hover {
  text-decoration: none;
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
  font-size: 0.8rem;
  color: var(--text-muted);
}

.footer-beian {
  margin: 0.75rem 0 0;
  font-size: 0.72rem;
  line-height: 1.5;
}

.beian-row {
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.beian-row + .beian-row {
  margin-top: 0.25rem;
}

.footer-beian a {
  color: var(--text-muted);
}

.footer-beian a:hover {
  color: var(--accent);
}

.gongan-icon {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
}

.content {
  flex: 1;
  min-width: 0;
  overflow-y: auto;
  padding: 2.5rem 3rem;
}
</style>
