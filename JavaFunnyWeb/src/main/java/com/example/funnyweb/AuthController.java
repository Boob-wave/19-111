package com.example.funnyweb;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class AuthController {
    private static final String OWNER_KEY = "OWNER_AUTHENTICATED";
    private static final String CSRF_KEY = "OWNER_CSRF_TOKEN";

    @Value("${app.owner-password:}")
    private String ownerPassword;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpSession session) {
        if (ownerPassword == null || ownerPassword.isBlank()) {
            return ResponseEntity.status(503).body(Map.of("error", "Chủ trang chưa cấu hình OWNER_PASSWORD trên máy chủ."));
        }
        if (request == null || request.password() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Vui lòng nhập mật khẩu."));
        }
        boolean matches = MessageDigest.isEqual(
                ownerPassword.getBytes(StandardCharsets.UTF_8),
                request.password().getBytes(StandardCharsets.UTF_8));
        if (!matches) {
            return ResponseEntity.status(401).body(Map.of("error", "Mật khẩu không đúng."));
        }
        session.setAttribute(OWNER_KEY, Boolean.TRUE);
        String csrf = UUID.randomUUID().toString();
        session.setAttribute(CSRF_KEY, csrf);
        return ResponseEntity.ok(Map.of("ok", true, "csrfToken", csrf));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @GetMapping("/me")
    public Map<String, Object> me(HttpSession session) {
        boolean owner = isOwner(session);
        return owner ? Map.of("owner", true, "csrfToken", session.getAttribute(CSRF_KEY))
                     : Map.of("owner", false);
    }

    public static boolean isOwner(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute(OWNER_KEY));
    }

    public static boolean hasValidCsrf(HttpSession session, String supplied) {
        Object expected = session.getAttribute(CSRF_KEY);
        return expected instanceof String token && supplied != null
                && MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),
                                         supplied.getBytes(StandardCharsets.UTF_8));
    }

    public record LoginRequest(String password) {}
}
