# Blog++

一个极简的个人博客系统，前后端分离。后端使用 Spring Boot + JDBC，前端使用 Vue 3 + Vite，支持 Markdown 渲染、代码高亮、数学公式、标签与置顶等功能，并提供带凭证校验的后台管理。

## 技术栈

| 端 | 技术 |
|---|---|
| 后端 | Spring Boot 4.x、Spring JDBC、MySQL |
| 前端 | Vue 3、Vue Router、Vite |
| 渲染 | marked、highlight.js、KaTeX |

## 功能

- 主页展示已发布文章，右侧「已固定」栏目展示置顶文章
- 搜索页 `/search`：全文搜索已发布文章正文，匹配关键词高亮
- 文章详情页右侧目录（ToC），展示 h1-h3 标题并支持点击跳转
- Markdown 由前端渲染（支持代码块、行内/块级公式）
- 后台管理 `/console`：
  - 进入前需校验访问凭证（key）
  - 文章管理：文章内容、属性管理，编写文章实时渲染 markdown（分屏预览）
  - 文件管理：上传/删除图片（图床）与普通文件，图片可通过 `/images/...` 在 Markdown 中引用

## 目录结构

```
BlogPlus/
├── src/                          # 后端
│   └── main/java/me/iynotuo/blog/
│       ├── config/               # 静态资源映射、全局异常处理
│       ├── controller/           # REST 接口（公开 + 后台 + 文件 + 搜索 + 站点配置）
│       ├── dao/                  # 数据访问层（JdbcClient）
│       ├── entity/               # 实体类
│       └── BlogPlusApplication.java  # 启动类
├── frontend/                     # 前端
│   ├── public/                   # 静态资源（favicon、公安备案图标等）
│   └── src/
│       ├── assets/               # 全局样式
│       ├── layouts/              # 布局
│       ├── views/                # 页面（主页/文章/搜索/关于/后台/404）
│       ├── router/               # 路由
│       ├── admin.js              # 后台凭证状态
│       ├── markdown.js           # Markdown 渲染工具
│       ├── App.vue               # 根组件
│       └── main.js               # 入口
└── pom.xml
```

## 快速开始

### 1. 初始化数据库

开发与部署都需要先创建 `blog` 数据库并建表：

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

## 开发

### 1. 启动后端

```bash
./mvnw spring-boot:run
```

后端默认运行在 `http://localhost:8080`。

### 2. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认运行在 `http://localhost`（80 端口），开发环境下 `/api`、`/images`、`/files` 请求会代理到 `http://localhost:8080`。

## 部署

### 1. 打包后端

```bash
./mvnw clean package -DskipTests
```

产物为 `target/BlogPlus-0.0.1-SNAPSHOT.jar`，运行：

```bash
java -jar target/BlogPlus-0.0.1-SNAPSHOT.jar
```

首次运行会在工作目录生成 `config.yml`，从 jar 内置模板生成，填写后重启生效：

```yaml
# config.yml
# ===== 数据库连接 =====
spring:
  datasource:
    url: "jdbc:mysql://localhost:3306/blog?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai"
    username: 你的用户名
    password: "你的密码"

# ===== 后台访问凭证 =====
blog:
  admin:
    key: "你的后台凭证"

  # ===== 上传目录 =====
  upload:
    dir: uploads

  # ===== 关于页头像（URL，留空则不显示）=====
  about:
    avatar: "/images/xxx.jpg"

  # ===== 备案信息（可选，留空则不显示）=====
  beian:
    icp: "浙ICP备xxxx号"
    icp-url: "https://beian.miit.gov.cn/#/Integrated/index"
    gongan: "浙公网安备xxxx号"
    gongan-url: "https://beian.mps.gov.cn/#/query/webSearch?code=xxxxxx"
```

### 2. 打包前端

```bash
cd frontend
npm install
npm run build
```

产物在 `frontend/dist/`，将整个目录交给静态服务器（如 Nginx）托管。

### 3. 配置反向代理（Nginx 示例）

前端 SPA 与后端 API 通过 Nginx 统一对外，`/api`、`/images`、`/files` 反向代理到后端 8080：

```nginx
server {
    listen 80;
    server_name 你的域名;

    # 前端静态资源
    root /path/to/frontend/dist;
    index index.html;

    # SPA 路由兜底
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 后端 API 与上传文件
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location /images/ {
        proxy_pass http://127.0.0.1:8080;
    }

    location /files/ {
        proxy_pass http://127.0.0.1:8080;
    }
}
```

> 上传文件大小上限（默认 128MB）由 `application.properties` 中的 `spring.servlet.multipart.max-file-size` / `max-request-size` 控制；若经 Nginx 转发，还需将 `client_max_body_size` 调到不小于该值。
>
> 备案信息（`blog.beian.*`）为可选配置，不配置时侧边栏底部不显示备案号。

## 使用

- 主页：`http://localhost/`
- 后台：手动访问 `http://localhost/console`，输入与 `blog.admin.key` 相同的凭证进入

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
