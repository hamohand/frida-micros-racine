package com.muhend.backendai.config.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.SecureRandom;
import java.util.Date;

@Slf4j
@Profile("!calc-only")
@Component
public class JwtUtils {

    /**
     * Ancienne valeur par défaut, publiée dans le dépôt : toutes les installations signaient avec
     * elle, et n'importe qui pouvait fabriquer un jeton Maître. Elle n'est jamais utilisée comme clé.
     */
    static final String ANCIENNE_CLE_PUBLIQUE = "frida_super_secret_key_which_must_be_at_least_256_bits_long";

    /** HS256 exige une clé d'au moins 256 bits. */
    private static final int OCTETS_MINIMUM = 32;

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${jwt.expirationMs:86400000}") // 24 heures par défaut
    private int jwtExpirationMs;

    private Key cle;

    /**
     * Clé propre au poste, lue dans JWT_SECRET (générée par l'installeur notaire). Absente : clé
     * aléatoire pour la durée de l'exécution. Les connexions ne survivent pas à un redémarrage, mais
     * aucun jeton ne peut être fabriqué avec une clé connue.
     */
    @PostConstruct
    void initialiserCle() {
        if (jwtSecret == null || jwtSecret.isBlank() || ANCIENNE_CLE_PUBLIQUE.equals(jwtSecret)) {
            byte[] aleatoire = new byte[64];
            new SecureRandom().nextBytes(aleatoire);
            cle = Keys.hmacShaKeyFor(aleatoire);
            log.warn("JWT_SECRET absent : clé de signature aléatoire pour cette exécution. "
                    + "Les connexions seront perdues au redémarrage ; définissez JWT_SECRET dans le .env.");
            return;
        }
        byte[] octets = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (octets.length < OCTETS_MINIMUM) {
            throw new IllegalStateException(
                    "JWT_SECRET trop court : " + OCTETS_MINIMUM + " caractères au minimum.");
        }
        cle = Keys.hmacShaKeyFor(octets);
    }

    public String generateJwtToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        String role = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst().orElse("ROLE_USER");

        return Jwts.builder()
                .setSubject((userPrincipal.getUsername()))
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(cle, SignatureAlgorithm.HS256)
                .compact();
    }

    public String getUserNameFromJwtToken(String token) {
        return Jwts.parserBuilder().setSigningKey(cle).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(cle).build().parseClaimsJws(authToken);
            return true;
        } catch (ExpiredJwtException e) {
            log.info("Jeton de connexion expiré : {}", e.getMessage());
        } catch (JwtException | IllegalArgumentException e) {
            // Signature d'une autre clé, jeton mal formé ou vide. Auparavant, une mauvaise
            // signature levait une exception au lieu de renvoyer false.
            log.warn("Jeton de connexion refusé : {}", e.getMessage());
        }
        return false;
    }
}
