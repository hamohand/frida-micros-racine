package com.muhend.backendai.service.pipeline;

import com.muhend.backendai.client.ocr.OcrApiClient;
import com.muhend.backendai.client.ocr.dto.*;
import com.muhend.backendai.dto.MrzResult;
import com.muhend.backendai.service.pipeline.MrzService.PhoneticResult;
import com.muhend.backendai.entities.IdentitesEntity;
import com.muhend.backendai.enums.DocumentType;
import com.muhend.backendai.service.calculs_outils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service responsable de l'interaction avec le microservice OCR
 * et du mapping des résultats vers les entités JPA.
 */
@Slf4j
@Service
public class OcrMappingService {

    private final OcrApiClient ocrApiClient;
    private final MrzService mrzService;
    private final NinValidationService ninValidationService;

    @Value("${services.ocr.mode:rapide}")
    private String defaultOcrMode;

    public OcrMappingService(OcrApiClient ocrApiClient, MrzService mrzService, NinValidationService ninValidationService) {
        this.ocrApiClient = ocrApiClient;
        this.mrzService = mrzService;
        this.ninValidationService = ninValidationService;
    }

    /**
     * Récupère la définition d'entité OCR depuis le cache ou l'API.
     *
     * @param cache   Cache local des définitions déjà chargées.
     * @param docType Type de document à traiter.
     * @return La définition d'entité, ou {@code null} si introuvable.
     */
    public OcrEntityDefinitionDto getOrCacheEntityDef(
            Map<String, OcrEntityDefinitionDto> cache, DocumentType docType, String customEntityName) {
        String ocrEntityId = (customEntityName != null && !customEntityName.isEmpty()) 
                ? customEntityName 
                : docType.getOcrEntityId();
        OcrEntityDefinitionDto entityDef = cache.get(ocrEntityId);
        if (entityDef == null) {
            entityDef = ocrApiClient.getEntityDefinition(ocrEntityId);
            if (entityDef != null) {
                cache.put(ocrEntityId, entityDef);
            } else {
                log.error("Entité '{}' introuvable dans le service OCR.", ocrEntityId);
            }
        }
        return entityDef;
    }

