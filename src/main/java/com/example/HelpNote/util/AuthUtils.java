package com.example.HelpNote.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

public final class AuthUtils {

    private AuthUtils() {}

    public static Long getUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("authenticatedUserId");
    }

    public static ResponseEntity<?> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Não autenticado."));
    }
}
