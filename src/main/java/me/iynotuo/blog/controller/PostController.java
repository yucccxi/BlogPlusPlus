package me.iynotuo.blog.controller;

import me.iynotuo.blog.dao.PostDao;
import me.iynotuo.blog.entity.Post;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostDao postDao;

    public PostController(PostDao postDao) {
        this.postDao = postDao;
    }

    @GetMapping
    public List<PostSummary> list() {
        return postDao.findPublished().stream().map(PostSummary::from).toList();
    }

    @GetMapping("/pinned")
    public List<PostSummary> pinned() {
        return postDao.findPinned().stream().map(PostSummary::from).toList();
    }

    @GetMapping("/{slug}")
    public ResponseEntity<Post> getBySlug(@PathVariable String slug) {
        return postDao.findBySlug(slug)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

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
                    post.getSummary(),
                    post.isPublished(),
                    post.isPinned(),
                    post.getTags(),
                    post.getCreatedAt(),
                    post.getPublishedAt());
        }
    }
}
