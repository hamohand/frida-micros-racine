package com.muhend.backendai.config.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Clé de signature propre au poste (JWT_SECRET). Jusqu'au 2026-09-12, toutes les installations
 * signaient avec une valeur par défaut publiée dans le dépôt.
 */
class JwtUtilsTest {

    private static final String CLE_DU_POSTE = "cle-de-test-propre-au-poste-0123456789-abcdefghij";

    private static JwtUtils jwtUtils(String secret) {
        JwtUtils utils = new JwtUtils();
        ReflectionTestUtils.setField(utils, "jwtSecret", secret);
        ReflectionTestUtils.setField(utils, "jwtExpirationMs", 60_000);
        utils.initialiserCle();
        return utils;
    }

    private static Authentication maitre() {
        User user = new User("maitre", "x", List.of(new SimpleGrantedAuthority("ROLE_MAITRE")));
        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }

    private static String jetonMaitreSigneAvec(String secret) {
        return Jwts.builder()
                .setSubject("maitre")
                .claim("role", "ROLE_MAITRE")
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }

    @Test
    void cleDuPoste_JetonToujoursValideApresRedemarrage() {
        String jeton = jwtUtils(CLE_DU_POSTE).generateJwtToken(maitre());

        JwtUtils apresRedemarrage = jwtUtils(CLE_DU_POSTE);

        assertTrue(apresRedemarrage.validateJwtToken(jeton));
        assertEquals("maitre", apresRedemarrage.getUserNameFromJwtToken(jeton));
    }

    @Test
    void jetonFabriqueAvecLaCleDuPoste_Accepte() {
        // Témoin : la fabrication de jeton du test est correcte, le refus ci-dessous n'est pas un faux négatif
        assertTrue(jwtUtils(CLE_DU_POSTE).validateJwtToken(jetonMaitreSigneAvec(CLE_DU_POSTE)));
    }

    @Test
    void jetonFabriqueAvecLAncienneClePublique_Refuse() {
        String forge = jetonMaitreSigneAvec(JwtUtils.ANCIENNE_CLE_PUBLIQUE);

        assertFalse(jwtUtils(CLE_DU_POSTE).validateJwtToken(forge));
        assertFalse(jwtUtils("").validateJwtToken(forge));
        assertFalse(jwtUtils(JwtUtils.ANCIENNE_CLE_PUBLIQUE).validateJwtToken(forge),
                "l'ancienne valeur par défaut n'est jamais utilisée comme clé");
    }

    @Test
    void cleDUnAutrePoste_Refusee() {
        String jeton = jwtUtils(CLE_DU_POSTE).generateJwtToken(maitre());

        assertFalse(jwtUtils(CLE_DU_POSTE + "-autre-poste").validateJwtToken(jeton));
    }

    @Test
    void sansCle_CleAleatoireLeTempsDeLExecution() {
        JwtUtils execution = jwtUtils(null);
        String jeton = execution.generateJwtToken(maitre());

        assertTrue(execution.validateJwtToken(jeton));
        assertFalse(jwtUtils(null).validateJwtToken(jeton), "une autre exécution a une autre clé");
    }

    @Test
    void cleTropCourte_DemarrageRefuse() {
        assertThrows(IllegalStateException.class, () -> jwtUtils("trop-courte"));
    }

    @Test
    void jetonMalForme_RefuseSansException() {
        JwtUtils utils = jwtUtils(CLE_DU_POSTE);

        assertFalse(utils.validateJwtToken("pas.un.jeton"));
        assertFalse(utils.validateJwtToken(""));
    }
}
