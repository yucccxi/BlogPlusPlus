import { Marked } from 'marked'
import markedKatex from 'marked-katex-extension'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'
import 'katex/dist/katex.min.css'

function escapeHtml(s) {
  return s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

const marked = new Marked()

// 公式渲染：$...$ 行内公式，$$...$$ 块级公式
marked.use(markedKatex({ throwOnError: false, nonStandard: true }))

// 代码块渲染：语法高亮 + 语言标签 + 复制按钮
marked.use({
  useNewRenderer: true,
  renderer: {
    code(code, infoString, escaped) {
      if (typeof code === 'object') {
        escaped = code.escaped
        infoString = code.lang
        code = code.text
      }
      const lang = (infoString || '').trim()
      const language = hljs.getLanguage(lang) ? lang : ''
      const highlighted = language
        ? hljs.highlight(code.replace(/\n$/, ''), { language }).value
        : escapeHtml(code.replace(/\n$/, ''))
      const label = language
        ? `<span class="code-lang">${language}</span>`
        : '<span class="code-lang"></span>'
      return (
        '<div class="code-block">' +
        `<div class="code-header">${label}<button class="code-copy" type="button">复制</button></div>` +
        `<pre><code class="hljs${language ? ' language-' + language : ''}">${highlighted}</code></pre>` +
        '</div>'
      )
    },
  },
})

export function renderMarkdown(text) {
  const html = marked.parse(text || '')
  return buildWithHeadings(html)
}

function slugify(text) {
  const s = text
    .toLowerCase()
    .replace(/[^\w\u4e00-\u9fa5-]+/g, '-')
    .replace(/^-+|-+$/g, '')
  return s || 'section'
}

/**
 * 解析渲染后的 HTML，为 h1~h3 标题补充 id（用于生成目录），
 * 并给每个表格包一层 .table-wrapper 容器，便于在窄屏下横向滚动。
 */
function buildWithHeadings(html) {
  const doc = new DOMParser().parseFromString(html, 'text/html')
  const headings = []
  const used = new Map()

  doc.querySelectorAll('table').forEach((table) => {
    const wrapper = doc.createElement('div')
    wrapper.className = 'table-wrapper'
    table.parentNode.insertBefore(wrapper, table)
    wrapper.appendChild(table)
  })

  doc.querySelectorAll('h1, h2, h3').forEach((h) => {
    const text = (h.textContent || '').trim()
    if (!text) return
    const level = Number(h.tagName[1])
    let id = slugify(text)
    const count = used.get(id) || 0
    used.set(id, count + 1)
    if (count > 0) {
      id = `${id}-${count + 1}`
    }
    h.id = id
    headings.push({ id, text, level })
  })
  return { html: doc.body.innerHTML, headings }
}

// markdown 容器统一点击处理：代码复制 + 图片放大
export function onMarkdownClick(e) {
  onCodeCopy(e)
  onImageClick(e)
}

export async function onCodeCopy(e) {
  const btn = e.target.closest('.code-copy')
  if (!btn) return
  const code = btn.closest('.code-block')?.querySelector('code')?.textContent ?? ''
  try {
    await navigator.clipboard.writeText(code.replace(/\n$/, ''))
    btn.textContent = '已复制'
    setTimeout(() => {
      btn.textContent = '复制'
    }, 1500)
  } catch {
    btn.textContent = '复制失败'
    setTimeout(() => {
      btn.textContent = '复制'
    }, 1500)
  }
}

// 图片点击放大：在 markdown 容器上做事件委托
export function onImageClick(e) {
  const img = e.target.closest('img')
  if (!img) return
  openLightbox(img.src, img.alt || '')
}

function openLightbox(src, alt) {
  let overlay = document.getElementById('md-lightbox')
  if (!overlay) {
    overlay = document.createElement('div')
    overlay.id = 'md-lightbox'
    overlay.innerHTML = '<img alt="">'
    overlay.addEventListener('click', () => overlay.remove())
    document.body.appendChild(overlay)
  }
  const img = overlay.querySelector('img')
  img.src = src
  img.alt = alt
}
