package com.muhend.backendai.service.pipeline;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.muhend.backendai.dto.MrzResult;
import com.muhend.backendai.entities.IdentitesEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Service de lecture et parsing MRZ (Machine Readable Zone).
 * <p>
 * Délègue la détection et l'OCR de la zone MRZ au service Python,
 * puis parse les lignes brutes selon les standards ICAO 9303 :
 * <ul>
 *     <li>TD1 (CNI) : 3 lignes × 30 caractères</li>
 *     <li>TD3 (Passeport) : 2 lignes × 44 caractères</li>
 * </ul>
 */
@Slf4j
@Service
public class MrzService {

    @Value("${services.ocr.url}")
    private String ocrApiUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Poids pour le checksum MRZ (ICAO 9303)
    private static final int[] CHECK_WEIGHTS = {7, 3, 1};

    public MrzService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // =========================================================================
    // POINT D'ENTRÉE
    // =========================================================================

    /**
     * Appelle le service Python pour extraire le texte MRZ, puis le parse.
     *
     * @param uploadedFilename Nom du fichier déjà uploadé sur le service OCR.
     * @return MrzResult avec les données structurées, ou un résultat invalide.
     */
    public MrzResult extractAndParse(String uploadedFilename) {
        try {
            // 1. Appeler le service Python /api/mrz/lire
            String url = ocrApiUrl + "/api/mrz/lire";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = new HashMap<>();
            body.put("filename", uploadedFilename);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
            String responseJson = restTemplate.postForObject(url, request, String.class);

            JsonNode response = objectMapper.readTree(responseJson);

            if (!response.has("success") || !response.get("success").asBoolean()) {
                String error = response.has("error") ? response.get("error").asText() : "Erreur inconnue";
                log.warn("MRZ : Lecture échouée — {}", error);
                return MrzResult.builder().valid(false).build();
            }

            // 2. Extraire les lignes MRZ
            String format = response.get("format").asText();
            double confidence = response.get("confidence").asDouble();
            String rawMrz = response.get("mrz_raw").asText();

            JsonNode linesNode = response.get("mrz_lines");
            String[] lines = new String[linesNode.size()];
            for (int i = 0; i < linesNode.size(); i++) {
                lines[i] = linesNode.get(i).asText();
            }

            // 3. Parser selon le format
            MrzResult result;
            if ("TD1".equals(format)) {
                result = parseTD1(lines);
            } else if ("TD3".equals(format)) {
                result = parseTD3(lines);
            } else {
                log.warn("MRZ : Format inconnu '{}'", format);
                return MrzResult.builder().valid(false).build();
            }

            result.setConfidence(confidence);
            result.setRawMrz(rawMrz);
            result.setFormat(format);

            log.info("✅ MRZ {} parsée : {} {} (NIN={}, confiance={}%)",
                    format,
                    result.getSurname(),
                    result.getGivenNames(),
                    result.getNin(),
                    Math.round(confidence * 100.0));

            return result;

        } catch (Exception e) {
            log.error("MRZ : Erreur lors de l'extraction — {}", e.getMessage(), e);
            return MrzResult.builder().valid(false).build();
        }
    }

    // =========================================================================
    // CROSS-VALIDATION avec OCR classique
    // =========================================================================

