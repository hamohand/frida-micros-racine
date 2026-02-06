package com.muhend.backendai.service.aibd;

import com.google.cloud.documentai.v1.*;
import com.google.protobuf.ByteString;
import com.muhend.backendai.dto.DocumentInfo;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LectureAiService {
    private static final Logger logger = LoggerFactory.getLogger(LectureAiService.class); // Utilisation du logger

    /**
     * Cette classe extrait les données des extraits de naissance en utilisant l'IA.
     */
    @Getter
    private String folderPath; // Chemin du dossier à analyser
    private Path directoryName; // nom du dossier courant
    private final List<String> tableauNumParente = new ArrayList<>(); // tableau des parentés (codes: 0=témoin,
                                                                      // 1=défunt, etc.)
    @Getter
    private final List<Path> pdfFiles = new ArrayList<>(); // Liste des fichiers détectés
    @Getter
    private final Map<Path, DocumentInfo> fileDocumentInfoMap = new HashMap<>(); // Mapping fichier -> info document

    /**
     * Définit le chemin du dossier pour le traitement.
     *
     * @param folderPath Chemin vers le dossier à configurer.
     * @throws IllegalArgumentException Si le chemin est null ou vide.
     */
    public void setFolderPath(String folderPath) {
        if (folderPath == null || folderPath.trim().isEmpty()) {
            logger.error("Le chemin fourni pour le dossier est null ou vide.");
            throw new IllegalArgumentException("Le chemin du dossier ne peut pas être null ou vide.");
        }
        this.folderPath = folderPath;
        logger.info("Chemin du dossier configuré : {}", folderPath);
    }

    /**
     * Liste les fichiers du dossier configuré de manière récursive.
     *
     * @throws IllegalStateException Si le dossier n'est pas configuré ou
     *                               inexistant.
     * @throws IOException           En cas d'erreur d'accès au système de fichiers.
     */
    public List<String> listFolderContents() throws IOException {
        pdfFiles.clear();
        tableauNumParente.clear();
        fileDocumentInfoMap.clear();
        logger.info("Analyse des fichiers dans le dossier : {}", folderPath);

        try {
            Files.walk(Paths.get(folderPath))
                    .forEach(filePath -> {
                        if (Files.isDirectory(filePath)) {
                            directoryName = filePath;
                            logger.info("Dossier détecté : {}", filePath.getFileName());
                        } else if (Files.isRegularFile(filePath)) {
                            String fileName = filePath.getFileName().toString().toLowerCase();
                            if (fileName.endsWith(".pdf") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")
                                    || fileName.endsWith(".png")) {
                                pdfFiles.add(filePath);
                                String folderName = directoryName.getFileName().toString();
                                logger.info("Fichier détecté : {} dans dossier {}", filePath.getFileName(), folderName);

                                // Parse folder name format: "{code}_{type}" (ex: "2_cni")
                                try {
                                    DocumentInfo docInfo = DocumentInfo.fromFolderName(folderName);
                                    fileDocumentInfoMap.put(filePath, docInfo);
                                    tableauNumParente.add(String.valueOf(docInfo.getHeirCategory().getCode()));
                                    logger.info("Document: {} -> catégorie={}, type={}",
                                            filePath.getFileName(),
                                            docInfo.getHeirCategory(),
                                            docInfo.getDocumentType());
                                } catch (IllegalArgumentException e) {
                                    // Fallback: ancien format (juste le numéro de parenté)
                                    logger.warn(
                                            "Format de dossier non reconnu '{}', utilisation comme numéro de parenté",
                                            folderName);
                                    tableauNumParente.add(folderName);
                                }
                            } else {
                                logger.debug("Fichier ignoré : {}", filePath.getFileName());
                            }
                        }
                    });
            logger.info("Analyse terminée : {} fichiers détectés.", pdfFiles.size());
        } catch (IOException e) {
            logger.error("Erreur d'accès aux fichiers du dossier : {}", folderPath, e);
            throw e;
        }
        logger.debug("tableauNumParente : {}", tableauNumParente);
        return tableauNumParente;
    }

    /**
     * Lecture et traitement des fichiers présents dans le dossier à l'aide d'une
     * IA.
     *
     * @return Une liste de documents extraits.
     */
    public List<Document> lecturePdfsAi() {
        if (pdfFiles.isEmpty()) {
            logger.warn("Aucun fichier disponible dans la liste des fichiers pour le traitement.");
            return new ArrayList<>();
        }

        logger.info("Traitement des fichiers détectés avec l'IA...");
        List<Document> extractedDocuments = new ArrayList<>();

        for (Path file : pdfFiles) {
            try {
                Document document = mockDocumentExtraction(file);
                extractedDocuments.add(document);
                logger.debug("Document extrait avec succès : {}", file);
            } catch (Exception e) {
                logger.error("Erreur lors du traitement du fichier : {}", file, e);
            }
        }

        logger.info("Traitement terminé : {} documents extraits.", extractedDocuments.size());
        return extractedDocuments;
    }

    /**
     * Simule un traitement IA pour extraire un objet Document à partir d'un
     * fichier.
     *
     * @param filePath Chemin du fichier à traiter.
     * @return Un document simulé.
     */
    private Document mockDocumentExtraction(Path filePath) throws IOException {
        // Paramètres pour l'IA 'Document Ai'
        String projectId = "docai-419410";
        String location = "eu"; // Format is "us" or "eu".
        String processorId = "7e616fae24f40bf3"; // extrait de naissance
        // String processorId = "8f557d7383790093"; // facture
        String endpoint = String.format("%s-documentai.googleapis.com:443", location);

        DocumentProcessorServiceSettings settings = DocumentProcessorServiceSettings.newBuilder().setEndpoint(endpoint)
                .build();
        try (DocumentProcessorServiceClient client = DocumentProcessorServiceClient.create(settings)) {
            //
            String name = String.format("projects/%s/locations/%s/processors/%s", projectId, location, processorId);

            // Read the file.
            byte[] imageFileData = Files.readAllBytes(Paths.get(filePath.toUri()));
            // System.out.println("imageFileData : " + new String(imageFileData,
            // StandardCharsets.UTF_8));

            // Convert the image data to a Buffer and base64 encode it.
            ByteString content = ByteString.copyFrom(imageFileData);
            // System.out.println("content : " + content);
            RawDocument document = RawDocument.newBuilder().setContent(content).setMimeType("application/pdf").build();
            // Configure the process request.
            ProcessRequest request = ProcessRequest.newBuilder().setName(name).setRawDocument(document).build();
            // Recognizes text entities in the PDF document
            ProcessResponse result = client.processDocument(request);

            return result.getDocument();
        }
    }

    /**
     * Valide que le chemin du dossier est correctement configuré.
     *
     * @throws IllegalStateException Si le chemin du dossier est null ou vide.
     */
    private void validateFolderPath() {
        if (folderPath == null || folderPath.trim().isEmpty()) {
            logger.error("Le chemin du dossier n'est pas configuré.");
            throw new IllegalStateException("Le chemin vers le dossier est invalide ou non configuré.");
        }
    }
}
