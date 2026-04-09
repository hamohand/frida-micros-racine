package com.muhend.backendai.service.aibd;

import com.muhend.backendai.client.ocr.OcrApiClient;
import com.muhend.backendai.client.ocr.dto.*;
import com.muhend.backendai.entities.IdentitesEntity;
import com.muhend.backendai.enums.DocumentType;
import com.muhend.backendai.service.calculs_outils.MethodesChaine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Service responsable de l'interaction avec le microservice OCR
 * et du mapping des résultats vers les entités JPA.
 */
@Slf4j
@Service
public class OcrMappingService {

    private final OcrApiClient ocrApiClient;

    public OcrMappingService(OcrApiClient ocrApiClient) {
        this.ocrApiClient = ocrApiClient;
    }

    /**
     * Récupère la définition d'entité OCR depuis le cache ou l'API.
     *
     * @param cache   Cache local des définitions déjà chargées.
     * @param docType Type de document à traiter.
     * @return La définition d'entité, ou {@code null} si introuvable.
     */
    public OcrEntityDefinitionDto getOrCacheEntityDef(
            Map<String, OcrEntityDefinitionDto> cache, DocumentType docType) {
        String ocrEntityId = docType.getOcrEntityId();
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
     * @param file      Fichier à analyser.
     * @param entityDef Définition de l'entité OCR.
     * @param docType   Type de document.
     * @return L'entité IdentitesEntity peuplée, ou {@code null} en cas d'erreur.
     */
    public IdentitesEntity processFile(Path file, OcrEntityDefinitionDto entityDef, DocumentType docType) {
        // 1. Upload du fichier
        OcrUploadResponseDto uploadResponse = ocrApiClient.uploadFile(file);
        if (!uploadResponse.isSuccess()) {
            throw new RuntimeException("Echec upload OCR: " + uploadResponse.getError());
        }

        // 2. Préparer les zones d'analyse
        Map<String, OcrZoneConfigDto> zones = buildZoneConfig(entityDef);

        OcrAnalysisRequestDto request = OcrAnalysisRequestDto.builder()
                .filename(uploadResponse.getSaved_filename())
                .zones(zones)
                .cadre_reference(entityDef.getCadre_reference())
                .build();

        // 3. Analyser
        OcrAnalysisResponseDto response = ocrApiClient.analyze(request);
        logOcrResponse(request.getFilename(), response);

        if (!response.isSuccess()) {
            throw new RuntimeException("Echec analyse OCR: " + response.getError());
        }

        // 4. Mapper selon le type de document
        return switch (docType) {
            case EXTRAIT_NAISSANCE -> mapExtraitNaissance(response);
            case CNI, PASSEPORT -> mapPieceIdentite(response, docType);
        };
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
        entity.setNom((prenom + " " + nom).trim());
        entity.setPrenom(prenom);
        entity.setSexe(getText.apply("sexe"));
        entity.setLieuNaissance(getText.apply("lieuNaissance"));
        entity.setDateNaissanceLettres(getText.apply("dateNaissanceLettres"));
        entity.setPere(getText.apply("pere"));
        entity.setMere(getText.apply("mere"));

        parseDateNaissance(entity, getText.apply("dateNaissance"));
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
        entity.setLieuNaissance(getText.apply("lieu_naissance"));
        entity.setNin(getText.apply("nin"));
        entity.setSexe(getText.apply("sexe"));
        entity.setNumeroPiece(getText.apply("numero_piece"));
        entity.setDelivrePar(getText.apply("delivre_par"));
        entity.setDelivreLe(getText.apply("delivre_le"));
        entity.setExpireLe(getText.apply("expire_le"));
        entity.setNomPiece(docType == DocumentType.CNI
                ? "Carte Nationale d'Identité" : "Passeport");

        parseDateNaissance(entity, getText.apply("date_naissance"));
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
                    .valeurs_attendues(z.getValeurs_attendues())
                    .build());
        }
        return zones;
    }

    private void parseDateNaissance(IdentitesEntity entity, String dateTxt) {
        try {
            java.time.LocalDate parsedDate = MethodesChaine.parseOcrDate(dateTxt);
            entity.setDateNaissance(parsedDate != null ? parsedDate : java.time.LocalDate.now());
        } catch (Exception e) {
            log.warn("Impossible de parser la date de naissance '{}', utilisation de la date du jour.", dateTxt);
            entity.setDateNaissance(java.time.LocalDate.now());
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
