package com.example.funnyweb;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api")
public class PhotoController {
    private final Path uploadDir;

    public PhotoController(@Value("${app.upload-dir:./uploads}") String directory) throws IOException {
        uploadDir = Paths.get(directory).toAbsolutePath().normalize();
        Files.createDirectories(uploadDir);
    }

    // Public: everyone with the site link can view the gallery.
    @GetMapping("/photos")
    public List<String> listPhotos() throws IOException {
        try (Stream<Path> paths = Files.list(uploadDir)) {
            return paths.filter(Files::isRegularFile).filter(this::isAllowedImage)
                    .map(path -> "/api/photos/" + path.getFileName()).sorted().toList();
        }
    }

    // Private: only an authenticated owner session with a CSRF token can upload.
    @PostMapping("/photos")
    public ResponseEntity<?> upload(
            @RequestParam("files") List<MultipartFile> files,
            @RequestHeader(value = "X-Owner-CSRF", required = false) String csrf,
            HttpSession session) throws IOException {
        if (!AuthController.isOwner(session)) {
            return ResponseEntity.status(401).body(Map.of("error", "Chỉ chủ trang web mới được tải ảnh lên."));
        }
        if (!AuthController.hasValidCsrf(session, csrf)) {
            return ResponseEntity.status(403).body(Map.of("error", "Phiên tải ảnh không hợp lệ. Hãy đăng nhập lại."));
        }
        if (files == null || files.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Bạn chưa chọn ảnh."));
        }

        List<String> saved = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            String extension = extensionFor(file.getContentType());
            if (extension == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Chỉ chấp nhận ảnh JPEG, PNG, GIF hoặc WEBP."));
            }
            String filename = UUID.randomUUID() + extension; // Never trust client filenames.
            Path destination = uploadDir.resolve(filename).normalize();
            if (!destination.startsWith(uploadDir)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Tên tệp không hợp lệ."));
            }
            Files.copy(file.getInputStream(), destination);
            saved.add("/api/photos/" + filename);
        }
        return ResponseEntity.ok(Map.of("uploaded", saved.size(), "photos", saved));
    }

    // Public: images are intentionally readable by visitors.
    @GetMapping("/photos/{filename:.+}")
    public ResponseEntity<Resource> getPhoto(@PathVariable String filename) throws IOException {
        Path file = uploadDir.resolve(filename).normalize();
        if (!file.startsWith(uploadDir) || !Files.isRegularFile(file) || !isAllowedImage(file)) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new UrlResource(file.toUri());
        String detected = Files.probeContentType(file);
        MediaType type = MediaType.parseMediaType(detected == null ? "application/octet-stream" : detected);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFileName() + "\"")
                .contentType(type).body(resource);
    }

    private String extensionFor(String contentType) {
        if (contentType == null) return null;
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> null;
        };
    }

    private boolean isAllowedImage(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")
                || name.endsWith(".gif") || name.endsWith(".webp");
    }
}
