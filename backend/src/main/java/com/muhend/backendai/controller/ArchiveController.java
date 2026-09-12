package com.muhend.backendai.controller;

import com.muhend.backendai.dto.ArchiveInfo;
import com.muhend.backendai.dto.FridaDetailsDTO;
import com.muhend.backendai.service.ArchiveService;
import com.muhend.backendai.service.TelechargementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Profile("!calc-only")
@RestController
@RequestMapping("/api/archives")
@RequiredArgsConstructor
@Slf4j
public class ArchiveController {

    /** Noms produits par ArchiveService.archiveFrida : lettres latines ou arabes, chiffres, « _ », « - », « . ». */
    private static final Pattern NOM_ARCHIVE =
            Pattern.compile("[A-Za-z0-9\\u0600-\\u06FF][A-Za-z0-9_.\\-\\u0600-\\u06FF]*\\.zip");

    private final ArchiveService archiveService;
    private final TelechargementService telechargementService;

    @Value("${app.sauvegardes.lecture-seule:false}")
    private boolean lectureSeule;

    /**
     * Liste toutes les archives disponibles.
     */
    @GetMapping
    public ResponseEntity<List<ArchiveInfo>> listArchives() {
        return ResponseEntity.ok(archiveService.listArchives());
    }

    /**
     * Liste les dossiers Frida éligibles à l'archivage (anciens de + de X mois).
     */
    @GetMapping("/archivable")
    public ResponseEntity<List<FridaDetailsDTO>> getArchivableFridas() {
        return ResponseEntity.ok(archiveService.getArchivableFridas());
    }

    /**
     * Archive un dossier Frida spécifique (le retire de la base active).
     */
    @PostMapping("/{numFrida}")
    @PreAuthorize("hasRole('MAITRE')")
    public ResponseEntity<?> archiveFrida(@PathVariable String numFrida) {
        if (lectureSeule) {
            return BackupController.erreur(HttpStatus.FORBIDDEN, BackupController.MESSAGE_LECTURE_SEULE);
        }
        try {
            ArchiveInfo info = archiveService.archiveFrida(numFrida);
            return ResponseEntity.ok(info);
        } catch (IllegalArgumentException e) {
            return BackupController.erreur(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            log.error("Échec de l'archivage de {}", numFrida, e);
            return BackupController.erreur(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur lors de l'archivage : " + e.getMessage());
        }
    }

    /**
     * Restaure un dossier Frida depuis une archive vers la base active.
     */
    @PostMapping("/{fileName}/restore")
    @PreAuthorize("hasRole('MAITRE')")
    public ResponseEntity<?> restoreFromArchive(@PathVariable String fileName) {
        ResponseEntity<?> refus = refus(fileName);
        if (refus != null) {
            return refus;
        }
        try {
            archiveService.restoreFromArchive(fileName);
            return ResponseEntity.ok(Map.of("message", "Dossier restauré avec succès dans la base active."));
        } catch (IllegalStateException e) {
            return BackupController.erreur(HttpStatus.CONFLICT, e.getMessage());
        } catch (IllegalArgumentException e) {
            return BackupController.erreur(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            log.error("Échec de la restauration de l'archive {}", fileName, e);
            return BackupController.erreur(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur lors de la restauration : " + e.getMessage());
        }
    }

    /**
     * Supprime définitivement une archive.
     */
    @DeleteMapping("/{fileName}")
    @PreAuthorize("hasRole('MAITRE')")
    public ResponseEntity<?> deleteArchive(@PathVariable String fileName) {
        ResponseEntity<?> refus = refus(fileName);
        if (refus != null) {
            return refus;
        }
        try {
            archiveService.deleteArchive(fileName);
            return ResponseEntity.ok(Map.of("message", "Archive supprimée."));
        } catch (Exception e) {
            log.error("Échec de la suppression de l'archive {}", fileName, e);
            return BackupController.erreur(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur lors de la suppression : " + e.getMessage());
        }
    }

    /**
     * Lien à usage unique pour télécharger une archive.
     */
    @PostMapping("/{fileName}/lien-telechargement")
    @PreAuthorize("hasRole('MAITRE')")
    public ResponseEntity<?> lienTelechargement(@PathVariable String fileName) {
        ResponseEntity<?> refus = refus(fileName);
        if (refus != null) {
            return refus;
        }
        if (!Files.isRegularFile(archiveService.getArchiveFilePath(fileName))) {
            return BackupController.erreur(HttpStatus.NOT_FOUND, "Archive introuvable : " + fileName);
        }
        String jeton = telechargementService.creer(TelechargementService.Type.ARCHIVE, fileName);
        return ResponseEntity.ok(Map.of("url", "/api/telechargements/" + jeton));
    }

    /**
     * Lance l'archivage de tous les dossiers éligibles (action volontaire du Maître).
     */
    @PostMapping("/auto")
    @PreAuthorize("hasRole('MAITRE')")
    public ResponseEntity<?> autoArchive() {
        if (lectureSeule) {
            return BackupController.erreur(HttpStatus.FORBIDDEN, BackupController.MESSAGE_LECTURE_SEULE);
        }
        try {
            int count = archiveService.autoArchive();
            return ResponseEntity.ok(Map.of(
                    "message", count + " dossier(s) archivé(s) avec succès.",
                    "count", count));
        } catch (Exception e) {
            log.error("Échec de l'archivage automatique", e);
            return BackupController.erreur(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur lors de l'archivage automatique : " + e.getMessage());
        }
    }

    /** Refus (lecture seule ou nom invalide), ou null si l'action peut continuer. */
    private ResponseEntity<?> refus(String fileName) {
        if (lectureSeule) {
            return BackupController.erreur(HttpStatus.FORBIDDEN, BackupController.MESSAGE_LECTURE_SEULE);
        }
        if (fileName == null || !NOM_ARCHIVE.matcher(fileName).matches()) {
            return BackupController.erreur(HttpStatus.BAD_REQUEST, "Nom d'archive invalide : " + fileName);
        }
        return null;
    }
}
