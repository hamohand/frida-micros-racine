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

    @org.springframework.beans.factory.annotation.Value("${gcp.project.id:docai-419410}")
    private String projectId;
    
    @org.springframework.beans.factory.annotation.Value("${gcp.location:eu}")
    private String location;
    
    @org.springframework.beans.factory.annotation.Value("${gcp.processor.id:7e616fae24f40bf3}")
    private String processorId;

    /**
     * Cette classe extrait les données des extraits de naissance en utilisant l'IA.
     */
    
    public static class FolderScanResult {
        private final List<String> tableauNumParente = new ArrayList<>();
        private final List<Path> pdfFiles = new ArrayList<>();
        private final Map<Path, DocumentInfo> fileDocumentInfoMap = new HashMap<>();

        public List<String> getTableauNumParente() { return tableauNumParente; }
        public List<Path> getPdfFiles() { return pdfFiles; }
        public Map<Path, DocumentInfo> getFileDocumentInfoMap() { return fileDocumentInfoMap; }
    }

    // La méthode setFolderPath a été supprimée pour rendre le service stateless

    /**
     * Liste les fichiers du dossier configuré de manière récursive.
     *
     * @throws IOException           En cas d'erreur d'accès au système de fichiers.
     */
    public FolderScanResult listFolderContents(String folderPath) throws IOException {
        FolderScanResult result = new FolderScanResult();
        logger.info("Analyse des fichiers dans le dossier : {}", folderPath);

        try {
            // Un tableau à 1 élément pour stocker le nom du dossier courant dans le foreach (lambda)
            final Path[] directoryName = new Path[1];
            Files.walk(Paths.get(folderPath))
                    .forEach(filePath -> {
                        if (Files.isDirectory(filePath)) {
                            directoryName[0] = filePath;
                            logger.info("Dossier détecté : {}", filePath.getFileName());
                        } else if (Files.isRegularFile(filePath)) {
                            String fileName = filePath.getFileName().toString().toLowerCase();
                            if (fileName.endsWith(".pdf") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")
                                    || fileName.endsWith(".png")) {
                                result.getPdfFiles().add(filePath);
                                String folderName = directoryName[0] != null ? directoryName[0].getFileName().toString() : filePath.getParent().getFileName().toString();
                                logger.info("Fichier détecté : {} dans dossier {}", filePath.getFileName(), folderName);

                                // Parse folder name format: "{code}_{type}" (ex: "2_cni")
                                try {
                                    DocumentInfo docInfo = DocumentInfo.fromFolderName(folderName);
                                    result.getFileDocumentInfoMap().put(filePath, docInfo);
                                    result.getTableauNumParente().add(docInfo.getHeirCategory().getFormattedCode());
                                    logger.info("Document: {} -> catégorie={}, type={}",
                                            filePath.getFileName(),
                                            docInfo.getHeirCategory(),
                                            docInfo.getDocumentType());
                                } catch (IllegalArgumentException e) {
                                    // Fallback: ancien format (juste le numéro de parenté)
                                    logger.warn(
                                            "Format de dossier non reconnu '{}', utilisation comme numéro de parenté. Erreur: ",
                                            folderName, e);
                                    result.getTableauNumParente().add(folderName);
                                }
                            } else {
                                logger.debug("Fichier ignoré : {}", filePath.getFileName());
                            }
                        }
                    });
            logger.info("Analyse terminée : {} fichiers détectés.", result.getPdfFiles().size());
        } catch (IOException e) {
            logger.error("Erreur d'accès aux fichiers du dossier : {}", folderPath, e);
            throw e;
        }
        logger.debug("tableauNumParente : {}", result.getTableauNumParente());
        return result;
    }

    /**
     * Lecture et traitement des fichiers présents dans le dossier à l'aide d'une
     * IA.
     *
     * @return Une liste de documents extraits.
     */
    public List<Document> lecturePdfsAi(List<Path> pdfFiles) {
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

    // validateFolderPath supprimée car plus besoin d'état
}
