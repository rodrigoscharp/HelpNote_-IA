package com.example.HelpNote.controller;

import com.example.HelpNote.domain.User;
import com.example.HelpNote.dto.ChangePasswordRequest;
import com.example.HelpNote.dto.LoginRequest;
import com.example.HelpNote.dto.RegisterRequest;
import com.example.HelpNote.dto.UpdateProfileRequest;
import com.example.HelpNote.dto.GoogleLoginRequest;
import com.example.HelpNote.repository.UserRepository;
import com.example.HelpNote.service.UserService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final UserRepository userRepository;

    @Value("${google.client.id}")
    private String googleClientId;

    public AuthController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        Optional<User> authenticatedUser = userService.authenticate(loginRequest.getEmail(), loginRequest.getPassword());

        if (authenticatedUser.isPresent()) {
            User user = authenticatedUser.get();
            HttpSession session = request.getSession(true);
            session.setAttribute("userId", user.getId());
            session.setAttribute("userName", user.getName());
            session.setAttribute("userEmail", user.getEmail());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Login realizado com sucesso.");
            response.put("userId", user.getId());
            response.put("userName", user.getName());
            response.put("planType", user.getPlanType().name());
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Email ou senha inválidos."));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        User newUser = userService.registerUser(
                registerRequest.getName(),
                registerRequest.getEmail(),
                registerRequest.getPassword()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Usuário registrado com sucesso.", "userId", newUser.getId()));
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody GoogleLoginRequest request, HttpServletRequest httpRequest) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(request.getCredential());
            if (idToken == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Token Google inválido."));
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            User user = userService.processGoogleLogin(payload.getEmail(), (String) payload.get("name"));

            HttpSession session = httpRequest.getSession(true);
            session.setAttribute("userId", user.getId());
            session.setAttribute("userName", user.getName());
            session.setAttribute("userEmail", user.getEmail());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Login realizado com sucesso.");
            response.put("userId", user.getId());
            response.put("userName", user.getName());
            response.put("planType", user.getPlanType().name());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erro no Google login: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erro ao processar login com Google."));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Não autenticado."));
        }

        Long userId = (Long) session.getAttribute("userId");
        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("userName", session.getAttribute("userName"));
        response.put("userEmail", session.getAttribute("userEmail"));

        userRepository.findById(userId).ifPresent(user ->
                response.put("planType", user.getPlanType().name())
        );
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @Valid @RequestBody UpdateProfileRequest profileRequest,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return unauthorized();

        User updated = userService.updateProfile(userId, profileRequest.getName());

        // Update session name
        HttpSession session = request.getSession(false);
        if (session != null) session.setAttribute("userName", updated.getName());

        return ResponseEntity.ok(Map.of("message", "Perfil atualizado com sucesso.", "name", updated.getName()));
    }

    @PatchMapping("/password")
    public ResponseEntity<?> changePassword(
            @Valid @RequestBody ChangePasswordRequest passwordRequest,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return unauthorized();

        userService.changePassword(userId, passwordRequest.getCurrentPassword(), passwordRequest.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Senha alterada com sucesso."));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        return ResponseEntity.ok(Map.of("message", "Logout realizado com sucesso."));
    }

    private Long getUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("userId") != null) {
            return (Long) session.getAttribute("userId");
        }
        return null;
    }

    private ResponseEntity<?> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Não autenticado."));
    }
}
