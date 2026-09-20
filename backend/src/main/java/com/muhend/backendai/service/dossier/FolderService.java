package com.muhend.backendai.service.dossier;

import com.muhend.backendai.config.exception.FolderCreationException;
import com.muhend.backendai.dto.DocumentInfo;
import com.muhend.backendai.dto.dossier.CreateFolderRequest;
import com.muhend.backendai.dto.dossier.FolderResponse;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import com.muhend.backendai.config.util.PathResolver;
import lombok.RequiredArgsConstructor;

@Profile("!calc-only")
@Service
@Slf4j
@RequiredArgsConstructor
public class FolderService {

    // ======================= Scan de dossier =======================

    public static class FolderScanResult {
        private final List<String> tableauNumParente = new ArrayList<>();
        private final List<Path> pdfFiles = new ArrayList<>();
        private final Map<Path, DocumentInfo> fileDocumentInfoMap = new HashMap<>();

        public List<String> getTableauNumParente() { return tableauNumParente; }
        public List<Path> getPdfFiles() { return pdfFiles; }
        public Map<Path, DocumentInfo> getFileDocumentInfoMap() { return fileDocumentInfoMap; }
    }

    /**
     * Liste les fichiers du dossier configuré de manière récursive.
     *
     * @throws IOException           En cas d'erreur d'accès au système de fichiers.
     */
    public FolderScanResult listFolderContents(String folderPath) throws IOException {
        FolderScanResult result = new FolderScanResult();
        log.info("Analyse des fichiers dans le dossier : {}", folderPath);

        try {
            // Un tableau à 1 élément pour stocker le nom du dossier courant dans le foreach (lambda)
            final Path[] directoryName = new Path[1];
            Files.walk(Paths.get(folderPath))
                    .forEach(filePath -> {
                        if (Files.isDirectory(filePath)) {
                            directoryName[0] = filePath;
                            log.info("Dossier détecté : {}", filePath.getFileName());
                        } else if (Files.isRegularFile(filePath)) {
                            String fileName = filePath.getFileName().toString().toLowerCase();
                            if (fileName.endsWith(".pdf") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")
                                    || fileName.endsWith(".png") || fileName.endsWith(".json")) {
                                result.getPdfFiles().add(filePath);
                                String folderName = directoryName[0] != null ? directoryName[0].getFileName().toString() : filePath.getParent().getFileName().toString();
                                log.info("Fichier détecté : {} dans dossier {}", filePath.getFileName(), folderName);

                                // Parse folder name format: "{code}_{type}" (ex: "2_cni")
                                try {
                                    DocumentInfo docInfo = DocumentInfo.fromFolderName(folderName);
                                    result.getFileDocumentInfoMap().put(filePath, docInfo);
                                    result.getTableauNumParente().add(docInfo.getHeirCategory().getFormattedCode());
                                    log.info("Document: {} -> catégorie={}, type={}",
                                            filePath.getFileName(),
                                            docInfo.getHeirCategory(),
                                            docInfo.getDocumentType());
                                } catch (IllegalArgumentException e) {
                                    // Fallback: ancien format (juste le numéro de parenté)
                                    log.warn(
                                            "Format de dossier non reconnu '{}', utilisation comme numéro de parenté. Erreur: ",
                                            folderName, e);
                                    result.getTableauNumParente().add(folderName);
                                }
                            } else {
                                log.debug("Fichier ignoré : {}", filePath.getFileName());
                            }
                        }
                    });
            log.info("Analyse terminée : {} fichiers détectés.", result.getPdfFiles().size());
        } catch (IOException e) {
            log.error("Erreur d'accès aux fichiers du dossier : {}", folderPath, e);
            throw e;
        }
        log.debug("tableauNumParente : {}", result.getTableauNumParente());
        return result;
    }

    @Value("${ROOT_PATH:/app/uploads}")
    private String rootPathString;
    
    private final PathResolver pathResolver;
    private final com.muhend.backendai.repository.BrouillonRepo brouillonRepository;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    @Getter
    public static Path folderPath;

    public FolderResponse createFolder(CreateFolderRequest request) {
        String baseFolderName = generateBaseFolderName(request);
        folderPath = findAvailablePath(baseFolderName);

        try {
            Files.createDirectories(folderPath);
            log.info("Dossier créé avec succès folderPath: {}", folderPath);

            com.muhend.backendai.entities.BrouillonEntity brouillon = new com.muhend.backendai.entities.BrouillonEntity();
            brouillon.setFolderName(folderPath.getFileName().toString());
            brouillon.setFolderPath(folderPath.toString());
            brouillon.setNomDefunt(request.getNom());
            brouillon.setPrenomDefunt(request.getPrenom());
            brouillon.setDateCreation(LocalDate.now());
            brouillon.setStatut("EN_COURS");
            brouillonRepository.save(brouillon);
            log.info("Brouillon créé en BD: {}", brouillon.getFolderName());

            return FolderResponse.builder()
                    .folderName(folderPath.getFileName().toString())
                    .fullPath(folderPath.toString())
                    .build();
        } catch (IOException e) {
            log.error("Erreur lors de la création du dossier: {}", e.getMessage());
            throw new FolderCreationException("Impossible de créer le dossier: " + e.getMessage());
        }
    }

    private String generateBaseFolderName(CreateFolderRequest request) {
        return (request.getNom() + request.getPrenom() +
                LocalDate.now().format(DATE_FORMAT)).toLowerCase();
    }

    private Path findAvailablePath(String baseName) { // ajoute "_i" si le dossier existe dèjà
        Path dossiersPath = Paths.get(rootPathString, "dossiers");
        try {
            if (!Files.exists(dossiersPath)) {
                Files.createDirectories(dossiersPath);
            }
        } catch (IOException e) {
            log.error("Erreur lors de la création du dossier parent 'dossiers': {}", e.getMessage());
        }
        
        Path baseFolder = dossiersPath.resolve(baseName);
        if (!Files.exists(baseFolder)) {
            return baseFolder;
        }

        int suffix = 1;
        Path folderPath;
        do {
            folderPath = dossiersPath.resolve(baseName + "_" + suffix++);
        } while (Files.exists(folderPath));

        return folderPath;
    }

    public void clearLatestFolder() {
        try {
            Path latest = pathResolver.getLatestFolder();
            Files.walk(latest)
                 .filter(Files::isRegularFile)
                 .forEach(file -> {
                     try {
                         Files.delete(file);
                     } catch (IOException e) {
                         log.error("Impossible de supprimer {}", file);
                     }
                 });
        } catch (IOException e) {
            log.error("Erreur lors du nettoyage du dernier dossier: {}", e.getMessage());
        }
    }

    public java.util.List<String> getPendingBatchFolders() {
        java.util.List<String> pendingFolders = new java.util.ArrayList<>();
        Path rootPath = Paths.get(rootPathString, "dossiers");
        if (!Files.exists(rootPath) || !Files.isDirectory(rootPath)) {
            return pendingFolders;
        }

        try (java.util.stream.Stream<Path> stream = Files.list(rootPath)) {
            stream.filter(Files::isDirectory).forEach(path -> {
                Path processedFile = path.resolve(".processed");
                if (!Files.exists(processedFile)) {
                    pendingFolders.add(path.getFileName().toString());
                }
            });
        } catch (IOException e) {
            log.error("Erreur lors de la récupération des dossiers batch en attente : {}", e.getMessage(), e);
        }
        return pendingFolders;
    }
}
