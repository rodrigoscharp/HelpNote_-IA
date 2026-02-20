package com.example.HelpNote.config;

import com.example.HelpNote.domain.User;
import com.example.HelpNote.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;

    @Autowired
    public DataInitializer(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.findByEmail("admin@helpnote.com").isEmpty()) {
            User testUser = new User("Administrador", "admin@helpnote.com", "admin");
            userRepository.save(testUser);
            System.out.println("Test user created: admin@helpnote.com / admin");
        }
    }
}
