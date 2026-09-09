package com.muhend.backendai.config;

import com.muhend.backendai.entities.UtilisateurEntity;
import com.muhend.backendai.repository.UtilisateurRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Profile("!calc-only")
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UtilisateurRepo utilisateurRepo;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:maitre}")
    private String adminUsername;

    /**
     * Vide par défaut : aucun mot de passe n'est codé en dur. L'installeur notaire en
     * génère un par poste et le passe via ADMIN_PASSWORD dans le .env. Si rien n'est
     * fourni, un mot de passe aléatoire est généré et journalisé au démarrage.
     */
    @Value("${app.admin.password:}")
    private String adminPassword;

    @Override
    public void run(String... args) throws Exception {
        if (utilisateurRepo.existsByUsername(adminUsername)) {
            log.info("Compte '{}' déjà présent, aucune initialisation nécessaire.", adminUsername);
            return;
        }

        boolean genere = (adminPassword == null || adminPassword.isBlank());
        String motDePasse = genere ? genererMotDePasse() : adminPassword;

        UtilisateurEntity maitre = new UtilisateurEntity();
        maitre.setUsername(adminUsername);
        maitre.setPassword(passwordEncoder.encode(motDePasse));
        maitre.setRole("ROLE_MAITRE");
        utilisateurRepo.save(maitre);

        if (genere) {
            log.warn("=================================================================");
            log.warn("  Compte '{}' créé avec un mot de passe GÉNÉRÉ :", adminUsername);
            log.warn("      {}", motDePasse);
            log.warn("  Notez-le : il n'est affiché qu'à cette création.");
            log.warn("  Pour en choisir un, définissez ADMIN_PASSWORD dans le .env.");
            log.warn("=================================================================");
        } else {
            log.info("Compte '{}' créé avec le mot de passe fourni par ADMIN_PASSWORD.", adminUsername);
        }
    }

    private String genererMotDePasse() {
        byte[] bytes = new byte[12];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
