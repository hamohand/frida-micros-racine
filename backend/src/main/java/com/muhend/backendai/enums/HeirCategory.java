package com.muhend.backendai.enums;

import lombok.Getter;

/**
 * Catégories d'héritiers et leur code.
 */
@Getter
public enum HeirCategory {
    TEMOIN(0),
    DEFUNT(1),
    CONJOINT(2),
    ENFANT(3),
    PARENT(4),
    FRATRIE(5),
    ONCLE_PATERNEL(6),
    COUSIN_PATERNEL(7),
    GRAND_PERE_PATERNEL(8);

    private final int code;

    HeirCategory(int code) {
        this.code = code;
    }

    /**
     * Retourne le code formaté sur 2 chiffres (ex: 1 -> "01", 0 -> "00")
     */
    public String getFormattedCode() {
        return String.format("%02d", code);
    }

    /**
     * Trouve la catégorie à partir de son code numérique.
     */
    public static HeirCategory fromCode(int code) {
        for (HeirCategory category : values()) {
            if (category.code == code) {
                return category;
            }
        }
        throw new IllegalArgumentException("Code de catégorie inconnu: " + code);
    }

    /**
     * Trouve la catégorie à partir d'une chaîne (ex: "2" -> CONJOINT).
     */
    public static HeirCategory fromString(String codeStr) {
        try {
            return fromCode(Integer.parseInt(codeStr));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Code de catégorie invalide: " + codeStr);
        }
    }
}
