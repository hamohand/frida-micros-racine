package com.muhend.backendai.controller;

import com.muhend.backendai.dto.ArchiveInfo;
import com.muhend.backendai.dto.FridaDetailsDTO;
import com.muhend.backendai.service.ArchiveService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/archives")
@RequiredArgsConstructor
public class ArchiveController {

    private final ArchiveService archiveService;

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
    public ResponseEntity<?> archiveFrida(@PathVariable String numFrida) {
        try {
            ArchiveInfo info = archiveService.archiveFrida(numFrida);
            return ResponseEntity.ok(info);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de l'archivage: " + e.getMessage()));
        }
    }

    /**
     * Restaure un dossier Frida depuis une archive vers la base active.
     */
    @PostMapping("/{fileName}/restore")
    public ResponseEntity<?> restoreFromArchive(@PathVariable String fileName) {
        try {
            archiveService.restoreFromArchive(fileName);
            return ResponseEntity.ok(Map.of("message",
                    "Dossier restauré avec succès dans la base active."));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de la restauration: " + e.getMessage()));
        }
    }

    /**
     * Supprime définitivement une archive.
     */
    @DeleteMapping("/{fileName}")
    public ResponseEntity<?> deleteArchive(@PathVariable String fileName) {
        try {
            archiveService.deleteArchive(fileName);
            return ResponseEntity.ok(Map.of("message", "Archive supprimée."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de la suppression: " + e.getMessage()));
        }
    }

    /**
     * Télécharge un fichier d'archive.
     */
    @GetMapping("/{fileName}/download")
    public ResponseEntity<Resource> downloadArchive(@PathVariable String fileName) {
        try {
            Path file = archiveService.getArchiveFilePath(fileName);
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + resource.getFilename() + "\"")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(resource);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Lance l'archivage automatique de tous les dossiers éligibles.
     */
    @PostMapping("/auto")
    public ResponseEntity<?> autoArchive() {
        try {
            int count = archiveService.autoArchive();
            return ResponseEntity.ok(Map.of(
                    "message", count + " dossier(s) archivé(s) avec succès.",
                    "count", count));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de l'archivage automatique: " + e.getMessage()));
        }
    }
}
