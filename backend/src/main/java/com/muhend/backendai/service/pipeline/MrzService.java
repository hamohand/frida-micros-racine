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

            log.info("✅ MRZ {} parsée : {} {} (NIN={}, confiance={:.0%})",
                    format,
                    result.getSurname(),
                    result.getGivenNames(),
                    result.getNin(),
                    confidence);

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

        // 1. Toujours remplir les champs latins
        if (mrz.getSurname() != null && !mrz.getSurname().isEmpty()) {
            ocrResult.setLatines(mrz.getSurname());
        }
        if (mrz.getGivenNames() != null && !mrz.getGivenNames().isEmpty()) {
            ocrResult.setPrenomLatines(mrz.getGivenNames());
        }

        // 2. Enrichir le NIN si vide ou plus long depuis la MRZ
        if (mrz.getNin() != null && !mrz.getNin().isEmpty()) {
            String ocrNin = ocrResult.getNin();
            if (ocrNin == null || ocrNin.isEmpty() || ocrNin.length() < mrz.getNin().length()) {
                log.info("MRZ : NIN enrichi '{}' → '{}'", ocrNin, mrz.getNin());
                ocrResult.setNin(mrz.getNin());
            }
        }

        // 3. Compléter la date de naissance si manquante
        if (ocrResult.getDateNaissance() == null && mrz.getDateOfBirth() != null) {
            log.info("MRZ : Date de naissance complétée → {}", mrz.getDateOfBirth());
            ocrResult.setDateNaissance(mrz.getDateOfBirth());
        }

        // 4. Compléter le sexe si vide
        if ((ocrResult.getSexe() == null || ocrResult.getSexe().isEmpty()) && mrz.getSex() != null) {
            ocrResult.setSexe("M".equals(mrz.getSex()) ? "ذكر" : "أنثى");
        }

        // 5. Compléter les champs de la pièce d'identité
        if (mrz.getDocumentNumber() != null && !mrz.getDocumentNumber().isEmpty()) {
            String ocrNumero = ocrResult.getNumeroPiece();
            if (ocrNumero == null || ocrNumero.isEmpty()) {
                ocrResult.setNumeroPiece(mrz.getDocumentNumber());
            }
        }

        if (mrz.getExpiryDate() != null) {
            String ocrExpire = ocrResult.getExpireLe();
            if (ocrExpire == null || ocrExpire.isEmpty()) {
                ocrResult.setExpireLe(mrz.getExpiryDate().toString());
            }
        }

        // 6. Stocker les données MRZ brutes
        ocrResult.setMrzRaw(mrz.getRawMrz());
        ocrResult.setMrzValid(true);

        // 7. Enrichir le JSON de confiances avec les scores MRZ
        enrichirConfidencesJson(ocrResult, mrz);

        return ocrResult;
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

        // Validation des checksums
        boolean checksumOk = validateTD1Checksums(line1, line2);

        return MrzResult.builder()
                .valid(checksumOk)
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

        // Validation des checksums
        boolean checksumOk = validateTD3Checksums(line2);

        return MrzResult.builder()
                .valid(checksumOk)
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
        String givenNames = parts.length > 1 ? parts[1].replace('<', ' ').trim() : "";
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

    private boolean validateTD1Checksums(String line1, String line2) {
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

            boolean ok = (expected1 == computed1 && expected2 == computed2 && expected3 == computed3);
            if (!ok) {
                log.warn("MRZ TD1 : Checksums invalides (doc={}/{}, dob={}/{}, exp={}/{})",
                        computed1, expected1, computed2, expected2, computed3, expected3);
            }
            return ok;
        } catch (Exception e) {
            log.warn("MRZ TD1 : Erreur validation checksums — {}", e.getMessage());
            return false;
        }
    }

    private boolean validateTD3Checksums(String line2) {
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

            boolean ok = (expected1 == computed1 && expected2 == computed2 && expected3 == computed3);
            if (!ok) {
                log.warn("MRZ TD3 : Checksums invalides (pp={}/{}, dob={}/{}, exp={}/{})",
                        computed1, expected1, computed2, expected2, computed3, expected3);
            }
            return ok;
        } catch (Exception e) {
            log.warn("MRZ TD3 : Erreur validation checksums — {}", e.getMessage());
            return false;
        }
    }

    // =========================================================================
    // ENRICHISSEMENT DU JSON DE CONFIANCES
    // =========================================================================

    private void enrichirConfidencesJson(IdentitesEntity entity, MrzResult mrz) {
        try {
            String json = entity.getConfidencesJson();
            Map<String, Object> confiances;
            if (json != null && !json.isBlank()) {
                confiances = objectMapper.readValue(json, Map.class);
            } else {
                confiances = new HashMap<>();
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
