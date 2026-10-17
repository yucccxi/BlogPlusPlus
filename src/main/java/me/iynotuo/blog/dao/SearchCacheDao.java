package me.iynotuo.blog.dao;

import me.iynotuo.blog.service.BigramUtil;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 全文搜索缓存：search_cache 表以 (bigram, post_id) 为联合主键，
 * 值为该 bigram 在文章 raw_text 中出现的位置列表（逗号分隔）。
 */
@Repository
public class SearchCacheDao {

    private static final int BATCH_SIZE = 250;

    private final JdbcClient jdbcClient;

    public SearchCacheDao(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    /**
     * 重建某篇文章的 bigram 缓存：先删除旧数据，再按 raw_text 批量写入。
     */
    @Transactional
    public void rebuild(long postId, String rawText) {
        delete(postId);
        List<Map.Entry<String, List<Integer>>> entries = new ArrayList<>(BigramUtil.index(rawText).entrySet());
        for (int start = 0; start < entries.size(); start += BATCH_SIZE) {
            int end = Math.min(start + BATCH_SIZE, entries.size());
            StringBuilder sql = new StringBuilder("INSERT INTO search_cache (post_id, bigram, positions) VALUES ");
            List<Object> params = new ArrayList<>();
            for (int i = start; i < end; i++) {
                if (i > start) {
                    sql.append(", ");
                }
                sql.append("(?, ?, ?)");
                Map.Entry<String, List<Integer>> e = entries.get(i);
                params.add(postId);
                params.add(e.getKey());
                params.add(joinPositions(e.getValue()));
            }
            jdbcClient.sql(sql.toString()).params(params.toArray()).update();
        }
    }

    public void delete(long postId) {
        jdbcClient.sql("DELETE FROM search_cache WHERE post_id = ?")
                .param(postId)
                .update();
    }

    public void clear() {
        jdbcClient.sql("DELETE FROM search_cache").update();
    }

    /**
     * 返回包含指定 bigram 的所有文章 id。
     */
    public Set<Long> findPostIds(String bigram) {
        return new HashSet<>(jdbcClient.sql("SELECT post_id FROM search_cache WHERE bigram = ?")
                .param(bigram)
                .query((rs, rowNum) -> rs.getLong("post_id"))
                .list());
    }

    /**
     * 返回某个 bigram 在某篇文章 raw_text 中出现的位置列表（升序）。
     */
    public List<Integer> findPositions(long postId, String bigram) {
        return jdbcClient.sql("SELECT positions FROM search_cache WHERE post_id = ? AND bigram = ?")
                .param(postId)
                .param(bigram)
                .query((rs, rowNum) -> rs.getString("positions"))
                .optional()
                .map(SearchCacheDao::parsePositions)
                .orElse(List.of());
    }

    private static String joinPositions(List<Integer> positions) {
        if (positions == null || positions.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < positions.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(positions.get(i));
        }
        return sb.toString();
    }

    private static List<Integer> parsePositions(String positions) {
        if (positions == null || positions.isBlank()) {
            return List.of();
        }
        List<Integer> result = new ArrayList<>();
        for (String part : positions.split(",")) {
            String s = part.trim();
            if (!s.isEmpty()) {
                result.add(Integer.parseInt(s));
            }
        }
        return result;
    }
}