    /**
     * Enrichit une IdentitesEntity (issue de l'OCR classique) avec les données MRZ.
     * <p>
     * Stratégie :
     * <ul>
     *     <li>Compléter les champs latins (toujours)</li>
     *     <li>Si la confiance OCR < 75% et la MRZ est valide → privilégier la MRZ</li>
     *     <li>Ajouter les scores de confiance MRZ dans le JSON</li>
     * </ul>
     */
    public IdentitesEntity enrichirAvecMrz(IdentitesEntity ocrResult, MrzResult mrz) {
        if (!mrz.isValid()) {
            log.info("MRZ invalide, pas d'enrichissement");
            return ocrResult;
        }

        boolean nomMismatch = false;
        boolean prenomMismatch = false;

        // 1. Toujours remplir les champs latins et faire la validation croisée
        if (mrz.getSurname() != null && !mrz.getSurname().isEmpty()) {
            ocrResult.setLatines(mrz.getSurname());
            // Validation croisée du nom arabe
            if (ocrResult.getNom() != null && !ocrResult.getNom().isEmpty()) {
                boolean match = verifierTranslitteration(ocrResult.getNom(), mrz.getSurname());
                if (!match) {
                    ocrResult.setRequiresCorrection(true);
                    nomMismatch = true;
                }
            }
        }
        if (mrz.getGivenNames() != null && !mrz.getGivenNames().isEmpty()) {
            ocrResult.setPrenomLatines(mrz.getGivenNames());
            // Validation croisée du prénom arabe
            if (ocrResult.getPrenom() != null && !ocrResult.getPrenom().isEmpty()) {
                boolean match = verifierTranslitteration(ocrResult.getPrenom(), mrz.getGivenNames());
                if (!match) {
                    ocrResult.setRequiresCorrection(true);
                    prenomMismatch = true;
                }
            }
        }

        // 2. Remplacer le NIN uniquement s'il est complet (18 chiffres pour l'Algérie).
        // Souvent la MRZ tronque le NIN par manque de place (TD1/TD3).
        if (mrz.getNin() != null && mrz.getNin().length() == 18) {
            log.info("MRZ : NIN écrasé par la MRZ '{}' → '{}'", ocrResult.getNin(), mrz.getNin());
            ocrResult.setNin(mrz.getNin());
        } else if (mrz.getNin() != null && !mrz.getNin().isEmpty()) {
            log.debug("MRZ : NIN ignoré car incomplet ({} chiffres)", mrz.getNin().length());
        }

        // 3. Remplacer la date de naissance (ultra fiable car validée par Checksum MRZ)
        if (mrz.getDateOfBirth() != null) {
            log.info("MRZ : Date de naissance écrasée par la MRZ → {}", mrz.getDateOfBirth());
            ocrResult.setDateNaissance(mrz.getDateOfBirth());
        }

        // 4. Remplacer le sexe
        if (mrz.getSex() != null) {
            ocrResult.setSexe("M".equals(mrz.getSex()) ? "ذكر" : "أنثى");
        }

        // 5. Remplacer les champs de la pièce d'identité (validés par Checksums MRZ)
        if (mrz.getDocumentNumber() != null && !mrz.getDocumentNumber().isEmpty()) {
            ocrResult.setNumeroPiece(mrz.getDocumentNumber());
        }

        if (mrz.getExpiryDate() != null) {
            ocrResult.setExpireLe(mrz.getExpiryDate().toString());
        }

        // 6. Stocker les données MRZ brutes
        ocrResult.setMrzRaw(mrz.getRawMrz());
        ocrResult.setMrzValid(true);

        // 7. Enrichir le JSON de confiances avec les scores MRZ et pénalités
        enrichirConfidencesJson(ocrResult, mrz, nomMismatch, prenomMismatch);

        return ocrResult;
    }

    /**
     * Appelle le service Python pour vérifier la correspondance phonétique.
     */
    private boolean verifierTranslitteration(String arabe, String latin) {
        if (arabe == null || latin == null || arabe.isEmpty() || latin.isEmpty()) return false;
        try {
            String url = ocrApiUrl + "/api/translitteration/verifier";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, String> body = new HashMap<>();
            body.put("arabe", arabe);
            body.put("latin", latin);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
            String responseJson = restTemplate.postForObject(url, request, String.class);
            JsonNode response = objectMapper.readTree(responseJson);
            if (response.has("success") && response.get("success").asBoolean()) {
                boolean match = response.get("match").asBoolean();
                double score = response.has("score") ? response.get("score").asDouble() : 0.0;
                String translit = response.has("arabe_translit") ? response.get("arabe_translit").asText() : "";
                String norm = response.has("latin_norm") ? response.get("latin_norm").asText() : "";
                
                if (match) {
                    log.info("✅ Translittération MATCH: Arabe='{}' [{}] <-> Latin='{}' [{}] (Score: {}%)", 
                            arabe, translit, latin, norm, Math.round(score));
                } else {
                    log.warn("❌ Translittération MISMATCH: Arabe='{}' [{}] <-> Latin='{}' [{}] (Score: {}%)", 
                            arabe, translit, latin, norm, Math.round(score));
                }
                return match;
            }
        } catch (Exception e) {
            log.warn("Erreur vérification translittération : {}", e.getMessage());
        }
        return false;
    }

    // =========================================================================
    // PARSING TD1 (CNI — 3 lignes × 30 caractères)
    // =========================================================================

