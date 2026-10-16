package me.iynotuo.blog.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * 图床与文件管理 API，所有接口都需要 X-Admin-Key 请求头校验。
 * 图片上传到 /images，普通文件上传到 /files，均通过静态资源映射公开访问。
 */
@RestController
@RequestMapping("/api/admin/files")
public class FileController {

    @Value("${blog.admin.key}")
    private String adminKey;

    @Value("${blog.upload.dir:uploads}")
    private String uploadDir;

    private boolean authorized(String key) {
        return adminKey != null && !adminKey.isBlank() && adminKey.equals(key);
    }

    private ResponseEntity<?> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    private String subDir(String type) {
        return switch (type) {
            case "image" -> "images";
            case "file" -> "files";
            default -> throw new IllegalArgumentException("invalid type");
        };
    }

    private Path resolveDir(String type) {
        return Path.of(uploadDir).toAbsolutePath().normalize().resolve(subDir(type));
    }

    /**
     * 列出指定类型（image/file）下的所有文件。
     */
    @GetMapping
    public ResponseEntity<?> list(
            @RequestHeader(value = "X-Admin-Key", required = false) String key,
            @RequestParam(defaultValue = "image") String type) {
        if (!authorized(key)) {
            return unauthorized();
        }
        Path dir;
        try {
            dir = resolveDir(type);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        List<FileItem> items = new ArrayList<>();
        if (Files.isDirectory(dir)) {
            try (var stream = Files.list(dir)) {
                stream.filter(Files::isRegularFile)
                        .sorted(Comparator.comparingLong((Path p) -> p.toFile().lastModified()).reversed())
                        .forEach(p -> items.add(new FileItem(
                                p.getFileName().toString(),
                                "/" + subDir(type) + "/" + p.getFileName(),
                                p.toFile().length())));
            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }
        return ResponseEntity.ok(items);
    }

    /**
     * 上传文件，文件名使用随机 UUID 以避免冲突和路径穿越。
     */
    @PostMapping
    public ResponseEntity<?> upload(
            @RequestHeader(value = "X-Admin-Key", required = false) String key,
            @RequestParam String type,
            @RequestParam("file") MultipartFile file) {
        if (!authorized(key)) {
            return unauthorized();
        }
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        Path dir;
        try {
            dir = resolveDir(type);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        try {
            Files.createDirectories(dir);
            String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
            String ext = "";
            int dot = original.lastIndexOf('.');
            if (dot >= 0) {
                ext = original.substring(dot).toLowerCase();
            }
            String name = UUID.randomUUID().toString().replace("-", "") + ext;
            Path target = dir.resolve(name).normalize();
            if (!dir.equals(target.getParent())) {
                return ResponseEntity.badRequest().build();
            }
            file.transferTo(target);
            return ResponseEntity.ok(new FileItem(
                    name,
                    "/" + subDir(type) + "/" + name,
                    target.toFile().length()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 删除指定类型下的文件。
     */
    @DeleteMapping("/{type}/{filename}")
    public ResponseEntity<?> delete(
            @RequestHeader(value = "X-Admin-Key", required = false) String key,
            @PathVariable String type,
            @PathVariable String filename) {
        if (!authorized(key)) {
            return unauthorized();
        }
        Path dir;
        try {
            dir = resolveDir(type);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        try {
            Path target = dir.resolve(filename).normalize();
            if (!dir.equals(target.getParent())) {
                return ResponseEntity.badRequest().build();
            }
            Files.deleteIfExists(target);
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    public record FileItem(String name, String url, long size) {}
}
