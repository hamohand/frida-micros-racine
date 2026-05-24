package com.muhend.backendai.config;

import com.muhend.backendai.entities.UtilisateurEntity;
import com.muhend.backendai.repository.UtilisateurRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UtilisateurRepo utilisateurRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (!utilisateurRepo.existsByUsername("maitre")) {
            log.info("Création du compte MAITRE par défaut...");
            UtilisateurEntity maitre = new UtilisateurEntity();
            maitre.setUsername("maitre");
            maitre.setPassword(passwordEncoder.encode("maitre123")); // Mot de passe par défaut
            maitre.setRole("ROLE_MAITRE");
            utilisateurRepo.save(maitre);
            log.info("Compte MAITRE créé (username: maitre, password: maitre123)");
        }
    }
}
