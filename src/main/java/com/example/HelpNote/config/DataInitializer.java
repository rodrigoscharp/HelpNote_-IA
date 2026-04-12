package com.example.HelpNote.config;

import com.example.HelpNote.domain.User;
import com.example.HelpNote.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private static final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private final UserRepository userRepository;

    public DataInitializer(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {
        if (userRepository.findByEmail("admin@helpnote.com").isEmpty()) {
            User testUser = new User("Administrador", "admin@helpnote.com", passwordEncoder.encode("admin123"));
            userRepository.save(testUser);
            log.info("Usuário admin criado: admin@helpnote.com / admin123");
        }
    }
}
