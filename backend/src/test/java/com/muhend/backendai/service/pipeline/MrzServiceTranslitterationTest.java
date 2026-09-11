package com.muhend.backendai.service.pipeline;

import com.muhend.backendai.service.pipeline.MrzService.PhoneticResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Une vérification phonétique impossible ne doit pas être prise pour une incohérence.
 * Constaté le 2026-09-11 : la route /api/translitteration/verifier n'existe pas dans
 * ocr-api, et chaque nom était signalé « Incohérence phonétique (0 %) ».
 */
@ExtendWith(MockitoExtension.class)
class MrzServiceTranslitterationTest {

    @Mock
    private RestTemplate restTemplate;

    private MrzService mrzService;

    @BeforeEach
    void setUp() {
        mrzService = new MrzService(restTemplate);
        ReflectionTestUtils.setField(mrzService, "ocrApiUrl", "http://ocr-api:8082");
    }

    @AfterEach
    void nettoyer() throws Exception {
        // verifierTranslitteration écrit un fichier de débogage dans le répertoire courant
        Files.deleteIfExists(Paths.get("phonetic_debug.txt"));
    }

    @Test
    void routeAbsente_ResultatIndisponible_EtNonIncoherent() {
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        PhoneticResult res = mrzService.verifierTranslitteration("أحمد", "AHMED");

        assertFalse(res.disponible, "un 404 ne permet aucune conclusion");
        assertNull(PhoneticResult.siDisponible(res));
    }

    @Test
    void nomVide_ResultatIndisponible() {
        PhoneticResult res = mrzService.verifierTranslitteration("", "AHMED");

        assertFalse(res.disponible);
        verifyNoInteractions(restTemplate);
    }

    @Test
    void reponseDuService_ResultatDisponible() {
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .thenReturn("{\"success\":true,\"match\":true,\"score\":92.0,"
                        + "\"arabe_translit\":\"ahmed\",\"latin_norm\":\"ahmed\"}");

        PhoneticResult res = mrzService.verifierTranslitteration("أحمد", "AHMED");

        assertTrue(res.disponible);
        assertTrue(res.match);
        assertSame(res, PhoneticResult.siDisponible(res));
    }
}
