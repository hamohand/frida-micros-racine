package com.muhend.backendai.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Liens de téléchargement à usage unique, valables 60 secondes.
 *
 * Une sauvegarde contient les documents et peut peser plusieurs Go : le navigateur doit la
 * recevoir en flux en suivant un lien, ce qui ne permet pas d'envoyer le jeton de connexion.
 * Un compte Maître obtient donc ce lien par un appel authentifié, puis le navigateur le suit.
 */
@Profile("!calc-only")
@Service
public class TelechargementService {

    public enum Type { SAUVEGARDE, ARCHIVE }

    public record Demande(Type type, String nom) {
    }

    private record Jeton(Demande demande, Instant expiration) {
    }

    static final Duration VALIDITE = Duration.ofSeconds(60);

    private final SecureRandom aleatoire = new SecureRandom();
    private final Map<String, Jeton> jetons = new ConcurrentHashMap<>();
    private final Supplier<Instant> maintenant;

    public TelechargementService() {
        this(Instant::now);
    }

    TelechargementService(Supplier<Instant> maintenant) {
        this.maintenant = maintenant;
    }

    public String creer(Type type, String nom) {
        Instant instant = maintenant.get();
        jetons.values().removeIf(jeton -> instant.isAfter(jeton.expiration()));
        byte[] octets = new byte[32];
        aleatoire.nextBytes(octets);
        String jeton = Base64.getUrlEncoder().withoutPadding().encodeToString(octets);
        jetons.put(jeton, new Jeton(new Demande(type, nom), instant.plus(VALIDITE)));
        return jeton;
    }

    /** Le jeton est retiré dès sa première présentation, valide ou non. */
    public Optional<Demande> consommer(String jeton) {
        if (jeton == null) {
            return Optional.empty();
        }
        Jeton trouve = jetons.remove(jeton);
        if (trouve == null || maintenant.get().isAfter(trouve.expiration())) {
            return Optional.empty();
        }
        return Optional.of(trouve.demande());
    }
}
