<script setup>
import { RouterLink, RouterView } from 'vue-router'
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

.content {
  flex: 1;
  min-width: 0;
  overflow-y: auto;
  padding: 2.5rem 3rem;
}
</style>
