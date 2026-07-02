package com.muhend.backendai.service.pipeline;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

/**
 * Service dédié à la validation et au nettoyage du Numéro d'Identification National (NIN).
 */
@Slf4j
@Service
public class NinValidationService {

    /**
     * Nettoie et valide un NIN extrait par l'OCR.
     * @param rawNin Le NIN brut extrait par l'OCR
     * @return Le NIN nettoyé s'il est valide (exactement 18 chiffres), sinon null.
     */
    public String cleanAndValidate(String rawNin) {
        if (rawNin == null || rawNin.trim().isEmpty()) {
            return null;
        }

        // 1. Suppression des espaces et caractères parasites courants
        String cleaned = rawNin.replaceAll("[\\s\\-_.]", "").toUpperCase();

        // 2. Correction des erreurs OCR classiques (O -> 0, I/l -> 1, S -> 5, B -> 8)
        cleaned = cleaned.replace('O', '0')
                         .replace('Q', '0')
                         .replace('I', '1')
                         .replace('L', '1')
                         .replace('S', '5')
                         .replace('Z', '2')
                         .replace('B', '8');

        // 3. Vérification du format strict (18 chiffres)
        if (cleaned.matches("^\\d{18}$")) {
            log.info("NIN OCR validé et nettoyé: {}", cleaned);
            return cleaned;
        } else {
            log.warn("NIN OCR invalide après nettoyage: '{}' (Longueur: {})", cleaned, cleaned.length());
            return null; // Format invalide
        }
    }
}