    /**
     * Traite un fichier avec le service OCR et retourne l'entité correspondante.
     *
     * @param file      Fichier recto à analyser (OCR zones).
     * @param versoFile Fichier verso optionnel (pour lecture MRZ sur CNI). Peut être null.
     * @param entityDef Définition de l'entité OCR.
     * @param docType   Type de document.
     * @return L'entité IdentitesEntity peuplée, ou {@code null} en cas d'erreur.
     */
    public IdentitesEntity processFile(Path file, Path versoFile, OcrEntityDefinitionDto entityDef, DocumentType docType, String mode) {
        // --- CAS DU DUMP NFC (JSON) ---
        if (file.getFileName().toString().toLowerCase().endsWith(".json")) {
            log.info("📱 Fichier JSON NFC détecté : {}, parsing direct sans OCR.", file.getFileName());
            return parseNfcJson(file, docType);
        }

        // --- CAS CLASSIQUE (IMAGE/PDF -> OCR) ---
        // 1. Upload du fichier recto
        OcrUploadResponseDto uploadResponse = ocrApiClient.uploadFile(file);
        if (!uploadResponse.isSuccess()) {
            throw new RuntimeException("Echec upload OCR: " + uploadResponse.getError());
        }

        // Gestion du mode
        String finalMode = (mode != null && !mode.isEmpty()) ? mode : defaultOcrMode;
        if ("batch".equals(finalMode)) {
            finalMode = "approfondi";
        }

        // 2. Préparer les zones d'analyse
        Map<String, OcrZoneConfigDto> zones = buildZoneConfig(entityDef);

        OcrAnalysisRequestDto request = OcrAnalysisRequestDto.builder()
                .filename(uploadResponse.getSaved_filename())
                .mode(finalMode)
                .zones(zones)
                .cadre_reference(entityDef.getCadre_reference())
                .build();

        // 3. Analyser le recto
        OcrAnalysisResponseDto response = ocrApiClient.analyze(request);
        logOcrResponse(request.getFilename(), response);

        if (!response.isSuccess()) {
            throw new RuntimeException("Echec analyse OCR: " + response.getError());
        }

        // 4. Mapper selon le type de document
        IdentitesEntity result = switch (docType) {
            case EXTRAIT_NAISSANCE -> mapExtraitNaissance(response);
            case CNI, PASSEPORT -> mapPieceIdentite(response, docType);
        };

        // 5. Lecture MRZ pour les pièces d'identité
        if (docType == DocumentType.CNI || docType == DocumentType.PASSEPORT) {
            try {
                // CNI : MRZ sur le verso (fichier séparé)
                // Passeport : MRZ sur le recto (même fichier)
                String mrzFilename = null;

                if (docType == DocumentType.CNI && versoFile != null) {
                    // Upload du verso pour la MRZ
                    log.info("🔍 Upload du verso CNI pour lecture MRZ : {}", versoFile.getFileName());
                    OcrUploadResponseDto versoUpload = ocrApiClient.uploadFile(versoFile);
                    if (versoUpload.isSuccess()) {
                        mrzFilename = versoUpload.getSaved_filename();
                    } else {
                        log.warn("⚠️ Échec upload verso : {}", versoUpload.getError());
                    }
                } else if (docType == DocumentType.PASSEPORT) {
                    // Le passeport a la MRZ sur le recto (déjà uploadé)
                    mrzFilename = uploadResponse.getSaved_filename();
                    log.info("🔍 Lecture MRZ sur le recto du passeport");
                } else if (docType == DocumentType.CNI) {
                    log.info("📋 Pas de verso fourni séparément pour la CNI. Tentative de lecture de la MRZ sur le fichier principal (cas des Héritiers ou PDF fusionné).");
                    mrzFilename = uploadResponse.getSaved_filename();
                }

                if (mrzFilename != null) {
                    log.info("🔍 Tentative de lecture MRZ pour {} ...", docType);
                    MrzResult mrz = mrzService.extractAndParse(mrzFilename);
                    
                    // Fallback en cas d'inversion Recto/Verso par l'utilisateur
                    if (!mrz.isValid() && docType == DocumentType.CNI && versoFile != null) {
                        log.warn("⚠️ MRZ non trouvée sur le verso désigné. Tentative de secours sur le recto...");
                        mrz = mrzService.extractAndParse(uploadResponse.getSaved_filename());
                    }

                    if (mrz.isValid()) {
                        result = mrzService.enrichirAvecMrz(result, mrz);
                        log.info("✅ MRZ enrichie : {} {} (latins)", mrz.getSurname(), mrz.getGivenNames());
                    } else {
                        log.info("⚠️ MRZ non valide ou non détectée sur les deux faces, utilisation OCR seul");
                        result.setMrzValid(false);
                        
                        // Sécurité : si on a quand même pu extraire le nom/prénom MRZ, on fait le contrôle phonétique !
                        boolean nomMismatch = false;
                        boolean prenomMismatch = false;
                        
                        PhoneticResult resNom = null;
                        PhoneticResult resPrenom = null;
                        
                        if (mrz.getSurname() != null && !mrz.getSurname().isEmpty() && result.getNom() != null && !result.getNom().isEmpty()) {
                            resNom = mrzService.verifierTranslitteration(result.getNom(), mrz.getSurname());
                            if (!resNom.match) {
                                nomMismatch = true;
                                result.setRequiresCorrection(true);
                            }
                        }
                        if (mrz.getGivenNames() != null && !mrz.getGivenNames().isEmpty() && result.getPrenom() != null && !result.getPrenom().isEmpty()) {
                            resPrenom = mrzService.verifierTranslitteration(result.getPrenom(), mrz.getGivenNames());
                            if (!resPrenom.match) {
                                prenomMismatch = true;
                                result.setRequiresCorrection(true);
                            }
                        }
                        
                        if (nomMismatch || prenomMismatch || resNom != null || resPrenom != null) {
                            try {
                                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                                String json = result.getConfidencesJson();
                                Map<String, Object> confiances = new java.util.HashMap<>();
                                if (json != null && !json.isBlank()) {
                                    confiances = mapper.readValue(json, Map.class);
                                }
                                if (nomMismatch) {
                                    confiances.put("nom", 0.0);
                                    log.warn("⚠️ MRZ invalide mais mismatch phonétique sur le NOM détecté !");
                                }
                                if (prenomMismatch) {
                                    confiances.put("prenom", 0.0);
                                    log.warn("⚠️ MRZ invalide mais mismatch phonétique sur le PRÉNOM détecté !");
                                }
                                if (resNom != null) {
                                    confiances.put("phonetic_nom_score", resNom.score);
                                    confiances.put("phonetic_nom_translit", resNom.translit);
                                }
                                if (resPrenom != null) {
                                    confiances.put("phonetic_prenom_score", resPrenom.score);
                                    confiances.put("phonetic_prenom_translit", resPrenom.translit);
                                }
                                result.setConfidencesJson(mapper.writeValueAsString(confiances));
                            } catch (Exception e) {
                                log.error("Erreur forçage confiances phonétique MRZ de secours", e);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("⚠️ Erreur lecture MRZ (non bloquant) : {}", e.getMessage());
                result.setMrzValid(false);
            }
        }

        return result;
    }

    // ============================== Traitement JSON NFC ==============================

    private IdentitesEntity parseNfcJson(Path jsonFile, DocumentType docType) {
        IdentitesEntity entity = new IdentitesEntity();
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode rootNode = mapper.readTree(jsonFile.toFile());

            String nom = rootNode.path("primaryIdentifier").asText("");
            String prenom = rootNode.path("secondaryIdentifier").asText("");
            // Utiliser le NIN extrait par DG11 en priorité, sinon le documentNumber
            String nin = rootNode.path("nin_dg11").asText("");
            if (nin.isEmpty()) {
                nin = rootNode.path("personalNumber").asText("");
            }

            entity.setLatines(nom);
            entity.setPrenomLatines(prenom);
            entity.setNin(nin);
            entity.setSexe(rootNode.path("gender").asText("").startsWith("M") ? "M" : "F");
            entity.setNumeroPiece(rootNode.path("documentNumber").asText(""));
            
            // Les dates dans le MRZ sont au format yyMMdd, il faudrait idéalement les convertir, mais gardons la logique existante :
            parseDateNaissance(entity, rootNode.path("dateOfBirth").asText(""));

            entity.setNomPiece(docType == DocumentType.CNI ? "Carte Nationale d'Identité (NFC)" : "Passeport (NFC)");
            entity.setMrzValid(true);
            
            // On valide le NIN au cas où
            String validatedNin = ninValidationService.cleanAndValidate(nin);
            if (validatedNin != null) {
                entity.setNin(validatedNin);
            } else {
                entity.setRequiresCorrection(true);
            }

            // Mettre 100% de confiance pour tous les champs car ils viennent de la puce sécurisée !
            Map<String, Double> confiances = new HashMap<>();
            confiances.put("nom", 1.0);
            confiances.put("prenom", 1.0);
            confiances.put("nin", validatedNin != null ? 1.0 : 0.0);
            confiances.put("sexe", 1.0);
            confiances.put("dateNaissance", 1.0);
            confiances.put("numeroPiece", 1.0);
            entity.setConfidencesJson(mapper.writeValueAsString(confiances));

        } catch (Exception e) {
            log.error("Erreur lors du parsing du fichier JSON NFC : {}", jsonFile, e);
            entity.setRequiresCorrection(true);
        }
        return entity;
    }

    /**
     * Fusionne les résultats de l'OCR (qui contient l'Arabe) avec les données parfaites du NFC.
     * Effectue également la vérification phonétique entre l'Arabe de l'OCR et le Latin du NFC.
     */
    public IdentitesEntity mergeOcrAndNfc(IdentitesEntity ocrEntity, IdentitesEntity nfcEntity) {
        if (ocrEntity == null) return nfcEntity;
        if (nfcEntity == null) return ocrEntity;

        // 1. Toujours remplir les champs latins depuis le NFC
        ocrEntity.setLatines(nfcEntity.getLatines());
        ocrEntity.setPrenomLatines(nfcEntity.getPrenomLatines());

        // 2. Validation croisée du nom/prénom arabe (Translittération)
        boolean nomMismatch = false;
        boolean prenomMismatch = false;
        PhoneticResult resNom = null;
        PhoneticResult resPrenom = null;

        if (ocrEntity.getNom() != null && !ocrEntity.getNom().isEmpty() && nfcEntity.getLatines() != null && !nfcEntity.getLatines().isEmpty()) {
            resNom = mrzService.verifierTranslitteration(ocrEntity.getNom(), nfcEntity.getLatines());
            if (!resNom.match) {
                ocrEntity.setRequiresCorrection(true);
                nomMismatch = true;
            }
        }
        if (ocrEntity.getPrenom() != null && !ocrEntity.getPrenom().isEmpty() && nfcEntity.getPrenomLatines() != null && !nfcEntity.getPrenomLatines().isEmpty()) {
            resPrenom = mrzService.verifierTranslitteration(ocrEntity.getPrenom(), nfcEntity.getPrenomLatines());
            if (!resPrenom.match) {
                ocrEntity.setRequiresCorrection(true);
                prenomMismatch = true;
            }
        }
        
        // Ajouter les scores phonétiques si on a fait la vérification
        if (nomMismatch || prenomMismatch || resNom != null || resPrenom != null) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                String json = ocrEntity.getConfidencesJson();
                Map<String, Object> confiances = new java.util.HashMap<>();
                if (json != null && !json.isBlank()) {
                    confiances = mapper.readValue(json, Map.class);
                }
                if (resNom != null) {
                    confiances.put("phonetic_nom_score", resNom.score);
                    confiances.put("phonetic_nom_translit", resNom.translit);
                }
                if (resPrenom != null) {
                    confiances.put("phonetic_prenom_score", resPrenom.score);
                    confiances.put("phonetic_prenom_translit", resPrenom.translit);
                }
                ocrEntity.setConfidencesJson(mapper.writeValueAsString(confiances));
            } catch (Exception e) {
                log.error("Erreur ajout confiances phonétiques NFC", e);
            }
        }

        // 3. Remplacer les champs infaillibles du NFC
        if (nfcEntity.getNin() != null && !nfcEntity.getNin().isEmpty()) ocrEntity.setNin(nfcEntity.getNin());
        if (nfcEntity.getDateNaissance() != null) ocrEntity.setDateNaissance(nfcEntity.getDateNaissance());
        if (nfcEntity.getSexe() != null && !nfcEntity.getSexe().isEmpty()) ocrEntity.setSexe(nfcEntity.getSexe());
        if (nfcEntity.getNumeroPiece() != null && !nfcEntity.getNumeroPiece().isEmpty()) ocrEntity.setNumeroPiece(nfcEntity.getNumeroPiece());
        
        ocrEntity.setMrzValid(true); // NFC implies 100% validity of MRZ data

        // 4. Enrichir le JSON de confiances
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String json = ocrEntity.getConfidencesJson();
            Map<String, Object> confiances = new HashMap<>();
            if (json != null && !json.isBlank()) {
                confiances = mapper.readValue(json, Map.class);
            }
            
            if (nomMismatch) confiances.put("nom", 0.0);
            if (prenomMismatch) confiances.put("prenom", 0.0);
            
            // Score NFC (1.0) garantis
            confiances.put("mrz_nom", 1.0);
            confiances.put("mrz_prenom", 1.0);
            confiances.put("mrz_nin", 1.0);
            confiances.put("mrz_dateNaissance", 1.0);
            confiances.put("nin", 1.0);
            
            ocrEntity.setConfidencesJson(mapper.writeValueAsString(confiances));
        } catch(Exception e) {
            log.error("Erreur fusion confidences NFC", e);
        }

        log.info("✅ Fusion OCR+NFC réussie pour {} {}", ocrEntity.getPrenom(), ocrEntity.getNom());
        return ocrEntity;
    }

