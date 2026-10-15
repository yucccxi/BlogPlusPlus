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
  return marked.parse(text || '')
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
