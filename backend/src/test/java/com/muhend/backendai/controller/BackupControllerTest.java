package com.muhend.backendai.controller;

import com.muhend.backendai.dto.BackupInfo;
import com.muhend.backendai.service.BackupService;
import com.muhend.backendai.service.TelechargementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Message affiché après une restauration. Le 2026-09-12, « Retour à l'état de la sauvegarde
 * frida_backup_20260912_154037 effectué. L'état précédent a été sauvegardé dans
 * frida_avant_restauration_20260912_154652 : restaurez-la pour annuler. » n'était pas compris.
 */
@ExtendWith(MockitoExtension.class)
class BackupControllerTest {

    private static final String SAUVEGARDE = "frida_backup_20260912_154037";
    private static final String SECURITE = "frida_avant_restauration_20260912_154652";

    @Mock
    private BackupService backupService;

    @Mock
    private TelechargementService telechargementService;

    private BackupController controleur;

    @BeforeEach
    void setUp() {
        controleur = new BackupController(backupService, telechargementService);
    }

    private static BackupInfo sauvegarde(String nom, String date) {
        return BackupInfo.builder().fileName(nom).createdAt(OffsetDateTime.parse(date)).build();
    }

    @Test
    void restauration_MessageEnClairAvecLesDates() throws Exception {
        when(backupService.restoreBackup(SAUVEGARDE)).thenReturn(SECURITE);
        when(backupService.informations(SAUVEGARDE)).thenReturn(sauvegarde(SAUVEGARDE, "2026-09-12T15:40:39+02:00"));
        when(backupService.informations(SECURITE)).thenReturn(sauvegarde(SECURITE, "2026-09-12T15:46:55+02:00"));

        ResponseEntity<?> reponse = controleur.restoreBackup(SAUVEGARDE);

        assertEquals(HttpStatus.OK, reponse.getStatusCode());
        Map<?, ?> corps = (Map<?, ?>) reponse.getBody();
        assertEquals("FRIDA est revenu à l'état du 12/09/2026 à 15:40. Ce que vous aviez fait depuis a été mis de côté "
                + "dans la sauvegarde du 12/09/2026 à 15:46, marquée « Avant restauration ».", corps.get("message"));
        assertEquals(SECURITE, corps.get("sauvegardeDeSecurite"));
        assertEquals("12/09/2026 à 15:40", corps.get("dateSauvegarde"));
        assertEquals("12/09/2026 à 15:46", corps.get("dateSecurite"));
    }

    @Test
    void annulationDansLaMemeMinute_SecondesAffichees() throws Exception {
        // Constaté en test d'intégration : « revenu à l'état du 12/09/2026 à 16:25 ... mis de côté dans la
        // sauvegarde du 12/09/2026 à 16:25 » ne permet pas de distinguer les deux états
        String securiteSuivante = "frida_avant_restauration_20260912_154710";
        when(backupService.restoreBackup(SECURITE)).thenReturn(securiteSuivante);
        when(backupService.informations(SECURITE)).thenReturn(sauvegarde(SECURITE, "2026-09-12T15:46:55+02:00"));
        when(backupService.informations(securiteSuivante)).thenReturn(sauvegarde(securiteSuivante, "2026-09-12T15:47:10+02:00"));
        ResponseEntity<?> differentes = controleur.restoreBackup(SECURITE);
        assertEquals("12/09/2026 à 15:46", ((Map<?, ?>) differentes.getBody()).get("dateSauvegarde"),
                "minutes différentes : pas de secondes");

        String memeMinute = "frida_avant_restauration_20260912_154658";
        when(backupService.restoreBackup(SAUVEGARDE)).thenReturn(memeMinute);
        when(backupService.informations(SAUVEGARDE)).thenReturn(sauvegarde(SAUVEGARDE, "2026-09-12T15:46:20+02:00"));
        when(backupService.informations(memeMinute)).thenReturn(sauvegarde(memeMinute, "2026-09-12T15:46:58+02:00"));

        Map<?, ?> corps = (Map<?, ?>) controleur.restoreBackup(SAUVEGARDE).getBody();

        assertEquals("12/09/2026 à 15:46:20", corps.get("dateSauvegarde"));
        assertEquals("12/09/2026 à 15:46:58", corps.get("dateSecurite"));
        assertTrue(((String) corps.get("message")).contains("état du 12/09/2026 à 15:46:20"), (String) corps.get("message"));
    }

    @Test
    void restauration_DateIllisible_NomDeLaSauvegardeAffiche() throws Exception {
        when(backupService.restoreBackup(SAUVEGARDE)).thenReturn(SECURITE);
        when(backupService.informations(anyString())).thenThrow(new NoSuchElementException("illisible"));

        ResponseEntity<?> reponse = controleur.restoreBackup(SAUVEGARDE);

        assertEquals(HttpStatus.OK, reponse.getStatusCode(), "la restauration a eu lieu : pas d'erreur pour une date");
        String message = (String) ((Map<?, ?>) reponse.getBody()).get("message");
        assertTrue(message.contains(SAUVEGARDE) && message.contains(SECURITE), message);
    }
}
