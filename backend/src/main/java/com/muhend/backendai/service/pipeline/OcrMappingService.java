package com.muhend.backendai.service.pipeline;

import com.muhend.backendai.client.ocr.OcrApiClient;
import com.muhend.backendai.client.ocr.dto.*;
import com.muhend.backendai.dto.MrzResult;
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

    @Value("${services.ocr.mode:rapide}")
    private String defaultOcrMode;

    public OcrMappingService(OcrApiClient ocrApiClient, MrzService mrzService) {
        this.ocrApiClient = ocrApiClient;
        this.mrzService = mrzService;
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
                    log.info("📋 Pas de verso fourni pour la CNI, MRZ ignorée");
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
                    }
                }
            } catch (Exception e) {
                log.warn("⚠️ Erreur lecture MRZ (non bloquant) : {}", e.getMessage());
                result.setMrzValid(false);
            }
        }

        return result;
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
        entity.setNin(getText.apply("nin"));
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
            confiances.put(entry.getKey(), score);
            rawTexts.put(entry.getKey(), res.getTexte_final() != null ? res.getTexte_final() : "");
            
            if (score < SEUIL || "faible_confiance".equals(res.getStatut()) || "echec".equals(res.getStatut())) {
                hasLowConfidence = true;
            }
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
        entity.setNin(getText.apply("nin"));
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
            confiances.put(entry.getKey(), score);
            rawTexts.put(entry.getKey(), res.getTexte_final() != null ? res.getTexte_final() : "");
            
            if (score < SEUIL || "faible_confiance".equals(res.getStatut()) || "echec".equals(res.getStatut())) {
                hasLowConfidence = true;
            }
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
