package com.example.HelpNote.controller;

import com.example.HelpNote.domain.User;
import com.example.HelpNote.dto.ChangePasswordRequest;
import com.example.HelpNote.dto.LoginRequest;
import com.example.HelpNote.dto.RegisterRequest;
import com.example.HelpNote.dto.UpdateProfileRequest;
import com.example.HelpNote.dto.GoogleLoginRequest;
import com.example.HelpNote.repository.UserRepository;
import com.example.HelpNote.security.JwtService;
import com.example.HelpNote.service.UserService;
import com.example.HelpNote.util.AuthUtils;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.servlet.http.HttpServletRequest;
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
    private final JwtService jwtService;

    @Value("${google.client.id}")
    private String googleClientId;

    public AuthController(UserService userService, UserRepository userRepository, JwtService jwtService) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        Optional<User> authenticatedUser = userService.authenticate(loginRequest.getEmail(), loginRequest.getPassword());

        if (authenticatedUser.isPresent()) {
            User user = authenticatedUser.get();
            String token = jwtService.generateToken(user.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
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
        String token = jwtService.generateToken(newUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("token", token, "userId", newUser.getId(), "message", "Usuário registrado com sucesso."));
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody GoogleLoginRequest request) {
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
            String token = jwtService.generateToken(user.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
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
        Long userId = AuthUtils.getUserId(request);
        if (userId == null) return AuthUtils.unauthorized();

        return userRepository.findById(userId).map(user -> {
            Map<String, Object> response = new HashMap<>();
            response.put("userId", user.getId());
            response.put("userName", user.getName());
            response.put("userEmail", user.getEmail());
            response.put("planType", user.getPlanType().name());
            return ResponseEntity.ok(response);
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PatchMapping("/profile")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody UpdateProfileRequest profileRequest,
                                           HttpServletRequest request) {
        Long userId = AuthUtils.getUserId(request);
        if (userId == null) return AuthUtils.unauthorized();

        User updated = userService.updateProfile(userId, profileRequest.getName());
        return ResponseEntity.ok(Map.of("message", "Perfil atualizado com sucesso.", "name", updated.getName()));
    }

    @PatchMapping("/password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest passwordRequest,
                                            HttpServletRequest request) {
        Long userId = AuthUtils.getUserId(request);
        if (userId == null) return AuthUtils.unauthorized();

        userService.changePassword(userId, passwordRequest.getCurrentPassword(), passwordRequest.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Senha alterada com sucesso."));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok(Map.of("message", "Logout realizado com sucesso."));
    }
}