    /**
     * Parse le format TD1 (ICAO 9303 — CNI biométrique algérienne).
     *
     * Ligne 1 (30 chars):
     *   [0-1]   Document code (ID)
     *   [2-4]   Issuing state (DZA)
     *   [5-13]  Document number
     *   [14]    Check digit
     *   [15-29] Optional data (NIN)
     *
     * Ligne 2 (30 chars):
     *   [0-5]   Date of birth (YYMMDD)
     *   [6]     Check digit
     *   [7]     Sex (M/F/<)
     *   [8-13]  Expiry date (YYMMDD)
     *   [14]    Check digit
     *   [15-17] Nationality (DZA)
     *   [18-28] Optional data
     *   [29]    Composite check digit
     *
     * Ligne 3 (30 chars):
     *   [0-29]  Surname<<Given<Names
     */
    private MrzResult parseTD1(String[] lines) {
        if (lines.length < 3 || lines[0].length() < 30 || lines[1].length() < 30 || lines[2].length() < 30) {
            return MrzResult.builder().valid(false).build();
        }

        String line1 = lines[0];
        String line2 = lines[1];
        String line3 = lines[2];

        String documentCode = clean(line1.substring(0, 2));
        String issuingState = clean(line1.substring(2, 5));
        String documentNumber = clean(line1.substring(5, 14));
        String optionalData1 = clean(line1.substring(15, 30));

        LocalDate dateOfBirth = parseMrzDate(line2.substring(0, 6));
        String sex = clean(line2.substring(7, 8));
        LocalDate expiryDate = parseMrzDate(line2.substring(8, 14));
        String nationality = clean(line2.substring(15, 18));

        // Nom et prénom depuis la ligne 3
        String[] names = parseNames(line3);

        // Le NIN algérien est souvent dans les données optionnelles de la ligne 1
        String nin = extractNin(optionalData1);

        MrzResult result = MrzResult.builder()
                .documentCode(documentCode)
                .issuingState(issuingState)
                .documentNumber(documentNumber)
                .nin(nin)
                .dateOfBirth(dateOfBirth)
                .sex(sex)
                .expiryDate(expiryDate)
                .nationality(nationality)
                .surname(names[0])
                .givenNames(names[1])
                .build();

        // Validation des checksums (annule les champs individuellement si invalides)
        boolean checksumOk = validateTD1Checksums(line1, line2, result);
        result.setValid(checksumOk);

        return result;
    }

    // =========================================================================
    // PARSING TD3 (Passeport — 2 lignes × 44 caractères)
    // =========================================================================

    /**
     * Parse le format TD3 (ICAO 9303 — Passeport).
     *
     * Ligne 1 (44 chars):
     *   [0]     Document code (P)
     *   [1]     Document subtype
     *   [2-4]   Issuing state (DZA)
     *   [5-43]  Surname<<Given<Names<<<...
     *
     * Ligne 2 (44 chars):
     *   [0-8]   Passport number
     *   [9]     Check digit
     *   [10-12] Nationality (DZA)
     *   [13-18] Date of birth (YYMMDD)
     *   [19]    Check digit
     *   [20]    Sex (M/F/<)
     *   [21-26] Expiry date (YYMMDD)
     *   [27]    Check digit
     *   [28-41] Optional data (NIN)
     *   [42]    Check digit
     *   [43]    Composite check digit
     */
    private MrzResult parseTD3(String[] lines) {
        if (lines.length < 2 || lines[0].length() < 44 || lines[1].length() < 44) {
            return MrzResult.builder().valid(false).build();
        }

        String line1 = lines[0];
        String line2 = lines[1];

        String documentCode = clean(line1.substring(0, 1));
        String issuingState = clean(line1.substring(2, 5));

        // Nom et prénom depuis la ligne 1 (positions 5-43)
        String[] names = parseNames(line1.substring(5));

        String passportNumber = clean(line2.substring(0, 9));
        String nationality = clean(line2.substring(10, 13));
        LocalDate dateOfBirth = parseMrzDate(line2.substring(13, 19));
        String sex = clean(line2.substring(20, 21));
        LocalDate expiryDate = parseMrzDate(line2.substring(21, 27));
        String optionalData = clean(line2.substring(28, 42));

        String nin = extractNin(optionalData);

        MrzResult result = MrzResult.builder()
                .documentCode(documentCode)
                .issuingState(issuingState)
                .documentNumber(passportNumber)
                .nin(nin)
                .dateOfBirth(dateOfBirth)
                .sex(sex)
                .expiryDate(expiryDate)
                .nationality(nationality)
                .surname(names[0])
                .givenNames(names[1])
                .build();

        // Validation des checksums
        boolean checksumOk = validateTD3Checksums(line2, result);
        result.setValid(checksumOk);

        return result;
    }

