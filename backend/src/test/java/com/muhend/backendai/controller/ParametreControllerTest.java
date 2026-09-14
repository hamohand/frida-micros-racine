package com.muhend.backendai.controller;

import com.muhend.backendai.service.ParametreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

/**
 * Adresse réseau locale : saisie par le Maître pour que le QR code du scan NFC par smartphone
 * (nfc-scanner-modal) donne au téléphone une adresse joignable, au lieu de « localhost ».
 */
@ExtendWith(MockitoExtension.class)
class ParametreControllerTest {

    @Mock
    private ParametreService parametreService;

    private ParametreController controleur;

    @BeforeEach
    void setUp() {
        controleur = new ParametreController(parametreService);
        // lire() (appelé en fin de modifier()) assemble un Map.of(...), qui refuse toute valeur
        // nulle : sans ce repli, un mock non configuré pour ces méthodes ferait échouer le test
        // avec un NullPointerException sans rapport avec ce qui est vérifié.
        lenient().when(parametreService.getAdresseReseauLocale()).thenReturn("");
        lenient().when(parametreService.isVerificationPhonetiqueActive()).thenReturn(false);
    }

    @Test
    void lire_ReprendLesDeuxReglages() {
        when(parametreService.isVerificationPhonetiqueActive()).thenReturn(true);
        when(parametreService.getAdresseReseauLocale()).thenReturn("192.168.1.50");

        Map<String, Object> corps = controleur.lire();

        assertEquals(true, corps.get("verificationPhonetique"));
        assertEquals("192.168.1.50", corps.get("adresseReseauLocale"));
    }

    @Test
    void modifier_AdresseSeule_NeTouchePasLaVerificationPhonetique() {
        ResponseEntity<?> reponse = controleur.modifier(Map.of("adresseReseauLocale", "192.168.1.50"));

        assertEquals(HttpStatus.OK, reponse.getStatusCode());
        verify(parametreService).setAdresseReseauLocale("192.168.1.50");
        verify(parametreService, never()).setVerificationPhonetiqueActive(anyBoolean());
    }

    @Test
    void modifier_AdresseAvecEspaces_Nettoyee() {
        controleur.modifier(Map.of("adresseReseauLocale", "  192.168.1.50  "));

        verify(parametreService).setAdresseReseauLocale("192.168.1.50");
    }

    @Test
    void modifier_AdresseVide_Acceptee_PourEffacer() {
        ResponseEntity<?> reponse = controleur.modifier(Map.of("adresseReseauLocale", ""));

        assertEquals(HttpStatus.OK, reponse.getStatusCode());
        verify(parametreService).setAdresseReseauLocale("");
    }

    @Test
    void modifier_AdresseAvecSchema_Refusee() {
        ResponseEntity<?> reponse = controleur.modifier(Map.of("adresseReseauLocale", "http://192.168.1.50"));

        assertEquals(HttpStatus.BAD_REQUEST, reponse.getStatusCode());
        verify(parametreService, never()).setAdresseReseauLocale(any());
    }

    @Test
    void modifier_AdresseAvecPort_Refusee() {
        ResponseEntity<?> reponse = controleur.modifier(Map.of("adresseReseauLocale", "192.168.1.50:8080"));

        assertEquals(HttpStatus.BAD_REQUEST, reponse.getStatusCode());
    }

    @Test
    void modifier_AdresseAvecChemin_Refusee() {
        ResponseEntity<?> reponse = controleur.modifier(Map.of("adresseReseauLocale", "192.168.1.50/api"));

        assertEquals(HttpStatus.BAD_REQUEST, reponse.getStatusCode());
    }

    @Test
    void modifier_NomDePoste_Accepte() {
        ResponseEntity<?> reponse = controleur.modifier(Map.of("adresseReseauLocale", "poste-notaire-1"));

        assertEquals(HttpStatus.OK, reponse.getStatusCode());
        verify(parametreService).setAdresseReseauLocale("poste-notaire-1");
    }
}
