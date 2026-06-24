package com.muhend.backendai.service.pipeline;

import com.muhend.backendai.client.ocr.dto.OcrEntityDefinitionDto;
import com.muhend.backendai.dto.DocumentInfo;
import com.muhend.backendai.dto.CompositionAnalysisDto;
import com.muhend.backendai.dto.FicheUpdateDto;
import com.muhend.backendai.entities.*;
import com.muhend.backendai.enums.DocumentType;
import com.muhend.backendai.enums.HeirCategory;
import com.muhend.backendai.service.dossier.FolderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;

/**
 * Orchestrateur principal pour le traitement des documents d'héritiers.
 * <p>
 * Coordonne les services spécialisés :
 * <ul>
 *     <li>{@link OcrMappingService} — OCR et mapping des résultats</li>
 *     <li>{@link FridaIdentifierService} — Génération d'identifiants</li>
 *     <li>{@link FridaPersistenceService} — Sauvegarde en base</li>
 * </ul>
 * L'état mutable est encapsulé dans {@link TraitementContext},
 * ce qui rend ce service thread-safe.
 */
@Slf4j
@Service
public class DossierProcessingService {

    private final FolderService folderService;
    private final OcrMappingService ocrMappingService;
    private final FridaIdentifierService fridaIdentifierService;
    private final FridaPersistenceService fridaPersistenceService;

    @Value("${MAX_PARALLEL_FOLDERS:2}")
    private int maxParallelFolders;

    private Semaphore maxConcurrentFoldersSemaphore;

    @PostConstruct
    public void init() {
        log.info("Initialisation du Semaphore pour les dossiers parallèles. Max: {}", maxParallelFolders);
        maxConcurrentFoldersSemaphore = new Semaphore(maxParallelFolders);
    }

    public DossierProcessingService(
            FolderService folderService,
            OcrMappingService ocrMappingService,
            FridaIdentifierService fridaIdentifierService,
            FridaPersistenceService fridaPersistenceService) {
        this.folderService = folderService;
        this.ocrMappingService = ocrMappingService;
        this.fridaIdentifierService = fridaIdentifierService;
        this.fridaPersistenceService = fridaPersistenceService;
    }

    // ======================= Point d'entrée =======================

    /**
     * Point d'entrée principal pour traiter un dossier de documents d'héritiers.
     * Supporte les types : Extrait de Naissance, CNI, Passeport.
     *
     * @param folderPath Chemin vers le dossier contenant les sous-dossiers
     *                   au format {code}_{type} (ex: 1_en, 2_cni).
     * @return La fiche Frida créée, ou {@code null} si aucun document traité.
     */
    @org.springframework.transaction.annotation.Transactional
    public FridaEntity traiterExtraitsNaissance(String folderPath, String mode) {
        try {
            // Blocage si le nombre max de dossiers simultanés est atteint
            maxConcurrentFoldersSemaphore.acquire();
            log.info("Début traitement dossier (thread libéré/acquis). Dossier: {}", folderPath);

            FolderService.FolderScanResult scanResult = folderService.listFolderContents(folderPath);
            TraitementContext ctx = initialiserContext(scanResult);

            Map<Path, DocumentInfo> fileDocInfoMap = scanResult.getFileDocumentInfoMap();
            Map<String, OcrEntityDefinitionDto> entityDefCache = new HashMap<>();
            List<Path> files = scanResult.getPdfFiles();

            if (files.isEmpty()) {
                log.warn("Aucun document trouvé dans le dossier : {}", folderPath);
                marquerDossierCommeTraite(Paths.get(folderPath));
                return null;
            }

            // Séparer les fichiers recto (traitement OCR) des fichiers verso (MRZ uniquement)
            List<Path> rectoFiles = new ArrayList<>();
            Map<String, Path> versoFilesByFolder = new HashMap<>();

            for (Path file : files) {
                String fileName = file.getFileName().toString().toLowerCase();
                if (fileName.contains("verso")) {
                    // Fichier verso : on le stocke indexé par son dossier parent
                    String parentFolder = file.getParent().getFileName().toString();
                    versoFilesByFolder.put(parentFolder, file);
                    log.info("📋 Fichier verso détecté : {} (dossier {})", file.getFileName(), parentFolder);
                } else {
                    rectoFiles.add(file);
                }
            }

            int indiceParente = 0;
            for (Path file : rectoFiles) {
                try {
                    // Chercher un verso compagnon dans le même sous-dossier
                    String parentFolder = file.getParent().getFileName().toString();
                    Path versoFile = versoFilesByFolder.get(parentFolder);

                    indiceParente = traiterFichier(ctx, file, versoFile, fileDocInfoMap,
                            entityDefCache, indiceParente, mode);
                } catch (Exception e) {
                    log.error("Erreur traitement fichier OCR : {} - {}", file, e.getMessage(), e);
                }
            }

            if (indiceParente > 0) {
                if ("0".equals(ctx.getNumFrida())) {
                    ctx.setNumFrida(fridaIdentifierService.genererIdentifiant(""));
                }
                fridaPersistenceService.sauvegarderBrouillonFrida(ctx);
            }

            // Marquer le dossier comme traité
            marquerDossierCommeTraite(Paths.get(folderPath));

            return ctx.getFicheFrida();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Le traitement a été interrompu en attendant le Semaphore : {}", e.getMessage(), e);
            return null;
        } catch (IOException e) {
            log.error("Erreur lors de la lecture des fichiers : {}", e.getMessage(), e);
            return null;
        } finally {
            maxConcurrentFoldersSemaphore.release();
            log.info("Fin traitement dossier (thread libéré). Dossier: {}", folderPath);
        }
    }