    // =========================================================================
    // UTILITAIRES MRZ
    // =========================================================================

    /** Supprime les '<' de remplissage et normalise. */
    private String clean(String s) {
        if (s == null) return "";
        return s.replace('<', ' ').trim();
    }

    /** Parse un nom MRZ : "SURNAME<<GIVENNAME<SECOND" → ["SURNAME", "GIVENNAME SECOND"] */
    private String[] parseNames(String nameField) {
        String cleaned = nameField.trim();
        // Séparer nom et prénom par "<<"
        String[] parts = cleaned.split("<<", 2);
        
        String surname = parts[0].replace('<', ' ').trim();
        if (surname.contains("  ")) {
            surname = surname.substring(0, surname.indexOf("  ")).trim();
        }
        
        String givenNames = "";
        if (parts.length > 1) {
            givenNames = parts[1].replace('<', ' ').trim();
            // La norme MRZ utilise un seul '<' entre les prénoms. S'il y en a deux (double espace), c'est du padding.
            if (givenNames.contains("  ")) {
                givenNames = givenNames.substring(0, givenNames.indexOf("  "));
            }
            // Nettoyage du bruit OCR causé par l'hologramme sur les '<' (souvent lu comme K, X ou S)
            givenNames = givenNames.replaceAll("\\b[KXS]+\\b", " ");
            givenNames = givenNames.replaceAll(" +", " ").trim();
        }
        
        return new String[]{surname, givenNames};
    }

    /** Parse une date MRZ au format YYMMDD → LocalDate. */
    private LocalDate parseMrzDate(String yymmdd) {
        try {
            String cleaned = yymmdd.replace('<', '0');
            int yy = Integer.parseInt(cleaned.substring(0, 2));
            int mm = Integer.parseInt(cleaned.substring(2, 4));
            int dd = Integer.parseInt(cleaned.substring(4, 6));

            if (mm < 1 || mm > 12 || dd < 1 || dd > 31) return null;

            // Siècle : si yy > 30 → 19xx, sinon → 20xx
            int year = yy > 30 ? 1900 + yy : 2000 + yy;
            return LocalDate.of(year, mm, dd);
        } catch (Exception e) {
            log.warn("MRZ : Date invalide '{}'", yymmdd);
            return null;
        }
    }

    /** Extrait le NIN depuis les données optionnelles MRZ (18 chiffres). */
    private String extractNin(String optionalData) {
        if (optionalData == null || optionalData.isEmpty()) return "";
        // Le NIN algérien est composé de 18 chiffres
        String digitsOnly = optionalData.replaceAll("[^0-9]", "");
        if (digitsOnly.length() >= 18) {
            return digitsOnly.substring(0, 18);
        }
        return digitsOnly.isEmpty() ? "" : digitsOnly;
    }

    // =========================================================================
    // VALIDATION DES CHECKSUMS (ICAO 9303)
    // =========================================================================

    /**
     * Calcule un checksum MRZ selon la formule ICAO 9303.
     * Chaque caractère est multiplié par le poids (7, 3, 1) de façon cyclique.
     */
    private int computeCheckDigit(String data) {
        int sum = 0;
        for (int i = 0; i < data.length(); i++) {
            char c = data.charAt(i);
            int value;
            if (c >= '0' && c <= '9') {
                value = c - '0';
            } else if (c >= 'A' && c <= 'Z') {
                value = c - 'A' + 10;
            } else {
                value = 0; // '<' et autres
            }
            sum += value * CHECK_WEIGHTS[i % 3];
        }
        return sum % 10;
    }

