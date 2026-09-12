package com.muhend.backendai.controller;

import com.muhend.backendai.service.ArchiveService;
import com.muhend.backendai.service.BackupService;
import com.muhend.backendai.service.TelechargementService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Suit un lien de téléchargement créé par un compte Maître (voir {@link TelechargementService}).
 * Accessible sans jeton de connexion : le lien lui-même est la preuve, à usage unique.
 */
@Profile("!calc-only")
@RestController
@RequestMapping("/api/telechargements")
@RequiredArgsConstructor
public class TelechargementController {

    private static final MediaType TEXTE = new MediaType("text", "plain", StandardCharsets.UTF_8);

    private final TelechargementService telechargementService;
    private final BackupService backupService;
    private final ArchiveService archiveService;

    @GetMapping("/{jeton}")
    public ResponseEntity<String> telecharger(@PathVariable String jeton, HttpServletResponse reponse)
            throws IOException {
        Optional<TelechargementService.Demande> demande = telechargementService.consommer(jeton);
        if (demande.isEmpty()) {
            return ResponseEntity.status(HttpStatus.GONE).contentType(TEXTE)
                    .body("Lien de téléchargement expiré ou déjà utilisé. "
                            + "Relancez le téléchargement depuis la page Sauvegardes.");
        }
        String nom = demande.get().nom();
        if (demande.get().type() == TelechargementService.Type.SAUVEGARDE) {
            Path dossier;
            try {
                dossier = backupService.localiser(nom);
            } catch (IllegalArgumentException | NoSuchElementException e) {
                return introuvable(nom);
            }
            entetes(reponse, nom + ".zip");
            backupService.zipper(dossier, reponse.getOutputStream());
        } else {
            Path archive = archiveService.getArchiveFilePath(nom);
            if (!Files.isRegularFile(archive)) {
                return introuvable(nom);
            }
            entetes(reponse, nom);
            reponse.setContentLengthLong(Files.size(archive));
            Files.copy(archive, reponse.getOutputStream());
        }
        // Réponse déjà écrite en flux
        return null;
    }

    private static void entetes(HttpServletResponse reponse, String nomFichier) {
        reponse.setContentType("application/zip");
        reponse.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(nomFichier, StandardCharsets.UTF_8).build().toString());
    }

    private static ResponseEntity<String> introuvable(String nom) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(TEXTE).body("Fichier introuvable : " + nom);
    }
}
