package com.muhend.backendai.service.aibd;

import com.muhend.backendai.client.ocr.dto.OcrEntityDefinitionDto;
import com.muhend.backendai.dto.DocumentInfo;
import com.muhend.backendai.dto.CompositionAnalysisDto;
import com.muhend.backendai.entities.*;
import com.muhend.backendai.enums.DocumentType;
import com.muhend.backendai.enums.HeirCategory;
import com.muhend.backendai.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
 *     <li>{@link HeirPartCalculatorService} — Calcul des parts</li>
 * </ul>
 * L'état mutable est encapsulé dans {@link TraitementContext},
 * ce qui rend ce service thread-safe.
 */
@Slf4j
@Service
public class EcrireBdService {

    private final LectureAiService lectureAiService;
    private final OcrMappingService ocrMappingService;
    private final FridaIdentifierService fridaIdentifierService;
    private final HeirPartCalculatorService heirPartCalculatorService;

    private final IdentitesRepo identitesRepo;
    private final FridaRepo fridaRepo;
    private final HeritierRepo heritierRepo;
    private final DefuntRepo defuntRepo;
    private final CalculRepo calculRepo;
    private final TemoinRepo temoinRepo;

    @Value("${MAX_PARALLEL_FOLDERS:2}")
    private int maxParallelFolders;

    private Semaphore maxConcurrentFoldersSemaphore;

    @PostConstruct
    public void init() {
        log.info("Initialisation du Semaphore pour les dossiers parallèles. Max: {}", maxParallelFolders);
        maxConcurrentFoldersSemaphore = new Semaphore(maxParallelFolders);
    }

