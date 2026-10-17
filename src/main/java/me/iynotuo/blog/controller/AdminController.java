package me.iynotuo.blog.controller;

import me.iynotuo.blog.dao.PostDao;
import me.iynotuo.blog.entity.Post;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 后台管理 API，所有接口都需要 X-Admin-Key 请求头校验。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final PostDao postDao;

    @Value("${blog.admin.key}")
    private String adminKey;

    public AdminController(PostDao postDao) {
        this.postDao = postDao;
    }

    private boolean authorized(String key) {
        return adminKey != null && !adminKey.isBlank() && adminKey.equals(key);
    }

    private ResponseEntity<?> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    /**
     * 校验访问凭证是否有效。
     */
    @GetMapping("/verify")
    public ResponseEntity<?> verify(
            @RequestHeader(value = "X-Admin-Key", required = false) String key) {
        if (!authorized(key)) {
            return unauthorized();
        }
        return ResponseEntity.ok().build();
    }

    /**
     * 列出所有文章（含未发布）。
     */
    @GetMapping("/posts")
    public ResponseEntity<?> list(
            @RequestHeader(value = "X-Admin-Key", required = false) String key) {
        if (!authorized(key)) {
            return unauthorized();
        }
        return ResponseEntity.ok(postDao.findAll());
    }

    /**
     * 新建文章，slug 由后端自动生成。
     */
    @PostMapping("/posts")
    public ResponseEntity<?> create(
            @RequestHeader(value = "X-Admin-Key", required = false) String key,
            @RequestBody PostRequest req) {
        if (!authorized(key)) {
            return unauthorized();
        }
        Post post = new Post();
        post.setTitle(req.title());
        post.setContent(req.content());
        post.setPublished(req.published());
        post.setPinned(req.pinned());
        post.setTags(req.tags());
        if (req.published()) {
            post.setPublishedAt(LocalDateTime.now());
        }
        long id = postDao.insert(post);
        post.setId(id);
        return ResponseEntity.ok(post);
    }

    /**
     * 按 id 读取单篇文章（含未发布）。
     */
    @GetMapping("/posts/{id}")
    public ResponseEntity<?> get(
            @RequestHeader(value = "X-Admin-Key", required = false) String key,
            @PathVariable long id) {
        if (!authorized(key)) {
            return unauthorized();
        }
        return postDao.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * 修改文章，slug 保持不变。
     */
    @PutMapping("/posts/{id}")
    public ResponseEntity<?> update(
            @RequestHeader(value = "X-Admin-Key", required = false) String key,
            @PathVariable long id,
            @RequestBody PostRequest req) {
        if (!authorized(key)) {
            return unauthorized();
        }
        Post existing = postDao.findById(id).orElse(null);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        existing.setTitle(req.title());
        existing.setContent(req.content());
        existing.setPublished(req.published());
        existing.setPinned(req.pinned());
        existing.setTags(req.tags());
        if (req.published() && existing.getPublishedAt() == null) {
            existing.setPublishedAt(LocalDateTime.now());
        } else if (!req.published()) {
            existing.setPublishedAt(null);
        }
        postDao.update(existing);
        return ResponseEntity.ok(existing);
    }

    /**
     * 删除文章。
     */
    @DeleteMapping("/posts/{id}")
    public ResponseEntity<?> delete(
            @RequestHeader(value = "X-Admin-Key", required = false) String key,
            @PathVariable long id) {
        if (!authorized(key)) {
            return unauthorized();
        }
        int affected = postDao.deleteById(id);
        if (affected == 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().build();
    }

    public record PostRequest(
            String title,
            String content,
            boolean published,
            boolean pinned,
            List<String> tags) {
    }
}
