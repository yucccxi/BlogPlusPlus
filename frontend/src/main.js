import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import Vue3Toastify from 'vue3-toastify'
import 'vue3-toastify/dist/index.css'
import './assets/main.css'

// 从后端 /api/site/cdn 获取 CDN 地址（可在 config.yml 配置），动态加载 highlight.js 与 KaTeX
function loadScript(src) {
  return new Promise((resolve) => {
    if (!src) return resolve()
    const s = document.createElement('script')
    s.src = src
    s.onload = resolve
    s.onerror = resolve // 失败也继续，页面降级为纯文本
    document.head.appendChild(s)
  })
}

function loadCss(href) {
  if (!href) return
  const link = document.createElement('link')
  link.rel = 'stylesheet'
  link.href = href
  document.head.appendChild(link)
}

async function loadCdn() {
  try {
    const res = await fetch('/api/site/cdn')
    if (!res.ok) return
    const cdn = await res.json()
    loadCss(cdn.katexCss)
    loadCss(cdn.highlightCss)
    await loadScript(cdn.katexJs)
    await loadScript(cdn.highlightJs)
  } catch {
    // 获取失败则不加载 CDN，代码/公式降级为纯文本
  }
}

async function bootstrap() {
  await loadCdn()

  const app = createApp(App)
  app.use(router)
  app.use(Vue3Toastify, {
    autoClose: 3000,
    position: 'bottom-right',
  })
  app.mount('#app')
}

bootstrap()
