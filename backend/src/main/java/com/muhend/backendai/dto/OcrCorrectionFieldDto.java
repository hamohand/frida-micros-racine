package com.muhend.backendai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Représente un champ OCR suspect (confiance < seuil) à soumettre à la correction manuelle.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OcrCorrectionFieldDto {
    /** Identifiant de la personne concernée (null = défunt) */
    private Long personneId;
    /** Rôle lisible : "Défunt", "Conjoint", "Fils", "Père"... */
    private String personneLabel;
    /** Nom lu par l'OCR (pour identification) */
    private String personneNom;
    /** Prénom lu par l'OCR (pour identification) */
    private String personnePrenom;
    /** NIN lu par l'OCR (pour identification) */
    private String personneNin;
    /** Clé du champ OCR, ex : "nom", "prenom", "dateNaissance" */
    private String champ;
    /** Libellé lisible du champ, ex : "Nom", "Prénom", "Date de naissance" */
    private String champLabel;
    /** Valeur lue par l'OCR */
    private String valeurOcr;
    /** Valeur de référence (ex: nom latin de la MRZ) pour aider à la correction */
    private String valeurReference;
    /** Score de confiance OCR (0.0 à 1.0) */
    private Double confiance;
    /** numParente de l'héritier, null pour le défunt */
    private String numParente;
    
    /** Indique si ce champ est suspect (nécessite correction) */
    private boolean isSuspect;
    /** Raison pour laquelle il a été validé ou rejeté (ex: "Score OCR élevé", "Phonétique OK") */
    private String validationReason;
}
