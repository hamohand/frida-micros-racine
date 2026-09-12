package com.muhend.backendai.controller;

import com.muhend.backendai.dto.BackupInfo;
import com.muhend.backendai.service.BackupService;
import com.muhend.backendai.service.TelechargementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;

@Profile("!calc-only")
@RestController
@RequestMapping("/api/backups")
@RequiredArgsConstructor
@Slf4j
public class BackupController {

    static final String MESSAGE_LECTURE_SEULE = "Action désactivée sur la démonstration en ligne.";

    private static final DateTimeFormatter DATE_LISIBLE = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm", Locale.FRANCE);
    private static final DateTimeFormatter DATE_LISIBLE_SECONDES =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm:ss", Locale.FRANCE);

    private final BackupService backupService;
    private final TelechargementService telechargementService;

    /** Démo publique : sauvegardes et archives consultables, mais ni créées, ni restaurées, ni supprimées, ni téléchargées. */
    @Value("${app.sauvegardes.lecture-seule:false}")
    private boolean lectureSeule;

    @GetMapping
    public ResponseEntity<List<BackupInfo>> listBackups() {
        return ResponseEntity.ok(backupService.listBackups());
    }

    @PostMapping
    public ResponseEntity<?> createBackup() {
        if (lectureSeule) {
            return erreur(HttpStatus.FORBIDDEN, MESSAGE_LECTURE_SEULE);
        }
        try {
            return ResponseEntity.ok(backupService.createBackup(false));
        } catch (Exception e) {
            log.error("Échec de la sauvegarde", e);
            return erreur(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la sauvegarde : " + e.getMessage());
        }
    }

    @PostMapping("/{fileName}/restore")
    @PreAuthorize("hasRole('MAITRE')")
    public ResponseEntity<?> restoreBackup(@PathVariable String fileName) {
        if (lectureSeule) {
            return erreur(HttpStatus.FORBIDDEN, MESSAGE_LECTURE_SEULE);
        }
        try {
            String securite = backupService.restoreBackup(fileName);
            String[] dates = datesLisibles(fileName, securite);
            String dateSauvegarde = dates[0];
            String dateSecurite = dates[1];
            return ResponseEntity.ok(Map.of(
                    "message", "FRIDA est revenu à l'état du " + dateSauvegarde + ". Ce que vous aviez fait depuis "
                            + "a été mis de côté dans la sauvegarde du " + dateSecurite + ", marquée « Avant restauration ».",
                    "sauvegardeDeSecurite", securite,
                    "dateSauvegarde", dateSauvegarde,
                    "dateSecurite", dateSecurite));
        } catch (IllegalArgumentException e) {
            return erreur(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (NoSuchElementException e) {
            return erreur(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            log.error("Échec de la restauration de {}", fileName, e);
            return erreur(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la restauration : " + e.getMessage());
        }
    }

    @DeleteMapping("/{fileName}")
    @PreAuthorize("hasRole('MAITRE')")
    public ResponseEntity<?> deleteBackup(@PathVariable String fileName) {
        if (lectureSeule) {
            return erreur(HttpStatus.FORBIDDEN, MESSAGE_LECTURE_SEULE);
        }
        try {
            backupService.deleteBackup(fileName);
            return ResponseEntity.ok(Map.of("message", "Sauvegarde supprimée"));
        } catch (IllegalArgumentException e) {
            return erreur(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (NoSuchElementException e) {
            return erreur(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            log.error("Échec de la suppression de {}", fileName, e);
            return erreur(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la suppression : " + e.getMessage());
        }
    }

    /** Lien à usage unique : le navigateur télécharge ensuite la sauvegarde (.zip) en flux. */
    @PostMapping("/{fileName}/lien-telechargement")
    @PreAuthorize("hasRole('MAITRE')")
    public ResponseEntity<?> lienTelechargement(@PathVariable String fileName) {
        if (lectureSeule) {
            return erreur(HttpStatus.FORBIDDEN, MESSAGE_LECTURE_SEULE);
        }
        try {
            backupService.localiser(fileName);
        } catch (IllegalArgumentException e) {
            return erreur(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (NoSuchElementException e) {
            return erreur(HttpStatus.NOT_FOUND, e.getMessage());
        }
        String jeton = telechargementService.creer(TelechargementService.Type.SAUVEGARDE, fileName);
        return ResponseEntity.ok(Map.of("url", "/api/telechargements/" + jeton));
    }

    /**
     * Dates des deux sauvegardes telles que le notaire les lit (« 12/09/2026 à 15:40 »). Si elles tombent
     * dans la même minute, par exemple une annulation juste après une restauration, les secondes les
     * distinguent. Une date illisible est remplacée par le nom de la sauvegarde.
     */
    private String[] datesLisibles(String premiere, String seconde) {
        OffsetDateTime datePremiere = dateDe(premiere);
        OffsetDateTime dateSeconde = dateDe(seconde);
        boolean memeMinute = datePremiere != null && dateSeconde != null
                && datePremiere.format(DATE_LISIBLE).equals(dateSeconde.format(DATE_LISIBLE));
        DateTimeFormatter format = memeMinute ? DATE_LISIBLE_SECONDES : DATE_LISIBLE;
        return new String[]{
                datePremiere != null ? datePremiere.format(format) : premiere,
                dateSeconde != null ? dateSeconde.format(format) : seconde};
    }

    private OffsetDateTime dateDe(String nom) {
        try {
            return backupService.informations(nom).getCreatedAt();
        } catch (RuntimeException e) {
            return null;
        }
    }

    static ResponseEntity<Map<String, String>> erreur(HttpStatus statut, String message) {
        return ResponseEntity.status(statut).body(Map.of("message", message == null ? "Erreur inattendue" : message));
    }
}
