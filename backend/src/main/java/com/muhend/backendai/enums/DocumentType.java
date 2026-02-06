package com.muhend.backendai.enums;

import lombok.Getter;

/**
 * Types de documents d'identité avec leur suffixe de dossier et ID d'entité
 * OCR.
 */
@Getter
public enum DocumentType {
    EXTRAIT_NAISSANCE("en", "en01"),
    CNI("cni", "cni01"),
    PASSEPORT("pp", "pp01");

    private final String folderSuffix;
    private final String ocrEntityId;

    DocumentType(String folderSuffix, String ocrEntityId) {
        this.folderSuffix = folderSuffix;
        this.ocrEntityId = ocrEntityId;
    }

    /**
     * Trouve le type de document à partir du suffixe de dossier.
     */
    public static DocumentType fromFolderSuffix(String suffix) {
        for (DocumentType type : values()) {
            if (type.folderSuffix.equalsIgnoreCase(suffix)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Suffixe de document inconnu: " + suffix);
    }

    /**
     * Parse un nom de dossier au format "{code}_{type}" et retourne le
     * DocumentType.
     * Exemple: "2_cni" -> CNI
     */
    public static DocumentType fromFolderName(String folderName) {
        if (folderName == null || !folderName.contains("_")) {
            throw new IllegalArgumentException("Format de dossier invalide: " + folderName);
        }
        String[] parts = folderName.split("_", 2);
        if (parts.length < 2) {
            throw new IllegalArgumentException("Format de dossier invalide: " + folderName);
        }
        return fromFolderSuffix(parts[1]);
    }
}
