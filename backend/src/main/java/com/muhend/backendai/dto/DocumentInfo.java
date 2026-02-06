package com.muhend.backendai.dto;

import com.muhend.backendai.enums.DocumentType;
import com.muhend.backendai.enums.HeirCategory;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Information sur un document : catégorie d'héritier et type de pièce.
 */
@Data
@AllArgsConstructor
public class DocumentInfo {
    private HeirCategory heirCategory;
    private DocumentType documentType;

    /**
     * Parse un nom de dossier au format "{code}_{type}".
     * Exemple: "2_cni" -> DocumentInfo(CONJOINT, CNI)
     */
    public static DocumentInfo fromFolderName(String folderName) {
        if (folderName == null || !folderName.contains("_")) {
            throw new IllegalArgumentException("Format de dossier invalide: " + folderName);
        }
        String[] parts = folderName.split("_", 2);
        if (parts.length < 2) {
            throw new IllegalArgumentException("Format de dossier invalide: " + folderName);
        }

        HeirCategory category = HeirCategory.fromString(parts[0]);
        DocumentType docType = DocumentType.fromFolderSuffix(parts[1]);

        return new DocumentInfo(category, docType);
    }
}
