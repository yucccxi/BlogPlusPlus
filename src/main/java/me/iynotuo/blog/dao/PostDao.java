package me.iynotuo.blog.dao;

import me.iynotuo.blog.entity.Post;
import me.iynotuo.blog.service.MarkdownTextExtractor;
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
            SELECT id, slug, title, content, raw_text, is_published, is_pinned, tags, created_at, updated_at, published_at
            FROM posts
            """;

    private final JdbcClient jdbcClient;
    private final MarkdownTextExtractor extractor;
    private final SearchCacheDao searchCacheDao;

    public PostDao(JdbcClient jdbcClient, MarkdownTextExtractor extractor, SearchCacheDao searchCacheDao) {
        this.jdbcClient = jdbcClient;
        this.extractor = extractor;
        this.searchCacheDao = searchCacheDao;
    }

    public long insert(Post post) {
        String slug = generateSlug();
        post.setSlug(slug);
        String rawText = extractor.toPlainText(post.getContent());
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcClient.sql("""
                        INSERT INTO posts (slug, title, content, raw_text, is_published, is_pinned, tags, published_at)
                        VALUES (:slug, :title, :content, :rawText, :published, :pinned, :tags, :publishedAt)
                        """)
                .param("slug", slug)
                .param("title", post.getTitle())
                .param("content", post.getContent())
                .param("rawText", rawText)
                .param("published", post.isPublished())
                .param("pinned", post.isPinned())
                .param("tags", joinTags(post.getTags()))
                .param("publishedAt", post.getPublishedAt())
                .update(keyHolder);
        Number key = keyHolder.getKey();
        long id = key == null ? 0L : key.longValue();
        if (id != 0L) {
            searchCacheDao.rebuild(id, rawText);
        }
        return id;
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

    public Optional<Post> findPublishedBySlug(String slug) {
        return jdbcClient.sql(BASE_SELECT + " WHERE slug = ? AND is_published = TRUE")
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

    public List<Post> findPublished(int limit, int offset) {
        return jdbcClient.sql(BASE_SELECT + " WHERE is_published = TRUE ORDER BY published_at DESC LIMIT ? OFFSET ?")
                .param(limit)
                .param(offset)
                .query(this::mapRow)
                .list();
    }

    public List<Post> findPublishedByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(ids.size(), "?"));
        return jdbcClient.sql(BASE_SELECT + " WHERE is_published = TRUE AND id IN (" + placeholders + ") ORDER BY published_at DESC")
                .params(ids.toArray())
                .query(this::mapRow)
                .list();
    }

    /**
     * 已发布文章的轻量元信息（不含 content/raw_text），用于标题匹配。
     */
    public List<Post> findPublishedMeta() {
        return jdbcClient.sql("""
                        SELECT id, slug, title, NULL AS content, NULL AS raw_text,
                               is_published, is_pinned, tags, created_at, updated_at, published_at
                        FROM posts
                        WHERE is_published = TRUE
                        ORDER BY published_at DESC
                        """)
                .query(this::mapRow)
                .list();
    }

    public int countPublished() {
        Integer count = jdbcClient.sql("SELECT COUNT(*) FROM posts WHERE is_published = TRUE")
                .query((rs, rowNum) -> rs.getInt(1))
                .single();
        return count == null ? 0 : count;
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
        String rawText = extractor.toPlainText(post.getContent());
        int rows = jdbcClient.sql("""
                        UPDATE posts
                        SET slug = :slug, title = :title,
                            content = :content, raw_text = :rawText, is_published = :published, is_pinned = :pinned,
                            tags = :tags, published_at = :publishedAt
                        WHERE id = :id
                        """)
                .param("slug", post.getSlug())
                .param("title", post.getTitle())
                .param("content", post.getContent())
                .param("rawText", rawText)
                .param("published", post.isPublished())
                .param("pinned", post.isPinned())
                .param("tags", joinTags(post.getTags()))
                .param("publishedAt", post.getPublishedAt())
                .param("id", post.getId())
                .update();
        if (rows > 0) {
            searchCacheDao.rebuild(post.getId(), rawText);
        }
        return rows;
    }

    /**
     * 仅更新 raw_text（用于 --force-cache 回填历史文章）。
     */
    public int updateRawText(long id, String rawText) {
        return jdbcClient.sql("UPDATE posts SET raw_text = ? WHERE id = ?")
                .param(rawText)
                .param(id)
                .update();
    }

    public int deleteById(long id) {
        searchCacheDao.delete(id);
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

    private Post mapRow(ResultSet rs, int rowNum) throws SQLException {
        Post post = new Post();
        post.setId(rs.getLong("id"));
        post.setSlug(rs.getString("slug"));
        post.setTitle(rs.getString("title"));
        post.setContent(rs.getString("content"));
        post.setRawText(rs.getString("raw_text"));
        post.setPublished(rs.getBoolean("is_published"));
        post.setPinned(rs.getBoolean("is_pinned"));
        post.setTags(parseTags(rs.getString("tags")));
        post.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        post.setUpdatedAt(rs.getObject("updated_at", LocalDateTime.class));
        post.setPublishedAt(rs.getObject("published_at", LocalDateTime.class));
        return post;
    }
}
