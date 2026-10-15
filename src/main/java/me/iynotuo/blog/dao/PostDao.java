package me.iynotuo.blog.dao;

import me.iynotuo.blog.entity.Post;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.security.SecureRandom;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Repository
public class PostDao {

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final String BASE_SELECT = """
            SELECT id, slug, title, summary, content, is_published, is_pinned, tags, created_at, updated_at, published_at
            FROM posts
            """;

    private final JdbcClient jdbcClient;

    public PostDao(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public long insert(Post post) {
        String slug = generateSlug();
        post.setSlug(slug);
        String summary = post.getSummary() == null || post.getSummary().isBlank()
                ? summarize(post.getContent())
                : post.getSummary();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcClient.sql("""
                        INSERT INTO posts (slug, title, summary, content, is_published, is_pinned, tags, published_at)
                        VALUES (:slug, :title, :summary, :content, :published, :pinned, :tags, :publishedAt)
                        """)
                .param("slug", slug)
                .param("title", post.getTitle())
                .param("summary", summary)
                .param("content", post.getContent())
                .param("published", post.isPublished())
                .param("pinned", post.isPinned())
                .param("tags", joinTags(post.getTags()))
                .param("publishedAt", post.getPublishedAt())
                .update(keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? 0L : key.longValue();
    }

    public Optional<Post> findById(long id) {
        return jdbcClient.sql(BASE_SELECT + " WHERE id = ?")
                .param(id)
                .query(this::mapRow)
                .optional();
    }

    public Optional<Post> findBySlug(String slug) {
        return jdbcClient.sql(BASE_SELECT + " WHERE slug = ?")
                .param(slug)
                .query(this::mapRow)
                .optional();
    }

    public List<Post> findAll() {
        return jdbcClient.sql(BASE_SELECT + " ORDER BY created_at DESC")
                .query(this::mapRow)
                .list();
    }

    public List<Post> findPublished() {
        return jdbcClient.sql(BASE_SELECT + " WHERE is_published = TRUE ORDER BY published_at DESC")
                .query(this::mapRow)
                .list();
    }

    public List<Post> findPinned() {
        return jdbcClient.sql(BASE_SELECT + " WHERE is_published = TRUE AND is_pinned = TRUE ORDER BY published_at DESC")
                .query(this::mapRow)
                .list();
    }

    /**
     * 更新文章，updated_at 由数据库的 ON UPDATE CURRENT_TIMESTAMP 自动维护。
     */
    public int update(Post post) {
        String summary = post.getSummary() == null || post.getSummary().isBlank()
                ? summarize(post.getContent())
                : post.getSummary();
        return jdbcClient.sql("""
                        UPDATE posts
                        SET slug = :slug, title = :title, summary = :summary,
                            content = :content, is_published = :published, is_pinned = :pinned,
                            tags = :tags, published_at = :publishedAt
                        WHERE id = :id
                        """)
                .param("slug", post.getSlug())
                .param("title", post.getTitle())
                .param("summary", summary)
                .param("content", post.getContent())
                .param("published", post.isPublished())
                .param("pinned", post.isPinned())
                .param("tags", joinTags(post.getTags()))
                .param("publishedAt", post.getPublishedAt())
                .param("id", post.getId())
                .update();
    }

    public int deleteById(long id) {
        return jdbcClient.sql("DELETE FROM posts WHERE id = ?")
                .param(id)
                .update();
    }

    /**
     * 生成 8 位小写十六进制随机 slug，并保证唯一。
     */
    private String generateSlug() {
        String slug;
        do {
            byte[] bytes = new byte[4];
            RANDOM.nextBytes(bytes);
            slug = HexFormat.of().formatHex(bytes);
        } while (findBySlug(slug).isPresent());
        return slug;
    }

    private static List<String> parseTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private static String joinTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "";
        }
        return String.join(",", tags);
    }

    /**
     * 从 Markdown 正文提取摘要：优先取 {@code <!-- more -->} 之前的内容，
     * 否则取正文开头去掉 Markdown 标记后的前 150 个字符。
     */
    private static String summarize(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        String text = content;
        int moreIdx = text.indexOf("<!-- more -->");
        if (moreIdx >= 0) {
            text = text.substring(0, moreIdx);
        }
        text = text
                .replaceAll("```[\\s\\S]*?```", " ")
                .replaceAll("`[^`]*`", " ")
                .replaceAll("!\\[[^\\]]*]\\([^)]*\\)", " ")
                .replaceAll("\\[[^\\]]*]\\([^)]*\\)", "$1")
                .replaceAll("(?m)^#{1,6}\\s*", "")
                .replaceAll("(?m)^\\s*[-*+]\\s+", "")
                .replaceAll("\\$\\$[\\s\\S]*?\\$\\$", " ")
                .replaceAll("\\$[^$\\n]+\\$", " ")
                .replaceAll("[*_>|~]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (text.isEmpty()) {
            return null;
        }
        return text.length() > 150 ? text.substring(0, 150).trim() + "…" : text;
    }

    private Post mapRow(ResultSet rs, int rowNum) throws SQLException {
        Post post = new Post();
        post.setId(rs.getLong("id"));
        post.setSlug(rs.getString("slug"));
        post.setTitle(rs.getString("title"));
        post.setSummary(rs.getString("summary"));
        post.setContent(rs.getString("content"));
        post.setPublished(rs.getBoolean("is_published"));
        post.setPinned(rs.getBoolean("is_pinned"));
        post.setTags(parseTags(rs.getString("tags")));
        post.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        post.setUpdatedAt(rs.getObject("updated_at", LocalDateTime.class));
        post.setPublishedAt(rs.getObject("published_at", LocalDateTime.class));
        return post;
    }
}
