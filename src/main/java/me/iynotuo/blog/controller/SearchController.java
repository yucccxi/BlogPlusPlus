package me.iynotuo.blog.controller;

import me.iynotuo.blog.dao.PostDao;
import me.iynotuo.blog.entity.Post;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 全文搜索：将每篇文章的 Markdown 转为纯文本后做不区分大小写的子串匹配，
 * 并截取匹配处的上下文片段返回，供前端高亮展示。
 */
@RestController
@RequestMapping("/api/search")
public class SearchController {

    private static final int CONTEXT_BEFORE = 40;
    private static final int CONTEXT_AFTER = 60;
    private static final int MAX_SNIPPETS = 3;

    private final PostDao postDao;

    public SearchController(PostDao postDao) {
        this.postDao = postDao;
    }

    @GetMapping
    public SearchResponse search(@RequestParam String q) {
        String query = q == null ? "" : q.trim();
        if (query.isEmpty()) {
            return new SearchResponse(query, List.of());
        }
        String lowerQuery = query.toLowerCase(Locale.ROOT);

        List<Result> results = new ArrayList<>();
        for (Post post : postDao.findPublished()) {
            String text = toPlainText(post.getContent());
            List<Snippet> snippets = buildSnippets(text, lowerQuery);
            if (!snippets.isEmpty()) {
                results.add(new Result(post.getSlug(), post.getTitle(), post.getTags(),
                        post.getPublishedAt(), snippets));
            }
        }
        return new SearchResponse(query, results);
    }

    /**
     * 去掉 Markdown 语法，得到可用于全文搜索的纯文本。
     */
    static String toPlainText(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        String text = markdown;
        // 去掉代码块围栏，保留代码内容
        text = text.replaceAll("(?s)```[a-zA-Z0-9]*\\s*", " ");
        // 图片语法
        text = text.replaceAll("!\\[[^\\]]*]\\([^)]*\\)", " ");
        // 链接 [text](url) -> text
        text = text.replaceAll("\\[([^\\]]*)]\\([^)]*\\)", "$1");
        // 行内代码
        text = text.replaceAll("`([^`]*)`", "$1");
        // 标题
        text = text.replaceAll("(?m)^#{1,6}\\s*", "");
        // 列表项
        text = text.replaceAll("(?m)^\\s*[-*+]\\s+", "");
        text = text.replaceAll("(?m)^\\s*\\d+\\.\\s+", "");
        // 引用
        text = text.replaceAll("(?m)^\\s*>\\s?", "");
        // 块级公式
        text = text.replaceAll("(?s)\\$\\$.*?\\$\\$", " ");
        // 行内公式
        text = text.replaceAll("\\$[^$\\n]+\\$", " ");
        // 粗体 / 斜体 / 删除线
        text = text.replaceAll("[*_~]{1,3}", "");
        // HTML 标签
        text = text.replaceAll("<[^>]+>", " ");
        // 合并空白
        return text.replaceAll("\\s+", " ").trim();
    }

    /**
     * 在纯文本中查找关键词，将上下文窗口重叠的匹配合并为一个片段，
     * 并为每个片段记录关键词在片段内的起止位置。
     */
    static List<Snippet> buildSnippets(String text, String lowerQuery) {
        String lowerText = text.toLowerCase(Locale.ROOT);
        int qLen = lowerQuery.length();
        if (qLen == 0) {
            return List.of();
        }
        List<int[]> matches = new ArrayList<>();
        int idx = lowerText.indexOf(lowerQuery);
        while (idx >= 0) {
            matches.add(new int[]{idx, idx + qLen});
            idx = lowerText.indexOf(lowerQuery, idx + 1);
        }
        if (matches.isEmpty()) {
            return List.of();
        }

        List<List<int[]>> groups = new ArrayList<>();
        int windowEnd = -1;
        for (int[] m : matches) {
            int snippetStart = Math.max(0, m[0] - CONTEXT_BEFORE);
            if (groups.isEmpty() || snippetStart > windowEnd) {
                List<int[]> group = new ArrayList<>();
                group.add(m);
                groups.add(group);
                windowEnd = m[1] + CONTEXT_AFTER;
            } else {
                groups.get(groups.size() - 1).add(m);
                windowEnd = Math.max(windowEnd, m[1] + CONTEXT_AFTER);
            }
        }

        List<Snippet> snippets = new ArrayList<>();
        int limit = Math.min(groups.size(), MAX_SNIPPETS);
        for (int g = 0; g < limit; g++) {
            List<int[]> group = groups.get(g);
            int[] first = group.get(0);
            int[] last = group.get(group.size() - 1);
            int start = Math.max(0, first[0] - CONTEXT_BEFORE);
            int end = Math.min(text.length(), last[1] + CONTEXT_AFTER);

            StringBuilder sb = new StringBuilder();
            List<Match> snippetMatches = new ArrayList<>();
            if (start > 0) {
                sb.append('…');
            }
            int offset = sb.length();
            sb.append(text, start, end);
            if (end < text.length()) {
                sb.append('…');
            }
            for (int[] m : group) {
                snippetMatches.add(new Match(offset + (m[0] - start), offset + (m[1] - start)));
            }
            snippets.add(new Snippet(sb.toString(), snippetMatches));
        }
        return snippets;
    }

    public record SearchResponse(String query, List<Result> results) {
    }

    public record Result(String slug, String title, List<String> tags,
                         LocalDateTime publishedAt, List<Snippet> snippets) {
    }

    public record Snippet(String text, List<Match> matches) {
    }

    public record Match(int start, int end) {
    }
}
