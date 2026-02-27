package com.muhend.backendai.service.dossier;

import com.muhend.backendai.config.exception.FolderCreationException;
import com.muhend.backendai.dto.dossier.CreateFolderRequest;
import com.muhend.backendai.dto.dossier.FolderResponse;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;

@Service
@Slf4j
public class FolderService {

    @Value("${ROOT_PATH}")
    private String rootPathString;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    @Getter
    public static Path folderPath;

    /**
     * Sous-dossiers créés automatiquement : chaque catégorie × chaque type de
     * document
     */
    private static final String[] CATEGORY_CODES = { "1", "2", "3", "4", "5", "11" };
    private static final String[] DOC_TYPE_SUFFIXES = { "en", "cni", "pp" };

    public FolderResponse createFolder(CreateFolderRequest request) {
        String baseFolderName = generateBaseFolderName(request);
        folderPath = findAvailablePath(baseFolderName);

        try {
            Files.createDirectories(folderPath);
            log.info("Dossier créé avec succès folderPath: {}", folderPath);

            // Création automatique des sous-dossiers pour chaque catégorie × type
            createSubFolders(folderPath);

            return FolderResponse.builder()
                    .folderName(folderPath.getFileName().toString())
                    .fullPath(folderPath.toString())
                    .build();
        } catch (IOException e) {
            log.error("Erreur lors de la création du dossier: {}", e.getMessage());
            throw new FolderCreationException("Impossible de créer le dossier: " + e.getMessage());
        }
    }

    /**
     * Crée les sous-dossiers pour chaque catégorie × type de document.
     * Exemple: 1_en, 1_cni, 1_pp, 2_en, 2_cni, 2_pp, ...
     */
    private void createSubFolders(Path parentFolder) throws IOException {
        for (String category : CATEGORY_CODES) {
            for (String docType : DOC_TYPE_SUFFIXES) {
                Path subPath = parentFolder.resolve(category + "_" + docType);
                if (!Files.exists(subPath)) {
                    Files.createDirectories(subPath);
                    log.info("Sous-dossier créé: {}", subPath);
                }
            }
        }
    }

    private String generateBaseFolderName(CreateFolderRequest request) {
        return (request.getNom() + request.getPrenom() +
                LocalDate.now().format(DATE_FORMAT)).toLowerCase();
    }

    private Path findAvailablePath(String baseName) { // ajoute "_i" si le dossier existe dèjà
        Path baseFolder = Paths.get(rootPathString, baseName);
        if (!Files.exists(baseFolder)) {
            return baseFolder;
        }

        int suffix = 1;
        Path folderPath;
        do {
            folderPath = Paths.get(rootPathString, baseName + "_" + suffix++);
        } while (Files.exists(folderPath));

        return folderPath;
    }
}