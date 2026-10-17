<script setup>
import { ref, onMounted } from 'vue'

const avatar = ref('')

onMounted(async () => {
  try {
    const res = await fetch('/api/site/about')
    if (res.ok) {
      const data = await res.json()
      avatar.value = data.avatar || ''
    }
  } catch {
    avatar.value = ''
  }
})

const repo = 'https://github.com/yucccxi/BlogPlusPlus'

// 经历时间线（占位内容，可自行修改）
const timeline = [
  {
    period: '2025 - 2029',
    title: '本科阶段',
    desc: [
      '计算机相关专业，系统学习数据结构、算法等课程。',
      '- 2026.06 蓝桥杯 B 组国一',
      '- 2026.04 蓝桥杯 B 组浙江省省一'
    ]
  },
  {
    period: '更早',
    title: '开智阶段',
    desc: [
      '为 Minecraft 编写简单的 Bukkit 插件和 Forge 模组。'
    ],
  },
]
</script>

<template>
  <div class="about">
    <header class="about-hero">
      <h1>关于</h1>
      <p class="subtitle">关于这个博客，也关于我</p>
    </header>

    <!-- 关于我 -->
    <section class="card">
      <div class="card-icon">👋</div>
      <h2 class="card-title">关于我</h2>

      <div class="me">
        <div class="avatar-wrap">
          <img v-if="avatar" :src="avatar" alt="头像" class="avatar" />
          <div v-else class="avatar avatar-fallback">Y</div>
        </div>
        <div class="me-intro">
          <p class="card-text">
            你好，我是 yuchenxi。
          </p>
          <p class="card-text">
            这里是我的个人博客，写一些技术笔记、题解和其它乱七八糟的东西。
          </p>
        </div>
      </div>

      <h3 class="sub-title">经历</h3>
      <ul class="timeline">
        <li v-for="item in timeline" :key="item.period" class="timeline-item">
          <div class="timeline-dot"></div>
          <div class="timeline-body">
            <span class="timeline-period">{{ item.period }}</span>
            <h4 class="timeline-title">{{ item.title }}</h4>
            <p v-for="(line, i) in item.desc" :key="i" class="timeline-desc">{{ line }}</p>
          </div>
        </li>
      </ul>

      <h3 class="sub-title">链接</h3>
      <div class="links">
        <h4 class="links-title">我的链接</h4>
        <a class="link-item" href="https://github.com/yucccxi" target="_blank" rel="noopener">
          <svg class="link-icon" viewBox="0 0 16 16" width="16" height="16" aria-hidden="true">
            <path
              fill="currentColor"
              d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27s1.36.09 2 .27c1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.01 8.01 0 0 0 16 8c0-4.42-3.58-8-8-8z"
            />
          </svg>
          <span>GitHub</span>
        </a>
        <a class="link-item" href="https://codeforces.com/profile/yuchenxi" target="_blank" rel="noopener">
          <svg class="link-icon" viewBox="0 0 24 24" width="16" height="16" aria-hidden="true">
            <path
              fill="currentColor"
              d="M4.5 7.5C5.328 7.5 6 8.172 6 9v10.5c0 .828-.672 1.5-1.5 1.5h-3C.673 21 0 20.328 0 19.5V9c0-.828.673-1.5 1.5-1.5h3zm9-4.5c.828 0 1.5.672 1.5 1.5v15c0 .828-.672 1.5-1.5 1.5h-3c-.827 0-1.5-.672-1.5-1.5v-15c0-.828.673-1.5 1.5-1.5h3zm9 7.5c.828 0 1.5.672 1.5 1.5v7.5c0 .828-.672 1.5-1.5 1.5h-3c-.828 0-1.5-.672-1.5-1.5V12c0-.828.672-1.5 1.5-1.5h3z"
            />
          </svg>
          <span>Codeforces</span>
        </a>
        <a class="link-item" href="mailto:crazymax1016@gmail.com">
          <svg class="link-icon" viewBox="0 0 16 16" width="16" height="16" aria-hidden="true">
            <path
              fill="currentColor"
              d="M0 3a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H2a2 2 0 0 1-2-2V3zm2-.5A.5.5 0 0 0 1.5 3v.217l6.5 4.5 6.5-4.5V3a.5.5 0 0 0-.5-.5H2zm12.5 2.3-4.68 3.24a1.5 1.5 0 0 1-1.64 0L3.5 4.8V13a.5.5 0 0 0 .5.5h10a.5.5 0 0 0 .5-.5V4.8z"
            />
          </svg>
          <span>crazymax1016@gmail.com</span>
        </a>

        <h4 class="links-title">其它链接</h4>
        <a class="link-item" href="https://www.cnblogs.com/kzssCCC" target="_blank" rel="noopener">
          <svg class="link-icon" viewBox="0 0 16 16" width="16" height="16" aria-hidden="true">
            <path
              fill="currentColor"
              d="M0 1.5A1.5 1.5 0 0 1 1.5 0h13A1.5 1.5 0 0 1 16 1.5v13a1.5 1.5 0 0 1-1.5 1.5h-13A1.5 1.5 0 0 1 0 14.5v-13zM3 3v2h2V3H3zm2 3v2h2V6H5zm-2 3v2h2V9H3zm7-5H6v2h2.5A.5.5 0 0 1 9 6.5V8h2V4.5A1.5 1.5 0 0 0 9.5 3H10v1h1v2H9V5H8v1h2V4.5z"
            />
          </svg>
          <span>kzss 的博客</span>
        </a>
      </div>
    </section>

    <!-- 关于这个博客 -->
    <section class="card">
      <div class="card-icon">📝</div>
      <h2 class="card-title">关于这个博客</h2>
      <p class="card-text">
        这是一个基于 <strong>Spring Boot + Vue 3</strong> 的个人博客，前后端分离，支持
        Markdown 渲染、代码高亮、数学公式、标签与全文搜索。所有内容由本人独立维护。
      </p>
      <p class="card-text">
        本项目<strong>完全开源</strong>，欢迎 Star、Fork 与提 Issue，一起把它变得更好。
      </p>
      <a class="repo-link" :href="repo" target="_blank" rel="noopener">
        <svg class="repo-icon" viewBox="0 0 16 16" width="16" height="16" aria-hidden="true">
          <path
            fill="currentColor"
            d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27s1.36.09 2 .27c1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.01 8.01 0 0 0 16 8c0-4.42-3.58-8-8-8z"
          />
        </svg>
        GitHub 仓库
        <span class="repo-url">{{ repo }}</span>
      </a>
    </section>
  </div>
