package me.iynotuo.blog.controller;

import me.iynotuo.blog.dao.PostDao;
import me.iynotuo.blog.dao.SearchCacheDao;
import me.iynotuo.blog.entity.Post;
import me.iynotuo.blog.service.BigramUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 全文搜索：基于 raw_text 与 bigram 缓存，先通过关键词第一个 bigram 的倒排索引定位候选文章，
 * 再只在第一个 bigram 出现的位置附近校验完整关键词，并截取一个上下文片段返回。
 */
@RestController
@RequestMapping("/api/search")
public class SearchController {

    private static final int CONTEXT_BEFORE = 40;
    private static final int CONTEXT_AFTER = 60;

    private final PostDao postDao;
    private final SearchCacheDao searchCacheDao;

    public SearchController(PostDao postDao, SearchCacheDao searchCacheDao) {
        this.postDao = postDao;
        this.searchCacheDao = searchCacheDao;
    }

    @GetMapping
    public SearchResponse search(@RequestParam String q) {
        long start = System.nanoTime();

        String query = q == null ? "" : q.trim();
        if (query.isEmpty()) {
            return new SearchResponse(query, List.of(), elapsedMs(start));
        }
        String lowerQuery = query.toLowerCase(Locale.ROOT);
        List<String> bigrams = BigramUtil.bigrams(lowerQuery);
        // 搜索必须两个字符以上
        if (bigrams.isEmpty()) {
            return new SearchResponse(query, List.of(), elapsedMs(start));
        }
        String firstBigram = bigrams.getFirst();

        List<Result> results = new ArrayList<>();
        Set<Long> seenIds = new HashSet<>();

        List<Post> candidates = new ArrayList<>(resolveCandidates(firstBigram));
        for (Post post : postDao.findPublishedMeta()) {
            if (post.getTitle() != null && post.getTitle().toLowerCase(Locale.ROOT).contains(lowerQuery)) {
                candidates.add(post);
            }
        }

        for (Post post : candidates) {
            if (!seenIds.add(post.getId())) {
                continue;
            }
            String rawText = post.getRawText() == null ? "" : post.getRawText();
            Snippet snippet = buildSnippet(rawText, lowerQuery,
                    searchCacheDao.findPositions(post.getId(), firstBigram));
            boolean titleMatched = post.getTitle() != null
                    && post.getTitle().toLowerCase(Locale.ROOT).contains(lowerQuery);
            if (snippet != null || titleMatched) {
                results.add(new Result(post.getSlug(), post.getTitle(), post.getTags(),
                        post.getPublishedAt(), snippet == null ? List.of() : List.of(snippet)));
            }
        }

        results.sort((a, b) -> {
            LocalDateTime ta = a.publishedAt();
            LocalDateTime tb = b.publishedAt();
            if (ta == null && tb == null) {
                return 0;
            }
            if (ta == null) {
                return 1;
            }
            if (tb == null) {
                return -1;
            }
            return tb.compareTo(ta);
        });

        return new SearchResponse(query, results, elapsedMs(start));
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    /**
     * 用关键词第一个 bigram 的倒排索引筛选候选文章。
     */
    private List<Post> resolveCandidates(String firstBigram) {
        List<Long> ids = new ArrayList<>(searchCacheDao.findPostIds(firstBigram));
        if (ids.isEmpty()) {
            return List.of();
        }
        return postDao.findPublishedByIds(ids);
    }

    /**
     * 只在第一个 bigram 出现的位置附近校验完整关键词，命中则返回一个上下文片段（否则为 null）。
     */
    static Snippet buildSnippet(String text, String lowerQuery, List<Integer> positions) {
        String lowerText = text.toLowerCase(Locale.ROOT);
        int qLen = lowerQuery.length();
        if (qLen == 0) {
            return null;
        }
        for (int pos : positions) {
            if (pos < 0 || pos + qLen > lowerText.length()) {
                continue;
            }
            // 第一个 bigram 的位置即完整关键词可能出现的起始位置
            if (!lowerText.startsWith(lowerQuery, pos)) {
                continue;
            }
            int start = Math.max(0, pos - CONTEXT_BEFORE);
            int end = Math.min(text.length(), pos + qLen + CONTEXT_AFTER);

            StringBuilder sb = new StringBuilder();
            if (start > 0) {
                sb.append('…');
            }
            int offset = sb.length();
            sb.append(text, start, end);
            if (end < text.length()) {
                sb.append('…');
            }
            Match match = new Match(offset + (pos - start), offset + (pos - start) + qLen);
            return new Snippet(sb.toString(), List.of(match));
        }
        return null;
    }

    public record Result(String slug, String title, List<String> tags,
                         LocalDateTime publishedAt, List<Snippet> snippets) {}

    public record SearchResponse(String query, List<Result> results, long elapsedMs) {}
    public record Snippet(String text, List<Match> matches) {}
    public record Match(int start, int end) {}
}