    // ============================== Mapping ==============================

    /**
     * Mappe un résultat OCR d'extrait de naissance vers une IdentitesEntity.
     */
    private IdentitesEntity mapExtraitNaissance(OcrAnalysisResponseDto response) {
        IdentitesEntity entity = new IdentitesEntity();
        Map<String, OcrResultDto> results = response.getResultats();
        if (results == null) return entity;

        Function<String, String> getText = key ->
                results.containsKey(key) ? results.get(key).getTexte_final() : "";

        String nom = getText.apply("nom");
        String prenom = getText.apply("prenom");
        entity.setNom(nom);
        entity.setPrenom(prenom);
        entity.setSexe(getText.apply("sexe"));
        entity.setLieuNaissance(getText.apply("lieuNaissance"));
        entity.setDateNaissanceLettres(getText.apply("dateNaissanceLettres"));
        entity.setPere(getText.apply("pere"));
        entity.setMere(getText.apply("mere"));
        entity.setNumeroPiece(getText.apply("numeroPiece"));

        parseDateNaissance(entity, getText.apply("dateNaissance"));

        // Vérification du statut + sauvegarde des confiances et textes bruts par champ
        final double SEUIL = 0.75;
        boolean hasLowConfidence = false;
        Map<String, Double> confiances = new HashMap<>();
        Map<String, String> rawTexts = new HashMap<>();

        for (Map.Entry<String, OcrResultDto> entry : results.entrySet()) {
            OcrResultDto res = entry.getValue();
            if (res == null) continue;
            double score = res.getConfiance_auto() != null ? res.getConfiance_auto() : 1.0;
            
            // --- VALIDATION DU NIN ---
            if ("nin".equals(entry.getKey())) {
                String brutNin = res.getTexte_final() != null ? res.getTexte_final() : "";
                String validatedNin = ninValidationService.cleanAndValidate(brutNin);
                
                if (validatedNin != null) {
                    res.setTexte_final(validatedNin);
                    entity.setNin(validatedNin);
                } else if (!brutNin.trim().isEmpty()) {
                    // NIN présent mais format invalide (erreur OCR)
                    score = 0.0; // Force la correction visuelle
                    entity.setNin(brutNin);
                    log.warn("🚨 Extrait: NIN OCR '{}' invalide. Score forcé à 0.0.", brutNin);
                }
            }

            if (score < SEUIL || "faible_confiance".equals(res.getStatut()) || "echec".equals(res.getStatut())) {
                hasLowConfidence = true;
                score = 0.0; // S'assure que le champ apparaîtra dans la fiche de correction
            }
            
            confiances.put(entry.getKey(), score);
            rawTexts.put(entry.getKey(), res.getTexte_final() != null ? res.getTexte_final() : "");
        }

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            entity.setConfidencesJson(mapper.writeValueAsString(confiances));
            entity.setRawOcrTextJson(mapper.writeValueAsString(rawTexts));
        } catch (Exception e) {
            log.error("Erreur sérialisation json ocr", e);
        }
        