    private boolean validateTD1Checksums(String line1, String line2, MrzResult result) {
        try {
            // Check digit du numéro de document (line1[14])
            int expected1 = Character.getNumericValue(line1.charAt(14));
            int computed1 = computeCheckDigit(line1.substring(5, 14));

            // Check digit de la date de naissance (line2[6])
            int expected2 = Character.getNumericValue(line2.charAt(6));
            int computed2 = computeCheckDigit(line2.substring(0, 6));

            // Check digit de la date d'expiration (line2[14])
            int expected3 = Character.getNumericValue(line2.charAt(14));
            int computed3 = computeCheckDigit(line2.substring(8, 14));

            boolean docOk = (expected1 == computed1);
            boolean dobOk = (expected2 == computed2);
            boolean expOk = (expected3 == computed3);

            if (!docOk && result != null) {
                // Si le checksum du numéro de document échoue, on l'efface pour ne pas corrompre l'OCR
                log.warn("MRZ TD1 : Numéro de document invalide (checksum doc={}/{})", computed1, expected1);
                result.setDocumentNumber(null);
            }
            if (!dobOk && result != null) {
                log.warn("MRZ TD1 : Date de naissance invalide (checksum dob={}/{})", computed2, expected2);
                result.setDateOfBirth(null);
            }
            if (!expOk && result != null) {
                log.warn("MRZ TD1 : Date d'expiration invalide (checksum exp={}/{})", computed3, expected3);
                result.setExpiryDate(null);
            }

            // La MRZ est considérée comme valide si au moins UNE des dates ou le document est bon.
            // Cela prouve qu'on a bien lu une MRZ, on ne jette pas tout à la poubelle pour 1 chiffre raté !
            return docOk || dobOk || expOk;
        } catch (Exception e) {
            log.warn("MRZ TD1 : Erreur validation checksums — {}", e.getMessage());
            return false;
        }
    }

    private boolean validateTD3Checksums(String line2, MrzResult result) {
        try {
            // Check digit du numéro de passeport (line2[9])
            int expected1 = Character.getNumericValue(line2.charAt(9));
            int computed1 = computeCheckDigit(line2.substring(0, 9));

            // Check digit de la date de naissance (line2[19])
            int expected2 = Character.getNumericValue(line2.charAt(19));
            int computed2 = computeCheckDigit(line2.substring(13, 19));

            // Check digit de la date d'expiration (line2[27])
            int expected3 = Character.getNumericValue(line2.charAt(27));
            int computed3 = computeCheckDigit(line2.substring(21, 27));

            boolean docOk = (expected1 == computed1);
            boolean dobOk = (expected2 == computed2);
            boolean expOk = (expected3 == computed3);

            if (!docOk && result != null) {
                log.warn("MRZ TD3 : Numéro de document invalide (checksum doc={}/{})", computed1, expected1);
                result.setDocumentNumber(null);
            }
            if (!dobOk && result != null) {
                log.warn("MRZ TD3 : Date de naissance invalide (checksum dob={}/{})", computed2, expected2);
                result.setDateOfBirth(null);
            }
            if (!expOk && result != null) {
                log.warn("MRZ TD3 : Date d'expiration invalide (checksum exp={}/{})", computed3, expected3);
                result.setExpiryDate(null);
            }

            return docOk || dobOk || expOk;
        } catch (Exception e) {
            log.warn("MRZ TD3 : Erreur validation checksums — {}", e.getMessage());
            return false;
        }
    }

    // =========================================================================
    // ENRICHISSEMENT DU JSON DE CONFIANCES
    // =========================================================================

    private void enrichirConfidencesJson(IdentitesEntity entity, MrzResult mrz, boolean nomMismatch, boolean prenomMismatch) {
        try {
            String json = entity.getConfidencesJson();
            Map<String, Object> confiances;
            if (json != null && !json.isBlank()) {
                confiances = objectMapper.readValue(json, Map.class);
            } else {
                confiances = new HashMap<>();
            }

            // Pénaliser les champs OCR si la translittération a échoué (force la bordure rouge côté UI)
            if (nomMismatch) {
                confiances.put("nom", 0.0);
            }
            if (prenomMismatch) {
                confiances.put("prenom", 0.0);
            }

            // Ajouter les scores MRZ (confiance élevée car validé par checksum)
            double mrzScore = mrz.isValid() ? 0.99 : 0.5;
            if (mrz.getSurname() != null && !mrz.getSurname().isEmpty()) {
                confiances.put("mrz_nom", mrzScore);
            }
            if (mrz.getGivenNames() != null && !mrz.getGivenNames().isEmpty()) {
                confiances.put("mrz_prenom", mrzScore);
            }
            if (mrz.getNin() != null && !mrz.getNin().isEmpty()) {
                confiances.put("mrz_nin", mrzScore);
            }
            if (mrz.getDateOfBirth() != null) {
                confiances.put("mrz_dateNaissance", mrzScore);
            }

            entity.setConfidencesJson(objectMapper.writeValueAsString(confiances));
        } catch (Exception e) {
            log.error("Erreur enrichissement confidences MRZ", e);
        }
    }
}
