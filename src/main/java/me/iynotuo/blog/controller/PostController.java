package me.iynotuo.blog.controller;

import me.iynotuo.blog.dao.PostDao;
import me.iynotuo.blog.entity.Post;
import me.iynotuo.blog.service.MarkdownTextExtractor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private static final int MAX_SIZE = 100;

    private final PostDao postDao;

    public PostController(PostDao postDao) {
        this.postDao = postDao;
    }

    @GetMapping
    public PageResult list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(1, size), MAX_SIZE);
        int offset = (safePage - 1) * safeSize;
        List<PostSummary> items = postDao.findPublished(safeSize, offset).stream()
                .map(PostSummary::from)
                .toList();
        int total = postDao.countPublished();
        return new PageResult(items, total, safePage, safeSize);
    }

    @GetMapping("/pinned")
    public List<PostSummary> pinned() {
        return postDao.findPinned().stream().map(PostSummary::from).toList();
    }

    @GetMapping("/{slug}")
    public ResponseEntity<Post> getBySlug(@PathVariable String slug) {
        return postDao.findPublishedBySlug(slug)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    public record PageResult(List<PostSummary> items, int total, int page, int size) {}

    public record PostSummary(
            Long id,
            String slug,
            String title,
            String summary,
            boolean published,
            boolean pinned,
            List<String> tags,
            LocalDateTime createdAt,
            LocalDateTime publishedAt) {

        static PostSummary from(Post post) {
            return new PostSummary(
                    post.getId(),
                    post.getSlug(),
                    post.getTitle(),
                    MarkdownTextExtractor.summarize(post.getRawText(), 150),
                    post.isPublished(),
                    post.isPinned(),
                    post.getTags(),
                    post.getCreatedAt(),
                    post.getPublishedAt());
        }
    }
}
