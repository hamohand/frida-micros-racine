package com.muhend.backendai.service;

import com.muhend.backendai.service.TelechargementService.Demande;
import com.muhend.backendai.service.TelechargementService.Type;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TelechargementServiceTest {

    private Instant maintenant = Instant.parse("2026-09-12T10:00:00Z");
    private final TelechargementService service = new TelechargementService(() -> maintenant);

    @Test
    void jeton_UsageUnique() {
        String jeton = service.creer(Type.SAUVEGARDE, "frida_backup_20260912_100000");

        assertEquals(Optional.of(new Demande(Type.SAUVEGARDE, "frida_backup_20260912_100000")), service.consommer(jeton));
        assertTrue(service.consommer(jeton).isEmpty(), "un lien ne sert qu'une fois");
    }

    @Test
    void jeton_ExpireApres60Secondes() {
        String jeton = service.creer(Type.ARCHIVE, "archive_1.zip");
        maintenant = maintenant.plusSeconds(61);

        assertTrue(service.consommer(jeton).isEmpty());
    }

    @Test
    void jetonInconnuOuAbsent_Refuse() {
        assertTrue(service.consommer("inconnu").isEmpty());
        assertTrue(service.consommer(null).isEmpty());
    }

    @Test
    void jetons_DistinctsEtLongs() {
        String premier = service.creer(Type.SAUVEGARDE, "a");
        String second = service.creer(Type.SAUVEGARDE, "a");

        assertNotEquals(premier, second);
        assertTrue(premier.length() >= 43, premier);
    }
}
