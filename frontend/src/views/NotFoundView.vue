<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="not-found">
    <p class="code">404</p>
    <h1 class="title">页面不存在</h1>
    <p class="desc">你访问的页面可能已被删除或从未存在。</p>
    <button type="button" class="btn" @click="router.push('/')">返回首页</button>
  </div>
</template>

<style scoped>
.not-found {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  gap: 0.5rem;
}

.code {
  margin: 0;
  font-size: 5rem;
  font-weight: 700;
  color: var(--accent);
  line-height: 1;
}

.title {
  margin: 0;
  font-size: 1.5rem;
}

.desc {
  margin: 0 0 1rem;
  color: var(--text-muted);
}

.btn {
  padding: 0.5rem 1.25rem;
  border: 1px solid var(--accent);
  border-radius: 6px;
  background: var(--accent);
  color: #fff;
  font-size: 0.9rem;
  cursor: pointer;
  transition: opacity 0.15s ease;
}

.btn:hover {
  opacity: 0.9;
}
</style>