    public EcrireBdService(
            LectureAiService lectureAiService,
            OcrMappingService ocrMappingService,
            FridaIdentifierService fridaIdentifierService,
            HeirPartCalculatorService heirPartCalculatorService,
            IdentitesRepo identitesRepo,
            FridaRepo fridaRepo,
            HeritierRepo heritierRepo,
            DefuntRepo defuntRepo,
            CalculRepo calculRepo,
            TemoinRepo temoinRepo) {
        this.lectureAiService = lectureAiService;
        this.ocrMappingService = ocrMappingService;
        this.fridaIdentifierService = fridaIdentifierService;
        this.heirPartCalculatorService = heirPartCalculatorService;
        this.identitesRepo = identitesRepo;
        this.fridaRepo = fridaRepo;
        this.heritierRepo = heritierRepo;
        this.defuntRepo = defuntRepo;
        this.calculRepo = calculRepo;
        this.temoinRepo = temoinRepo;
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

            LectureAiService.FolderScanResult scanResult = lectureAiService.listFolderContents(folderPath);
            TraitementContext ctx = initialiserContext(scanResult);

            Map<Path, DocumentInfo> fileDocInfoMap = scanResult.getFileDocumentInfoMap();
            Map<String, OcrEntityDefinitionDto> entityDefCache = new HashMap<>();
            List<Path> files = scanResult.getPdfFiles();

            if (files.isEmpty()) {
                log.warn("Aucun document trouvé dans le dossier : {}", folderPath);
                marquerDossierCommeTraite(Paths.get(folderPath));
                return null;
            }

            int indiceParente = 0;
            for (Path file : files) {
                try {
                    indiceParente = traiterFichier(ctx, file, fileDocInfoMap,
                            entityDefCache, indiceParente, mode);
                } catch (Exception e) {
                    log.error("Erreur traitement fichier OCR : {} - {}", file, e.getMessage(), e);
                }
            }

            if (indiceParente > 0) {
                finaliserEtSauvegarder(ctx);
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

    /**
     * Phase 1 : Analyse des parents/enfants pour déterminer la composition.
     * Cette méthode traite les documents f1 à f4, génère une fiche Frida en base,
     * et retourne un DTO contenant l'état de la composition.
     */
    @org.springframework.transaction.annotation.Transactional
    public CompositionAnalysisDto analyzeComposition(String folderPath, String mode) {
        FridaEntity frida = traiterExtraitsNaissance(folderPath, mode);
        if (frida == null || frida.getCalcul() == null) {
            return CompositionAnalysisDto.builder().build();
        }
        
        CalculEntity calc = frida.getCalcul();
        boolean hasBoy = (calc.getNbGarcons() != null && calc.getNbGarcons() > 0);
        boolean hasFather = (calc.getNumerateurPere() != null && calc.getNumerateurPere() > 0);
        
        int sumNumerators = 0;
        sumNumerators += calc.getNumerateurConjoint() != null ? calc.getNumerateurConjoint() : 0;
        sumNumerators += calc.getNumerateurFilles() != null ? calc.getNumerateurFilles() : 0;
        sumNumerators += calc.getNumerateurGarcons() != null ? calc.getNumerateurGarcons() : 0;
        sumNumerators += calc.getNumerateurPere() != null ? calc.getNumerateurPere() : 0;
        sumNumerators += calc.getNumerateurMere() != null ? calc.getNumerateurMere() : 0;
        sumNumerators += calc.getNumerateurFreres() != null ? calc.getNumerateurFreres() : 0;
        sumNumerators += calc.getNumerateurSoeurs() != null ? calc.getNumerateurSoeurs() : 0;

        int denom = calc.getDenominateur() != null && calc.getDenominateur() > 0 ? calc.getDenominateur() : 1;
        boolean hasRemainingPart = sumNumerators < denom;
        String remainingPartDetails = (denom - sumNumerators) + "/" + denom;

        return CompositionAnalysisDto.builder()
                .numFrida(frida.getNumFrida())
                .hasBoy(hasBoy)
                .hasFather(hasFather)
                .hasRemainingPart(hasRemainingPart)
                .remainingPartDetails(remainingPartDetails)
                .build();
    }

    /**
     * Phase 2 : Mise à jour d'une fiche Frida existante avec de nouveaux documents (f5, f6, temoins).
     * Ne re-traite que les documents appartenant aux catégories 5, 6, 11.
     */
    @org.springframework.transaction.annotation.Transactional
    public FridaEntity updateFrida(String folderPath, String mode, String numFrida) {
        try {
            maxConcurrentFoldersSemaphore.acquire();
            log.info("Début mise à jour dossier: {}", folderPath);

            FridaEntity frida = fridaRepo.findByNumFrida(numFrida)
                    .orElseThrow(() -> new RuntimeException("Frida non trouvée: " + numFrida));

            LectureAiService.FolderScanResult scanResult = lectureAiService.listFolderContents(folderPath);
            TraitementContext ctx = TraitementContext.reconstruireContexte(frida);
            // Ensure lists are initialized
            if (ctx.getTableauNumParente() == null) {
                ctx.setTableauNumParente(scanResult.getTableauNumParente());
            }

            Map<Path, DocumentInfo> fileDocInfoMap = scanResult.getFileDocumentInfoMap();
            Map<String, OcrEntityDefinitionDto> entityDefCache = new HashMap<>();
            List<Path> files = scanResult.getPdfFiles();

            int processedCount = 0;
            for (Path file : files) {
                DocumentInfo docInfo = fileDocInfoMap.get(file);
                if (docInfo == null) continue;
                
                HeirCategory cat = docInfo.getHeirCategory();
                // On ne traite que les frères/soeurs (5), autres (6), ou témoins (11)
                if (cat == HeirCategory.FRATRIE || cat == HeirCategory.AUTRE || cat == HeirCategory.TEMOIN) {
                    // Trouver l'indice de parenté
                    String numParente = String.valueOf(cat.getCode());
                    int indiceParente = ctx.getTableauNumParente().indexOf(numParente);
                    if (indiceParente == -1) {
                        ctx.getTableauNumParente().add(numParente);
                        indiceParente = ctx.getTableauNumParente().size() - 1;
                    }
                    
                    try {
                        int newIndice = traiterFichier(ctx, file, fileDocInfoMap, entityDefCache, indiceParente, mode);
                        if (newIndice > indiceParente) {
                            processedCount++;
                        }
                    } catch (Exception e) {
                        log.error("Erreur mise à jour fichier OCR : {} - {}", file, e.getMessage(), e);
                    }
                }
            }

            if (processedCount > 0) {
                // Re-calculer les parts et sauvegarder
                CalculEntity calcul = heirPartCalculatorService.calculerParts(ctx);
                calculRepo.save(calcul);
                frida.setCalcul(calcul);
                
                for (HeritierEntity heritier : ctx.getListeHeritiers()) {
                    int numerateur = determinerNumerateur(heritier, calcul);
                    float coef = heirPartCalculatorService.calculerCoefficient(numerateur, calcul.getDenominateur());
                    heritier.setCoefPart(coef);
                }
                frida.setHeritiers(ctx.getListeHeritiers());
                frida.setTemoins(ctx.getListeTemoins());
                fridaRepo.save(frida);
            }

            return frida;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Le traitement a été interrompu : {}", e.getMessage(), e);
            return null;
        } catch (IOException e) {
            log.error("Erreur lecture fichiers pour mise à jour : {}", e.getMessage(), e);
            return null;
        } finally {
            maxConcurrentFoldersSemaphore.release();
            log.info("Fin mise à jour dossier: {}", folderPath);
        }
    }

    // ======================= Traitement d'un fichier =======================

    /**
     * Traite un fichier individuel : OCR → mapping → sauvegarde.
     *
     * @return L'indice de parenté mis à jour (incrémenté si traitement réussi).
     */
    private int traiterFichier(TraitementContext ctx, Path file,
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

        log.info("Traitement: {} -> type={}, catégorie={}", file.getFileName(), docType, heirCategory);

        // OCR + mapping
        IdentitesEntity identite = ocrMappingService.processFile(file, entityDef, docType, mode);

        if (identite != null) {
            // Générer l'identifiant au premier document (défunt)
            if (indiceParente == 0 && heirCategory == HeirCategory.DEFUNT) {
                String dateNaissance = identite.getDateNaissance() != null
                        ? identite.getDateNaissance().toString() : "";
                ctx.setNumFrida(fridaIdentifierService.genererIdentifiant(dateNaissance));
            }

            sauvegarderDocument(ctx, identite, heirCategory, indiceParente);
            return indiceParente + 1;
        }

        return indiceParente;
    }

    // ======================= Initialisation =======================

    private TraitementContext initialiserContext(LectureAiService.FolderScanResult scanResult) {
        TraitementContext ctx = new TraitementContext();
        ctx.setTableauNumParente(scanResult.getTableauNumParente());
        return ctx;
    }

    // ======================= Sauvegarde =======================

    /**
     * Sauvegarde l'identité et crée l'entité appropriée (défunt, héritier, témoin).
     */
    private void sauvegarderDocument(TraitementContext ctx, IdentitesEntity identite,
                                     HeirCategory heirCategory, int indiceParente) {
        identite.setNumFrida(ctx.getNumFrida());
        identitesRepo.save(identite);

        // Si l'identité nécessite une correction, marquer toute la Frida comme nécessitant correction
        if (identite.getRequiresCorrection() != null && identite.getRequiresCorrection()) {
            ctx.getFicheFrida().setRequiresCorrection(true);
        }

        switch (heirCategory) {
            case DEFUNT -> {
                DefuntEntity defunt = creerDefunt(ctx, identite);
                defuntRepo.save(defunt);
                ctx.getFicheFrida().setNumFrida(ctx.getNumFrida());
                ctx.getFicheFrida().setDefunt(defunt);
            }
            case TEMOIN -> {
                TemoinEntity temoin = creerTemoin(ctx, identite, indiceParente);
                temoinRepo.save(temoin);
                ctx.getListeTemoins().add(temoin);
            }
            default -> {
                HeritierEntity heritier = creerHeritier(ctx, identite, indiceParente);
                heritierRepo.save(heritier);
                ctx.getListeHeritiers().add(heritier);
            }
        }
    }

    // ======================= Création d'entités =======================

    private DefuntEntity creerDefunt(TraitementContext ctx, IdentitesEntity identite) {
        DefuntEntity defunt = new DefuntEntity();
        defunt.setNumFrida(ctx.getNumFrida());
        defunt.setIdentite(identite);
        return defunt;
    }

    private HeritierEntity creerHeritier(TraitementContext ctx, IdentitesEntity identite,
                                         int indiceParente) {
        HeritierEntity heritier = new HeritierEntity();
        heritier.setNumFrida(ctx.getNumFrida());
        heritier.setNumParente(ctx.getTableauNumParente().get(indiceParente));
        heritier.setIdentite(identite);

        // Comptage par catégorie
        String numParente = heritier.getNumParente();
        String sexe = identite.getSexe();

        switch (numParente) {
            case "2" -> ctx.incrementConjoints();
            case "3" -> {
                if ("ذكر".equals(sexe)) {
                    ctx.incrementGarcons();
                } else {
                    ctx.incrementFilles();
                }
            }
            case "4" -> ctx.incrementParents(sexe);
            case "5" -> {
                if ("ذكر".equals(sexe)) {
                    ctx.incrementFreres();
                } else {
                    ctx.incrementSoeurs();
                }
            }
        }

        return heritier;
    }

    private TemoinEntity creerTemoin(TraitementContext ctx, IdentitesEntity identite,
                                     int indiceParente) {
        TemoinEntity temoin = new TemoinEntity();
        temoin.setNumFrida(ctx.getNumFrida());
        temoin.setIdentite(identite);
        temoin.setNumParente(ctx.getTableauNumParente().get(indiceParente));
        return temoin;
    }

    // ======================= Finalisation =======================

    /**
     * Calcule les parts, attribue les coefficients, et persiste la fiche Frida.
     */
    private void finaliserEtSauvegarder(TraitementContext ctx) {
        // Calculer les parts via le microservice
        CalculEntity calcul = heirPartCalculatorService.calculerParts(ctx);
        calculRepo.save(calcul);

        // Attribuer le coefficient à chaque héritier
        for (HeritierEntity heritier : ctx.getListeHeritiers()) {
            int numerateur = determinerNumerateur(heritier, calcul);
            float coef = heirPartCalculatorService.calculerCoefficient(
                    numerateur, calcul.getDenominateur());
            heritier.setCoefPart(coef);
        }

        // Constituer et sauvegarder la fiche Frida
        FridaEntity ficheFrida = ctx.getFicheFrida();
        ficheFrida.setDateCreation(LocalDate.now());
        ficheFrida.setNotaire("محمد قثوم الموثق بالجزاىر شارع الانتصار،"); // TODO: rendre configurable
        ficheFrida.setCalcul(calcul);
        ficheFrida.setHeritiers(ctx.getListeHeritiers());
        ficheFrida.setTemoins(ctx.getListeTemoins());
        fridaRepo.save(ficheFrida);
    }

    /**
     * Détermine le numérateur de la part d'un héritier selon sa catégorie.
     */
    private int determinerNumerateur(HeritierEntity heritier, CalculEntity calcul) {
        return switch (heritier.getNumParente()) {
            case "2" -> calcul.getNumerateurConjoint();
            case "3" -> Objects.equals(heritier.getIdentite().getSexe(), "ذكر")
                    ? calcul.getNumerateurGarcons()
                    : calcul.getNumerateurFilles();
            case "4" -> Objects.equals(heritier.getIdentite().getSexe(), "ذكر")
                    ? (calcul.getNumerateurPere() != null ? calcul.getNumerateurPere() : 0)
                    : (calcul.getNumerateurMere() != null ? calcul.getNumerateurMere() : 0);
            case "5" -> Objects.equals(heritier.getIdentite().getSexe(), "ذكر")
                    ? (calcul.getNumerateurFreres() != null ? calcul.getNumerateurFreres() : 0)
                    : (calcul.getNumerateurSoeurs() != null ? calcul.getNumerateurSoeurs() : 0);
            default -> 0;
        };
    }

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