    // ======================= Traitement d'un fichier =======================

    /**
     * Traite un fichier individuel : OCR → mapping → sauvegarde.
     *
     * @param versoFile Fichier verso optionnel (pour lecture MRZ sur CNI).
     * @return L'indice de parenté mis à jour (incrémenté si traitement réussi).
     */
    private int traiterFichier(TraitementContext ctx, Path file, Path versoFile,
                               Map<Path, DocumentInfo> fileDocInfoMap,
                               Map<String, OcrEntityDefinitionDto> entityDefCache,
                               int indiceParente,
                               String mode) {

        DocumentInfo docInfo = fileDocInfoMap.get(file);
        DocumentType docType = (docInfo != null) ? docInfo.getDocumentType() : DocumentType.EXTRAIT_NAISSANCE;
        HeirCategory heirCategory = (docInfo != null) ? docInfo.getHeirCategory() : HeirCategory.DEFUNT;

        // Récupérer la définition OCR (avec cache)
        String entityName = (docInfo != null) ? docInfo.getEntityName() : null;
        OcrEntityDefinitionDto entityDef = ocrMappingService.getOrCacheEntityDef(entityDefCache, docType, entityName);
        if (entityDef == null) {
            return indiceParente;
        }

        log.info("Traitement: {} -> type={}, catégorie={}{}", file.getFileName(), docType, heirCategory,
                versoFile != null ? " [verso: " + versoFile.getFileName() + "]" : "");

        // OCR + mapping (avec verso optionnel pour MRZ)
        IdentitesEntity identite = null;
        try {
            identite = ocrMappingService.processFile(file, versoFile, entityDef, docType, mode);
        } catch (Exception e) {
            log.error("Erreur traitement fichier OCR (fallback sur entité vide à corriger) : {} - {}", file, e.getMessage(), e);
            identite = new IdentitesEntity();
            identite.setRequiresCorrection(true);
            identite.setConfidencesJson("{\"erreur\":0.0, \"nom\":0.0, \"prenom\":0.0, \"dateNaissance\":0.0, \"lieuNaissance\":0.0, \"sexe\":0.0, \"nin\":0.0}");
            identite.setRawOcrTextJson("{\"erreur\":\"OCR a échoué: " + e.getMessage().replace("\"", "'") + "\"}");
        }

        if (identite != null) {
            // Générer l'identifiant au document du défunt (peu importe l'ordre de traitement)
            if (heirCategory == HeirCategory.DEFUNT && "0".equals(ctx.getNumFrida())) {
                String dateNaissance = identite.getDateNaissance() != null
                        ? identite.getDateNaissance().toString() : "";
                ctx.setNumFrida(fridaIdentifierService.genererIdentifiant(dateNaissance));
            }

            fridaPersistenceService.sauvegarderDocument(ctx, identite, heirCategory, indiceParente);
            return indiceParente + 1;
        }

        return indiceParente;
    }

    // ======================= Initialisation =======================

    private TraitementContext initialiserContext(FolderService.FolderScanResult scanResult) {
        TraitementContext ctx = new TraitementContext();
        ctx.setTableauNumParente(scanResult.getTableauNumParente());
        return ctx;
    }

    // ======================= Délégation vers FridaPersistenceService =======================

    /**
     * Ecrase le brouillon de l'IA avec la version corrigée par l'humain.
     */
    @org.springframework.transaction.annotation.Transactional
    public void sauvegarderFicheCorrigee(String numFrida, FicheUpdateDto dto) {
        fridaPersistenceService.sauvegarderFicheCorrigee(numFrida, dto);
    }

    /**
     * Lance le calcul des parts sur une Frida existante et la met à jour.
     */
    @org.springframework.transaction.annotation.Transactional
    public FridaEntity lancerCalcul(String numFrida) {
        return fridaPersistenceService.lancerCalcul(numFrida);
    }

    // ======================= Utilitaires =======================

    private void marquerDossierCommeTraite(Path folderPath) {
        try {
            Path processedFile = folderPath.resolve(".processed");
            if (!java.nio.file.Files.exists(processedFile)) {
                java.nio.file.Files.createFile(processedFile);
                log.info("Dossier marqué comme traité : {}", processedFile);
            }
        } catch (IOException e) {
            log.error("Impossible de créer le fichier .processed dans {}", folderPath, e);
        }
    }
}