</template>

<style scoped>
.about {
  max-width: 760px;
  margin: 0 auto;
}

.about-hero {
  margin-bottom: 2rem;
}

.about-hero h1 {
  margin: 0 0 0.25rem;
  font-size: 2rem;
}

.subtitle {
  margin: 0;
  color: var(--text-muted);
}

.card {
  position: relative;
  background: var(--sidebar-bg);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 1.75rem 2rem;
  margin-bottom: 1.5rem;
  overflow: hidden;
}

.card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, var(--accent), #7c3aed);
}

.card-icon {
  font-size: 1.4rem;
  margin-bottom: 0.25rem;
}

.card-title {
  margin: 0 0 0.75rem;
  font-size: 1.25rem;
}

.card-text {
  margin: 0 0 0.75rem;
  color: var(--text-muted);
  line-height: 1.7;
}

.card-text strong {
  color: var(--text);
}

.repo-link {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 0.25rem;
  padding: 0.5rem 1rem;
  border: 1px solid var(--border);
  border-radius: 8px;
  color: var(--text);
  font-size: 0.92rem;
  transition: all 0.15s ease;
}

.repo-link:hover {
  border-color: var(--accent);
  color: var(--accent);
  text-decoration: none;
}

.repo-icon {
  flex-shrink: 0;
}

.repo-url {
  color: var(--text-muted);
  font-size: 0.85rem;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
}

.me {
  display: flex;
  gap: 1.5rem;
  align-items: flex-start;
  margin-bottom: 1.5rem;
}

.avatar-wrap {
  flex-shrink: 0;
}

.avatar {
  width: 88px;
  height: 88px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid #fff;
  box-shadow: 0 0 0 2px var(--border), 0 4px 14px rgba(0, 0, 0, 0.1);
}

.avatar-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2rem;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, var(--accent), #7c3aed);
}

.me-intro {
  flex: 1;
  min-width: 0;
}

.me-intro .card-text:last-child {
  margin-bottom: 0;
}

.sub-title {
  margin: 0 0 1rem;
  font-size: 1rem;
  color: var(--text);
}

.sub-title:not(:first-of-type) {
  margin-top: 1.75rem;
}

.timeline {
  list-style: none;
  margin: 0;
  padding: 0;
  position: relative;
}

.timeline::before {
  content: '';
  position: absolute;
  left: 6px;
  top: 4px;
  bottom: 4px;
  width: 2px;
  background: var(--border);
}

.timeline-item {
  position: relative;
  padding-left: 1.75rem;
  padding-bottom: 1.5rem;
}

.timeline-item:last-child {
  padding-bottom: 0;
}

.timeline-dot {
  position: absolute;
  left: 0;
  top: 6px;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: #fff;
  border: 3px solid var(--accent);
}

.timeline-period {
  font-size: 0.78rem;
  color: var(--accent);
  font-weight: 600;
}

.timeline-title {
  margin: 0.1rem 0 0.25rem;
  font-size: 1rem;
}

.timeline-desc {
  margin: 0;
  color: var(--text-muted);
  font-size: 0.9rem;
  line-height: 1.6;
}

.links {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
}

.links-title {
  width: 100%;
  margin: 0;
  font-size: 0.85rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--text-muted);
}

.links-title:not(:first-child) {
  margin-top: 0.5rem;
}

.link-item {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.45rem 0.9rem;
  border: 1px solid var(--border);
  border-radius: 999px;
  color: var(--text);
  font-size: 0.9rem;
  transition: all 0.15s ease;
}

.link-item:hover {
  border-color: var(--accent);
  color: var(--accent);
  text-decoration: none;
}

.link-icon {
  flex-shrink: 0;
}

/* 手机端 */
@media (max-width: 768px) {
  .about-hero h1 {
    font-size: 1.5rem;
  }

  .card {
    padding: 1.25rem 1.1rem;
  }

  .me {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }

  .avatar {
    width: 96px;
    height: 96px;
  }

  .repo-url {
    display: none;
  }
}
</style>
