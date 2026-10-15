# Blog++

一个极简的个人博客系统，前后端分离。后端使用 Spring Boot + JDBC，前端使用 Vue 3 + Vite，支持 Markdown 渲染、代码高亮、数学公式、标签与置顶等功能，并提供带凭证校验的后台管理。

## 技术栈

| 端 | 技术 |
|---|---|
| 后端 | Spring Boot 4.x、Spring JDBC、MySQL |
| 前端 | Vue 3、Vue Router、Vite |
| 渲染 | marked、highlight.js、KaTeX |
| 数据库 | MySQL |

## 功能

- 主页展示已发布文章，右侧「已固定」栏目展示置顶文章
- 搜索页 `/search`：全文搜索已发布文章正文，匹配关键词高亮，并展示发布时间与标签
- 文章详情页右侧目录（ToC），展示 h1-h3 标题并支持点击跳转
- Markdown 由前端渲染（支持代码高亮 + 复制、行内/块级公式）
  - 图片自动缩小展示，点击可放大查看
  - 过长的块级公式横向滚动
- 后台管理 `/console`：
  - 进入前需校验访问凭证（key）
  - 概览页分类展示已发布/未发布文章，支持编辑、删除、创建
  - 写文章页左右分栏（左侧 Markdown 编辑、右侧实时渲染），「设置」弹框编辑标题、摘要、标签、发布、固定；离开时若未保存会提示
  - 文件管理：上传/删除图片（图床）与普通文件，图片可通过 `/images/...` 在 Markdown 中引用；上传带进度条，单文件上限 128MB

## 目录结构

```
BlogPlus/
├── src/                          # 后端
│   └── main/java/me/iynotuo/blog/
│       ├── config/               # 静态资源映射（/images、/files）
│       ├── controller/           # REST 接口（公开 + 后台 + 文件）
│       ├── dao/                  # 数据访问层（JdbcClient）
│       └── entity/               # 实体类
├── frontend/                     # 前端
│   └── src/
│       ├── layouts/              # 布局
│       ├── views/                # 页面
│       ├── router/               # 路由
│       ├── admin.js              # 后台凭证状态
│       └── markdown.js           # Markdown 渲染工具
└── pom.xml
```

## 快速开始

### 1. 初始化数据库

创建 `blog` 数据库并建表：

```sql
CREATE DATABASE IF NOT EXISTS blog
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

USE blog;

CREATE TABLE posts (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    slug CHAR(8) CHARACTER SET ascii COLLATE ascii_bin NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    summary TEXT,
    content LONGTEXT NOT NULL,
    is_published BOOLEAN NOT NULL DEFAULT FALSE,
    is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
    tags VARCHAR(255) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    published_at DATETIME DEFAULT NULL,
    INDEX idx_published_at (is_published, published_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 2. 启动后端

数据库连接和管理端 key 通过启动参数提供：

```bash
./mvnw spring-boot:run \
  -Dspring-boot.run.arguments="
    --spring.datasource.url=jdbc:mysql://localhost:3306/blog?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
    --spring.datasource.username=你的用户名
    --spring.datasource.password=你的密码
    --blog.admin.key=你的后台凭证
    --blog.upload.dir=uploads
    --blog.beian.icp=xxxxx
    --blog.beian.icp-url=https://beian.miit.gov.cn/#/Integrated/index
    --blog.beian.gongan=xxxxx
    --blog.beian.gongan-url=https://beian.mps.gov.cn/#/query/webSearch?code=xxxxxx
  "
```

> 上传文件大小上限（默认 128MB）由 `application.properties` 中的 `spring.servlet.multipart.max-file-size` / `max-request-size` 控制。
>
> 备案信息（`blog.beian.*`）为可选参数，不配置时侧边栏底部不显示备案号；公安备案图标 `gongan.png` 放置在前端 `frontend/public/` 目录下。

后端默认运行在 `http://localhost:8080`。

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认运行在 `http://localhost`（80 端口），开发环境下 `/api` 请求会代理到 `http://localhost:8080`。

## 使用

- 主页：`http://localhost/`
- 后台：手动访问 `http://localhost/console`（不在导航栏中展示），输入与 `blog.admin.key` 相同的凭证进入

## 数据模型

`posts` 表字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `BIGINT UNSIGNED` | 自增主键 |
| `slug` | `CHAR(8)` | 8 位文章标识符，唯一 |
| `title` | `VARCHAR(255)` | 标题 |
| `summary` | `TEXT` | 摘要，可为空 |
| `content` | `LONGTEXT` | Markdown 正文 |
| `is_published` | `BOOLEAN` | 是否发布 |
| `is_pinned` | `BOOLEAN` | 是否固定 |
| `tags` | `VARCHAR(255)` | 标签，逗号分隔 |
| `created_at` | `DATETIME` | 创建时间 |
| `updated_at` | `DATETIME` | 最后修改时间 |
| `published_at` | `DATETIME` | 首次发布时间 |

## API 概览

公开接口：

- `GET /api/posts` — 已发布文章列表
- `GET /api/posts/pinned` — 已固定文章
- `GET /api/posts/{slug}` — 文章详情
- `GET /api/search?q=关键词` — 全文搜索，返回匹配文章及上下文片段（含高亮起止位置）
- `GET /api/site/beian` — 备案信息（ICP + 公安备案，未配置时字段为空）

后台接口（需 `X-Admin-Key` 请求头）：

- `GET /api/admin/verify` — 校验凭证
- `GET /api/admin/posts` — 全部文章
- `POST /api/admin/posts` — 新建文章
- `GET /api/admin/posts/{id}` — 读取文章
- `PUT /api/admin/posts/{id}` — 修改文章
- `DELETE /api/admin/posts/{id}` — 删除文章
- `GET /api/admin/files?type=image|file` — 列出文件
- `POST /api/admin/files?type=image|file` — 上传文件
- `DELETE /api/admin/files/{type}/{filename}` — 删除文件

静态资源（公开访问）：

- `/images/**` — 上传的图片（图床）
- `/files/**` — 上传的普通文件
