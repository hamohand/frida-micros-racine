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
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
@Profile("!calc-only")
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

            // Un recto (ou un dump NFC sans recto) = une personne.
            // Fils et filles d'une même catégorie partagent un seul sous-dossier
            // ("03_en_en_01") : le sexe vient du QR code. Du 2026-07-02 (9654f25) à
            // cette correction, les fichiers étaient regroupés par catégorie et
            // s'écrasaient : un seul enfant était traité, et jamais les autres.
            List<DocumentsPersonne> personnes = regrouperParPersonne(files);

            int processedCount = 0;
            for (DocumentsPersonne pers : personnes) {
                try {
                    log.info("Traitement héritier {} : Recto={}, Verso={}, NFC={}",
                            pers.heirCode, pers.recto, pers.verso, pers.nfc);
                    boolean success = traiterFichier(ctx, pers.recto, pers.verso, pers.nfc, fileDocInfoMap,
                            entityDefCache, mode, pers.heirCode);
                    if (success) {
                        processedCount++;
                    }
                } catch (Exception e) {
                    log.error("Erreur traitement héritier {} : {}", pers.heirCode, e.getMessage(), e);
                }
            }

            if (processedCount > 0) {
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

    // ======================= Regroupement par personne =======================

    /** Documents rattachés à une même personne. */
    private static final class DocumentsPersonne {
        final String heirCode;
        final Path recto;
        Path verso;
        Path nfc;

        DocumentsPersonne(String heirCode, Path recto, Path nfc) {
            this.heirCode = heirCode;
            this.recto = recto;
            this.nfc = nfc;
        }
    }

    /**
     * Regroupe les fichiers d'un dossier par personne.
     * <p>
     * Chaque recto est une personne. Un verso ou un dump NFC est rattaché au recto de
     * même nom de base (le frontend nomme le verso d'après son recto) ; à défaut, au
     * recto unique de sa catégorie s'il est lui-même le seul compagnon de ce type.
     * Si l'appariement reste ambigu, rien n'est rattaché : mieux vaut perdre une
     * lecture MRZ que lire la CNI d'une autre personne. Un dump NFC non rattaché est
     * traité comme une personne à part entière.
     */
    private List<DocumentsPersonne> regrouperParPersonne(List<Path> files) {
        List<DocumentsPersonne> personnes = new ArrayList<>();
        Map<String, List<DocumentsPersonne>> rectosParCategorie = new LinkedHashMap<>();
        List<Path> versos = new ArrayList<>();
        List<Path> dumpsNfc = new ArrayList<>();
        Map<String, Integer> versosParCategorie = new HashMap<>();
        Map<String, Integer> nfcParCategorie = new HashMap<>();

        for (Path file : files) {
            String fileName = file.getFileName().toString().toLowerCase();
            String parentFolder = file.getParent().getFileName().toString();
            String code = codeHeritier(file);

            if (fileName.endsWith(".json")) {
                dumpsNfc.add(file);
                nfcParCategorie.merge(code, 1, Integer::sum);
            } else if (fileName.contains("verso") || parentFolder.contains("verso")) {
                versos.add(file);
                versosParCategorie.merge(code, 1, Integer::sum);
            } else {
                DocumentsPersonne pers = new DocumentsPersonne(code, file, null);
                personnes.add(pers);
                rectosParCategorie.computeIfAbsent(code, k -> new ArrayList<>()).add(pers);
            }
        }

        for (Path verso : versos) {
            DocumentsPersonne cible = trouverRecto(verso, rectosParCategorie, versosParCategorie);
            if (cible != null && cible.verso == null) {
                cible.verso = verso;
            } else {
                log.warn("Verso {} non rattaché : aucun recto correspondant sans ambiguïté", verso.getFileName());
            }
        }

        for (Path dump : dumpsNfc) {
            DocumentsPersonne cible = trouverRecto(dump, rectosParCategorie, nfcParCategorie);
            if (cible != null && cible.nfc == null) {
                cible.nfc = dump;
            } else {
                personnes.add(new DocumentsPersonne(codeHeritier(dump), null, dump));
            }
        }

        return personnes;
    }

    /**
     * Recto auquel rattacher un verso ou un dump NFC : même nom de base d'abord,
     * sinon le recto unique de la catégorie quand le compagnon est lui aussi unique.
     */
    private DocumentsPersonne trouverRecto(Path compagnon,
                                           Map<String, List<DocumentsPersonne>> rectosParCategorie,
                                           Map<String, Integer> compagnonsParCategorie) {
        String code = codeHeritier(compagnon);
        List<DocumentsPersonne> rectos = rectosParCategorie.getOrDefault(code, List.of());

        String base = nomDeBase(compagnon);
        List<DocumentsPersonne> memeNom = rectos.stream()
                .filter(r -> nomDeBase(r.recto).equals(base))
                .toList();
        if (memeNom.size() == 1) {
            return memeNom.get(0);
        }
        if (rectos.size() == 1 && compagnonsParCategorie.getOrDefault(code, 0) == 1) {
            return rectos.get(0);
        }
        return null;
    }

    /** Code de parenté sur deux chiffres ("3_en" et "03_en" donnent tous deux "03"). */
    private static String codeHeritier(Path file) {
        String code = file.getParent().getFileName().toString().split("_")[0];
        try {
            return String.format("%02d", Integer.parseInt(code));
        } catch (NumberFormatException e) {
            return code;
        }
    }

    /**
     * Nom de base d'un fichier stocké, sans l'horodatage ajouté au stockage, sans
     * extension et sans le suffixe "_verso" : "303_cniA_verso.png" donne "cnia".
     */
    private static String nomDeBase(Path file) {
        String nom = file.getFileName().toString();
        nom = nom.replaceFirst("^\\d+_", "");
        nom = nom.replaceFirst("\\.[^.]+$", "");
        nom = nom.replaceFirst("(?i)_verso$", "");
        return nom.toLowerCase();
    }

    // ======================= Traitement d'un fichier =======================

    /**
     * Traite un fichier individuel : OCR → mapping → sauvegarde.
     *
     * @param versoFile Fichier verso optionnel (pour lecture MRZ sur CNI).
     * @return true si le fichier a été traité avec succès, false sinon.
     */
    private boolean traiterFichier(TraitementContext ctx, Path file, Path versoFile, Path nfcJsonFile,
                                   Map<Path, DocumentInfo> fileDocInfoMap,
                                   Map<String, OcrEntityDefinitionDto> entityDefCache,
                                   String mode, String heirCode) {

        DocumentInfo docInfo = (file != null) ? fileDocInfoMap.get(file) : ((nfcJsonFile != null) ? fileDocInfoMap.get(nfcJsonFile) : null);
        DocumentType docType = (docInfo != null) ? docInfo.getDocumentType() : DocumentType.EXTRAIT_NAISSANCE;
        HeirCategory heirCategory = (docInfo != null) ? docInfo.getHeirCategory() : HeirCategory.DEFUNT;
        
        String numParente;
        if (docInfo != null) {
            numParente = docInfo.getHeirCategory().getFormattedCode();
        } else {
            numParente = heirCode;
        }

        // Récupérer la définition OCR (avec cache)
        String entityName = (docInfo != null) ? docInfo.getEntityName() : null;
        OcrEntityDefinitionDto entityDef = ocrMappingService.getOrCacheEntityDef(entityDefCache, docType, entityName);
        if (entityDef == null) {
            return false;
        }

        log.info("Traitement: {} -> type={}, catégorie={}{}{}", 
                file != null ? file.getFileName() : nfcJsonFile.getFileName(), 
                docType, heirCategory,
                versoFile != null ? " [verso: " + versoFile.getFileName() + "]" : "",
                nfcJsonFile != null ? " [nfc: " + nfcJsonFile.getFileName() + "]" : "");

        IdentitesEntity ocrEntity = null;
        IdentitesEntity nfcEntity = null;

        // 1. OCR (Recto + Verso optionnel)
        if (file != null) {
            try {
                ocrEntity = ocrMappingService.processFile(file, versoFile, entityDef, docType, mode);
            } catch (Exception e) {
                log.error("Erreur traitement fichier OCR : {} - {}", file, e.getMessage(), e);
                ocrEntity = new IdentitesEntity();
                ocrEntity.setRequiresCorrection(true);
                ocrEntity.setConfidencesJson("{\"erreur\":0.0}");
                ocrEntity.setRawOcrTextJson("{\"erreur\":\"OCR a échoué: " + e.getMessage().replace("\"", "'") + "\"}");
            }
        }

        // 2. NFC
        if (nfcJsonFile != null) {
            try {
                // On utilise la méthode existante processFile mais on lui passe le JSON (qui est géré par OcrMappingService)
                nfcEntity = ocrMappingService.processFile(nfcJsonFile, null, null, docType, mode);
            } catch (Exception e) {
                log.error("Erreur traitement fichier NFC : {} - {}", nfcJsonFile, e.getMessage(), e);
            }
        }

        // 3. Fusion
        IdentitesEntity identite = ocrMappingService.mergeOcrAndNfc(ocrEntity, nfcEntity);

        if (identite != null) {
            // Générer l'identifiant au document du défunt (peu importe l'ordre de traitement)
            if (heirCategory == HeirCategory.DEFUNT && "0".equals(ctx.getNumFrida())) {
                String dateNaissance = identite.getDateNaissance() != null
                        ? identite.getDateNaissance().toString() : "";
                ctx.setNumFrida(fridaIdentifierService.genererIdentifiant(dateNaissance));
            }

            fridaPersistenceService.sauvegarderDocument(ctx, identite, heirCategory, numParente);
            return true;
        }

        return false;
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
