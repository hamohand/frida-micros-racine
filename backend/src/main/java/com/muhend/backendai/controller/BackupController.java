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

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Profile("!calc-only")
@RestController
@RequestMapping("/api/backups")
@RequiredArgsConstructor
@Slf4j
public class BackupController {

    static final String MESSAGE_LECTURE_SEULE = "Action désactivée sur la démonstration en ligne.";

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
            backupService.restoreBackup(fileName);
            return ResponseEntity.ok(Map.of("message",
                    "Sauvegarde " + fileName + " restaurée : base de données et documents."));
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

    static ResponseEntity<Map<String, String>> erreur(HttpStatus statut, String message) {
        return ResponseEntity.status(statut).body(Map.of("message", message == null ? "Erreur inattendue" : message));
    }
}
