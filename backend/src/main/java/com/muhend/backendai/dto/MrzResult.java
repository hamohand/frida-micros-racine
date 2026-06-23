package com.muhend.backendai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Résultat du parsing MRZ (Machine Readable Zone).
 * Supporte les formats TD1 (CNI, 3×30) et TD3 (Passeport, 2×44).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MrzResult {

    /** MRZ lue et parsée avec succès */
    private boolean valid;

    /** Format détecté : "TD1" (CNI) ou "TD3" (Passeport) */
    private String format;

    /** Code du document (ex: "ID", "P") */
    private String documentCode;

    /** État émetteur (ex: "DZA") */
    private String issuingState;

    /** Numéro du document */
    private String documentNumber;

    /** NIN (Numéro d'Identification Nationale) — extrait du champ optionnel */
    private String nin;

    /** Date de naissance */
    private LocalDate dateOfBirth;

    /** Sexe : "M" ou "F" */
    private String sex;

    /** Date d'expiration */
    private LocalDate expiryDate;

    /** Nationalité (ex: "DZA") */
    private String nationality;

    /** Nom de famille en caractères latins */
    private String surname;

    /** Prénom(s) en caractères latins */
    private String givenNames;

    /** Score de confiance de la lecture MRZ (0.0 - 1.0) */
    private double confidence;

    /** Les lignes MRZ brutes concaténées */
    private String rawMrz;
}
