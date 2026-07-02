package com.muhend.backendai.service;

import com.muhend.backendai.dto.FridaDetailsDTO;
import com.muhend.backendai.dto.OcrCorrectionFieldDto;
import com.muhend.backendai.entities.FridaEntity;
import com.muhend.backendai.entities.CalculEntity;
import com.muhend.backendai.entities.HeritierEntity;
import com.muhend.backendai.entities.IdentitesEntity;
import com.muhend.backendai.repository.DefuntRepo;
import com.muhend.backendai.repository.FridaRepo;
import com.muhend.backendai.repository.CalculRepo;
import com.muhend.backendai.repository.HeritierRepo;
import com.muhend.backendai.repository.IdentitesRepo;
import com.muhend.backendai.service.pipeline.HeirPartCalculatorService;
import com.muhend.backendai.service.pipeline.MrzService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.*;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class FridaService {
    private final FridaRepo fridaRepo;
    private final DefuntRepo defuntRepo;
    private final CalculRepo calculRepo;
    private final HeirPartCalculatorService heirPartCalculatorService;
    private final HeritierRepo heritierRepo;
    private final IdentitesRepo identitesRepo;
    private final MrzService mrzService;

    // Seuil de confiance OCR (75%)
    private static final double SEUIL_CONFIANCE = 0.75;

    // Correspondance clé OCR -> libellé français
    private static final Map<String, String> CHAMP_LABELS = Map.of(
        "nom", "Nom",
        "prenom", "Prénom",
        "dateNaissance", "Date de naissance",
        "lieuNaissance", "Lieu de naissance",
        "sexe", "Sexe",
        "nin", "NIN",
        "pere", "Père",
        "mere", "Mère"
    );

    @Autowired
    public FridaService(FridaRepo fridaRepository, DefuntRepo defuntRepo,
                        CalculRepo calculRepo, HeirPartCalculatorService calculatorService,
                        HeritierRepo heritierRepo, IdentitesRepo identitesRepo,
                        MrzService mrzService) {
        this.fridaRepo = fridaRepository;
        this.defuntRepo = defuntRepo;
        this.calculRepo = calculRepo;
        this.heirPartCalculatorService = calculatorService;
        this.heritierRepo = heritierRepo;
        this.identitesRepo = identitesRepo;
        this.mrzService = mrzService;
    }

    // Méthode pour récupérer toutes les fridas
    public List<FridaEntity> getAllFridaEntities() {
        return fridaRepo.findAll();
    }

    // Méthode pour récupérer une fiche par numFrida
    public Optional<FridaEntity> getFridaByNumFrida(String numFrida) {
        return fridaRepo.findByNumFrida(numFrida);
    }

    // Méthode pour récupérer une liste de fridas par nom
    public Optional<List<FridaDetailsDTO>> getFridaByNom(String nom) {
        return Optional.ofNullable(defuntRepo.findByNom(nom));
    }

    // ai
    public List<FridaDetailsDTO> getFridas() {
        return fridaRepo.findAllFridas();
    }

    public List<FridaDetailsDTO> getFridasEnAttente() {
        return fridaRepo.findByStatutIn(java.util.Arrays.asList(FridaEntity.STATUT_EN_ATTENTE, "BROUILLON"));
    }

    public FridaEntity corrigerEtRecalculerFrida(String numFrida, FridaEntity corrections) {
        FridaEntity existing = fridaRepo.findByNumFrida(numFrida)
                .orElseThrow(() -> new RuntimeException("Frida non trouvée"));

        // Copy corrections (Here we do a full overwrite to keep it simple, assuming the UI sends the whole structure)
        // Ensure ID is same to avoid detached entity error
        corrections.setId(existing.getId());
        if (corrections.getDefunt() != null) corrections.getDefunt().setId(existing.getDefunt().getId());
        if (corrections.getCalcul() != null) corrections.getCalcul().setId(existing.getCalcul().getId());

        // Remove the warning state and mark as valid
        corrections.setRequiresCorrection(false);
        corrections.setStatut(FridaEntity.STATUT_VALIDE);

        // Recalculate Parts! Python Call
        try {
            CalculEntity nouveauCalcul = heirPartCalculatorService.recalculerParts(corrections);
            if (existing.getCalcul() != null) {
                nouveauCalcul.setId(existing.getCalcul().getId()); // prevent duplicate row Insert
            }
            corrections.setCalcul(nouveauCalcul);
        } catch (Exception e) {
            log.error("Failed to recalculate parts after correction", e);
            throw new RuntimeException("Erreur de recalcule des parts");
        }

        return fridaRepo.save(corrections);
    }

    /**
     * Retourne la liste des champs OCR dont la confiance est < SEUIL_CONFIANCE
     * pour le défunt et tous les héritiers d'une frida.
     */
    public List<OcrCorrectionFieldDto> getChampsSuspects(String numFrida) {
        List<OcrCorrectionFieldDto> suspects = new ArrayList<>();

        FridaEntity frida = fridaRepo.findByNumFrida(numFrida)
                .orElseThrow(() -> new RuntimeException("Frida introuvable"));

        // --- Défunt ---
        if (frida.getDefunt() != null && frida.getDefunt().getIdentite() != null) {
            IdentitesEntity idDefunt = frida.getDefunt().getIdentite();
            extraireChampsSuspects(idDefunt, "Défunt", null, null, suspects);
        }

        // --- Héritiers ---
        List<HeritierEntity> heritiers = heritierRepo.listeHeritiers(numFrida);
        for (HeritierEntity h : heritiers) {
            if (h.getIdentite() == null) continue;
            String label = getRoleLabel(h.getNumParente());
            extraireChampsSuspects(h.getIdentite(), label, h.getIdentite().getId(), h.getNumParente(), suspects);
        }

        return suspects;
    }

    private String getRoleLabel(String numParente) {
        if (numParente == null) return "Héritier";
        return switch (numParente) {
            case "02" -> "Conjoint";
            case "03" -> "Enfant (Fils/Fille)";
            case "04" -> "Parent (Père/Mère)";
            case "05" -> "Frère / Sœur";
            case "06" -> "Oncle paternel";
            case "07" -> "Cousin paternel";
            case "08" -> "Grand-père paternel";
            default -> "Héritier (" + numParente + ")";
        };
    }

    private void extraireChampsSuspects(IdentitesEntity identite, String label,
                                         Long personneId, String numParente,
                                         List<OcrCorrectionFieldDto> suspects) {
        String json = identite.getConfidencesJson();
        if (json == null || json.isBlank()) return;

        java.util.Map<String, String> rawTexts = new java.util.HashMap<>();
        String rawOcrJson = identite.getRawOcrTextJson();
        if (rawOcrJson != null && !rawOcrJson.isBlank()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                rawTexts = mapper.readValue(rawOcrJson, new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, String>>(){});
            } catch (Exception e) {
                log.warn("Erreur lecture rawOcrTextJson", e);
            }
        }

        java.util.Map<String, Object> confidences = new java.util.HashMap<>();
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            confidences = mapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>(){});
        } catch (Exception e) {
            log.warn("Erreur lecture confidencesJson", e);
        }

        for (java.util.Map.Entry<String, Object> entry : confidences.entrySet()) {
            String champ = entry.getKey();
            
            // On ignore les clés de métadonnées spéciales
            if (champ.startsWith("mrz_") || champ.startsWith("phonetic_")) continue;
            
            double score = 0.0;
            if (entry.getValue() instanceof Number) {
                score = ((Number) entry.getValue()).doubleValue();
            } else {
                continue;
            }

            // Valeurs par défaut
            boolean isSuspect = score < SEUIL_CONFIANCE;
            String reason = isSuspect ? "Faible confiance OCR (" + Math.round(score * 100) + "%)" : "Confiance OCR élevée (" + Math.round(score * 100) + "%)";
            
            if ("nom".equals(champ) || "prenom".equals(champ)) {
                String arabe = "nom".equals(champ) ? identite.getNom() : identite.getPrenom();
                String latin = "nom".equals(champ) ? identite.getLatines() : identite.getPrenomLatines();
                
                boolean hasPhonetic = false;
                double phoneticScore = 0.0;
                String translit = "";
                boolean phoneticMatch = true;
                
                if (confidences.containsKey("phonetic_" + champ + "_score")) {
                    hasPhonetic = true;
                    phoneticScore = ((Number) confidences.get("phonetic_" + champ + "_score")).doubleValue();
                    translit = (String) confidences.get("phonetic_" + champ + "_translit");
                    
                    // Si score OCR == 0.0, le pipeline a forcé le mismatch.
                    if (score == 0.0) {
                        phoneticMatch = false;
                    } 
                    // Si le score phonétique est de 0.0 et que le translit est vide, c'est probablement
                    // une anomalie de l'API (ex: match=True renvoyé à tort sans données). On lève le doute.
                    else if (phoneticScore == 0.0 && (translit == null || translit.isEmpty())) {
                        phoneticMatch = false;
                    } 
                    // Sinon on tente de faire la validation stricte de la première lettre ici 
                    // au cas où le pipeline l'aurait manqué (vieux dossiers).
                    else if (latin != null && !latin.isEmpty() && translit != null && !translit.isEmpty()) {
                        phoneticMatch = Character.toLowerCase(translit.charAt(0)) == Character.toLowerCase(latin.charAt(0));
                    }
                    else {
                        phoneticMatch = true;
                    }
                } else if (arabe != null && !arabe.isEmpty() && latin != null && !latin.isEmpty()) {
                    // Calcul à la volée pour les anciens dossiers
                    MrzService.PhoneticResult res = mrzService.verifierTranslitteration(arabe, latin);
                    hasPhonetic = true;
                    phoneticScore = res.score;
                    translit = res.translit;
                    phoneticMatch = res.match;
                }
                
                if (hasPhonetic) {
                    if (!phoneticMatch || score == 0.0) {
                        isSuspect = true;
                        reason = "Incohérence phonétique / MRZ détectée";
                        reason += String.format(" (Score phonétique %d%%, translit: %s)", Math.round(phoneticScore), translit);
                    } else if (!isSuspect) {
                        reason = "Phonétique validée";
                        reason += String.format(" (Score %d%%, translit: %s)", Math.round(phoneticScore), translit);
                    }
                }
            }

            String valeur = rawTexts.containsKey(champ) ? rawTexts.get(champ) : getChampValeur(identite, champ);
            OcrCorrectionFieldDto dto = new OcrCorrectionFieldDto();
            dto.setPersonneId(personneId);
            dto.setPersonneLabel(label);
            dto.setPersonneNom(identite.getNom());
            dto.setPersonnePrenom(identite.getPrenom());
            dto.setPersonneNin(identite.getNin());
            dto.setChamp(champ);
            dto.setChampLabel(CHAMP_LABELS.getOrDefault(champ, champ));
            dto.setValeurOcr(valeur);
            
            // Assigner la valeur de référence (Latin MRZ) pour les noms/prénoms
            if ("nom".equals(champ) && identite.getLatines() != null && !identite.getLatines().isEmpty()) {
                dto.setValeurReference(identite.getLatines());
            } else if ("prenom".equals(champ) && identite.getPrenomLatines() != null && !identite.getPrenomLatines().isEmpty()) {
                dto.setValeurReference(identite.getPrenomLatines());
            }
            
            dto.setConfiance(score);
            dto.setNumParente(numParente);
            dto.setSuspect(isSuspect);
            dto.setValidationReason(reason);
            suspects.add(dto);
        }
    }

    private String getChampValeur(IdentitesEntity identite, String champ) {
        try {
            Field f = IdentitesEntity.class.getDeclaredField(champ);
            f.setAccessible(true);
            Object val = f.get(identite);
            return val != null ? val.toString() : "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Applique les corrections manuelles OCR champ par champ sur les identités.
     * Chaque entrée est { "personneId": "123", "champ": "prenom", "valeur": "Mohammed" }
     * Si personneId est null/vide -> correction sur le défunt.
     */
    @Transactional
    public void appliquerCorrectionsOcr(String numFrida, List<Map<String, String>> corrections) {
        FridaEntity frida = fridaRepo.findByNumFrida(numFrida)
                .orElseThrow(() -> new RuntimeException("Frida introuvable"));

        List<HeritierEntity> heritiers = heritierRepo.listeHeritiers(numFrida);

        for (Map<String, String> correction : corrections) {
            String personneIdStr = correction.get("personneId");
            String champ = correction.get("champ");
            String valeur = correction.get("valeur");
            if (champ == null || valeur == null) continue;

            IdentitesEntity cible = null;
            if (personneIdStr == null || personneIdStr.isEmpty()) {
                // Correction sur le défunt
                if (frida.getDefunt() != null) cible = frida.getDefunt().getIdentite();
            } else {
                long pid = Long.parseLong(personneIdStr);
                cible = heritiers.stream()
                    .filter(h -> h.getIdentite() != null && pid == h.getIdentite().getId())
                    .map(HeritierEntity::getIdentite)
                    .findFirst().orElse(null);
            }

            if (cible != null) appliquerChamp(cible, champ, valeur);
        }

    // Réinitialiser le flag requiresCorrection et passer le statut à VALIDE
        frida.setRequiresCorrection(false);
        frida.setStatut(FridaEntity.STATUT_VALIDE);
        fridaRepo.save(frida);
    }

    /**
     * Sauvegarde les corrections OCR en tant que brouillon (mise en attente).
     * Ne modifie pas le statut VALIDE et laisse le dossier à corriger.
     */
    @Transactional
    public void mettreEnAttenteOcr(String numFrida, List<Map<String, String>> corrections) {
        FridaEntity frida = fridaRepo.findByNumFrida(numFrida)
                .orElseThrow(() -> new RuntimeException("Frida introuvable"));

        List<HeritierEntity> heritiers = heritierRepo.listeHeritiers(numFrida);

        for (Map<String, String> correction : corrections) {
            String personneIdStr = correction.get("personneId");
            String champ = correction.get("champ");
            String valeur = correction.get("valeur");
            if (champ == null || valeur == null) continue;

            IdentitesEntity cible = null;
            if (personneIdStr == null || personneIdStr.isEmpty()) {
                if (frida.getDefunt() != null) cible = frida.getDefunt().getIdentite();
            } else {
                long pid = Long.parseLong(personneIdStr);
                cible = heritiers.stream()
                    .filter(h -> h.getIdentite() != null && pid == h.getIdentite().getId())
                    .map(HeritierEntity::getIdentite)
                    .findFirst().orElse(null);
            }

            if (cible != null) appliquerChamp(cible, champ, valeur);
        }

        // On marque le dossier comme BROUILLON (en attente de reprise de correction)
        frida.setStatut("BROUILLON");
        fridaRepo.save(frida);
    }

    private void appliquerChamp(IdentitesEntity identite, String champ, String valeur) {
        try {
            Field f = IdentitesEntity.class.getDeclaredField(champ);
            f.setAccessible(true);
            f.set(identite, valeur);
            identitesRepo.save(identite);
        } catch (Exception e) {
            log.warn("Impossible d'appliquer la correction sur le champ '{}'", champ);
        }
    }

    @Transactional
    public boolean deleteFrida(String numFrida) {
        Optional<FridaEntity> fridaOpt = fridaRepo.findByNumFrida(numFrida);
        if (fridaOpt.isPresent()) {
            fridaRepo.delete(fridaOpt.get());
            log.info("Frida supprimée avec succès: {}", numFrida);
            return true;
        }
        log.warn("Tentative de suppression échouée : Frida {} introuvable", numFrida);
        return false;
    }
}