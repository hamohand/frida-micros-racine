package com.muhend.backendai.service.aibd;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FridaIdentifierServiceTest {

    private FridaIdentifierService service;

    @BeforeEach
    void setUp() {
        // Le service ne contient pas de dépendances complexes (Spring beans),
        // on peut l'instancier directement.
        service = new FridaIdentifierService();
    }

    @Test
    void genererIdentifiant_ShouldCleanDateAndAppendTimestamp() {
        String dateNaissanceMock = "01.01.1990";
        String idResultat = service.genererIdentifiant(dateNaissanceMock);

        assertNotNull(idResultat);
        // "01.01.1990" devient "01011990" (suppression des points)
        assertTrue(idResultat.startsWith("01011990"));
        // L'ID doit avoir la date (8 chars) + timestamp yyyyMMddHHmmss (14 chars) = 22 caractères
        assertEquals(22, idResultat.length());
    }

    @Test
    void genererIdentifiant_WithNullDate_ShouldOnlyReturnTimestamp() {
        String idResultat = service.genererIdentifiant(null);

        assertNotNull(idResultat);
        assertEquals(14, idResultat.length()); // Seulement le timestamp
    }

    @Test
    void genererIdentifiant_WithSlashesAndDashes_ShouldCleanAll() {
        String dateNaissanceMock = "01/01-1990";
        String idResultat = service.genererIdentifiant(dateNaissanceMock);

        assertTrue(idResultat.startsWith("01011990"));
    }
}