        if (hasLowConfidence) {
            entity.setRequiresCorrection(true);
            log.warn("Extrait de Naissance ({} {}) scanné avec confiance faible", prenom, nom);
        }

        return entity;
    }

    /**
     * Mappe un résultat OCR de pièce d'identité (CNI/Passeport) vers une IdentitesEntity.
     */
    private IdentitesEntity mapPieceIdentite(OcrAnalysisResponseDto response, DocumentType docType) {
        IdentitesEntity entity = new IdentitesEntity();
        Map<String, OcrResultDto> results = response.getResultats();
        if (results == null) return entity;

        Function<String, String> getText = key ->
                results.containsKey(key) ? results.get(key).getTexte_final() : "";

        entity.setNom(getText.apply("nom"));
        entity.setPrenom(getText.apply("prenom"));
        entity.setLieuNaissance(getText.apply("lieuNaissance"));
        entity.setSexe(getText.apply("sexe"));
        entity.setNumeroPiece(getText.apply("numeroPiece"));
        entity.setDelivrePar(getText.apply("delivrePar"));
        entity.setDelivreLe(getText.apply("delivreLe"));
        entity.setExpireLe(getText.apply("expireLe"));
        entity.setNomPiece(docType == DocumentType.CNI
                ? "Carte Nationale d'Identité" : "Passeport");

        parseDateNaissance(entity, getText.apply("dateNaissance"));

        // Vérification du statut + sauvegarde des confiances et textes bruts par champ
        final double SEUIL = 0.75;
        boolean hasLowConfidence = false;
        Map<String, Double> confiances = new HashMap<>();
        Map<String, String> rawTexts = new HashMap<>();

        for (Map.Entry<String, OcrResultDto> entry : results.entrySet()) {
            OcrResultDto res = entry.getValue();
            if (res == null) continue;
            double score = res.getConfiance_auto() != null ? res.getConfiance_auto() : 1.0;
            
            // --- VALIDATION DU NIN ---
            if ("nin".equals(entry.getKey())) {
                String brutNin = res.getTexte_final() != null ? res.getTexte_final() : "";
                String validatedNin = ninValidationService.cleanAndValidate(brutNin);
                
                if (validatedNin != null) {
                    res.setTexte_final(validatedNin);
                    entity.setNin(validatedNin);
                } else if (!brutNin.trim().isEmpty()) {
                    // NIN présent mais format invalide (erreur OCR)
                    score = 0.0; // Force la correction visuelle
                    entity.setNin(brutNin);
                    log.warn("🚨 CNI/Passeport: NIN OCR '{}' invalide. Score forcé à 0.0.", brutNin);
                }
            }

            if (score < SEUIL || "faible_confiance".equals(res.getStatut()) || "echec".equals(res.getStatut())) {
                hasLowConfidence = true;
                score = 0.0; // S'assure que le champ apparaîtra dans la fiche de correction
            }

            confiances.put(entry.getKey(), score);
            rawTexts.put(entry.getKey(), res.getTexte_final() != null ? res.getTexte_final() : "");
        }

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            entity.setConfidencesJson(mapper.writeValueAsString(confiances));
            entity.setRawOcrTextJson(mapper.writeValueAsString(rawTexts));
        } catch (Exception e) {
            log.error("Erreur sérialisation json ocr", e);
        }

        if (hasLowConfidence) {
            entity.setRequiresCorrection(true);
            log.warn("Pièce d'Identité scannée avec confiance faible");
        }

        return entity;
    }

    // ============================== Utilitaires ==============================

    private Map<String, OcrZoneConfigDto> buildZoneConfig(OcrEntityDefinitionDto entityDef) {
        if (entityDef == null || entityDef.getZones() == null) {
            throw new RuntimeException("Définition de zones manquante pour l'analyse OCR.");
        }
        Map<String, OcrZoneConfigDto> zones = new HashMap<>();
        for (OcrEntityZoneDto z : entityDef.getZones()) {
            zones.put(z.getNom(), OcrZoneConfigDto.builder()
                    .coords(z.getCoords())
                    .lang(z.getLang())
                    .type(z.getType())
                    .preprocess(z.getPreprocess())
                    .expected_format(z.getExpected_format())
                    .char_filter(z.getChar_filter())
                    .margin(z.getMargin())
                    .valeurs_attendues(z.getValeurs_attendues())
                    .build());
        }
        return zones;
    }

    private void parseDateNaissance(IdentitesEntity entity, String dateTxt) {
        try {
            java.time.LocalDate parsedDate = StringUtils.parseOcrDate(dateTxt);
            entity.setDateNaissance(parsedDate);
        } catch (Exception e) {
            log.warn("Impossible de parser la date de naissance '{}', elle restera vide.", dateTxt);
            entity.setDateNaissance(null);
        }
    }

    private void logOcrResponse(String filename, OcrAnalysisResponseDto response) {
        log.info("====== RÉPONSE DU SERVICE OCR ======");
        log.info("Fichier analysé: {}", filename);
        if (response != null && response.getResultats() != null) {
            response.getResultats().forEach((key, value) ->
                    log.info("Champ: {} -> Valeur: '{}'", key,
                            value != null ? value.getTexte_final() : "null"));
        } else {
            log.warn("Aucun résultat retourné par le service OCR.");
        }
    }
}
