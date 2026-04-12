package com.example.HelpNote.service;

import com.example.HelpNote.domain.User;
import com.example.HelpNote.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private static final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User registerUser(String name, String email, String password) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email já cadastrado.");
        }
        User newUser = new User(name, email, passwordEncoder.encode(password));
        log.info("Novo usuário registrado: {}", email);
        return userRepository.save(newUser);
    }

    @Transactional
    public Optional<User> authenticate(String email, String password) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return Optional.empty();

        User user = userOpt.get();
        boolean isBcrypt = user.getPassword().startsWith("$2a$") || user.getPassword().startsWith("$2b$");

        if (isBcrypt) {
            // Normal BCrypt check
            if (!passwordEncoder.matches(password, user.getPassword())) return Optional.empty();
        } else {
            // Legacy plain-text password — check and migrate on-the-fly
            if (!user.getPassword().equals(password)) return Optional.empty();
            user.setPassword(passwordEncoder.encode(password));
            userRepository.save(user);
            log.info("Senha migrada para BCrypt: {}", email);
        }
        return Optional.of(user);
    }

    @Transactional
    public User processGoogleLogin(String email, String name) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User(name, email, passwordEncoder.encode(UUID.randomUUID().toString()));
            log.info("Novo usuário via Google: {}", email);
            return userRepository.save(newUser);
        });
    }

    @Transactional
    public User updateProfile(Long userId, String name) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
        user.setName(name);
        log.info("Perfil atualizado para userId={}", userId);
        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Senha atual incorreta.");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Senha alterada para userId={}", userId);
    }
}
