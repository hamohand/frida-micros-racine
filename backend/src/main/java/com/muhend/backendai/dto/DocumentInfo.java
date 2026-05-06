package com.muhend.backendai.dto;

import com.muhend.backendai.enums.DocumentType;
import com.muhend.backendai.enums.HeirCategory;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Information sur un document : catégorie d'héritier et type de pièce.
 */
@Data
public class DocumentInfo {
    private HeirCategory heirCategory;
    private DocumentType documentType;
    private String entityName; // Custom OCR entity name

    public DocumentInfo(HeirCategory heirCategory, DocumentType documentType) {
        this.heirCategory = heirCategory;
        this.documentType = documentType;
        this.entityName = null;
    }

    public DocumentInfo(HeirCategory heirCategory, DocumentType documentType, String entityName) {
        this.heirCategory = heirCategory;
        this.documentType = documentType;
        this.entityName = entityName;
    }

    /**
     * Parse un nom de dossier au format "{code}_{type}" ou "{code}_{type}_{entityName}".
     * Exemple: "2_cni" -> DocumentInfo(CONJOINT, CNI, null)
     * Exemple: "2_cni_cni_algo_recto_01" -> DocumentInfo(CONJOINT, CNI, "cni_algo_recto_01")
     */
    public static DocumentInfo fromFolderName(String folderName) {
        if (folderName == null) {
            throw new IllegalArgumentException("Format de dossier invalide: null");
        }
        folderName = folderName.trim();
        if (!folderName.contains("_")) {
            throw new IllegalArgumentException("Format de dossier invalide: " + folderName);
        }
        
        // Split par le premier underscore pour obtenir le code
        int firstUnderscore = folderName.indexOf('_');
        String codePart = folderName.substring(0, firstUnderscore).trim();
        String rest = folderName.substring(firstUnderscore + 1).trim(); // ex: "cni" ou "cni_cni_algo_recto_01"
        
        HeirCategory category = HeirCategory.fromString(codePart);
        
        // Trouver le DocumentType qui correspond au début de "rest"
        DocumentType docType = null;
        String entityName = null;
        
        for (DocumentType type : DocumentType.values()) {
            if (rest.equals(type.getFolderSuffix())) {
                docType = type;
                break;
            } else if (rest.startsWith(type.getFolderSuffix() + "_")) {
                docType = type;
                entityName = rest.substring(type.getFolderSuffix().length() + 1);
                break;
            }
        }
        
        if (docType == null) {
            throw new IllegalArgumentException("Suffixe de document inconnu dans: " + folderName);
        }

        return new DocumentInfo(category, docType, entityName);
    }
}
