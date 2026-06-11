package com.muhend.backendai.calculs.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Sexe {
    MASCULIN("M"),
    FEMININ("F");

    private final String code;

    Sexe(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static Sexe fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("Le sexe du défunt ne peut pas être null");
        }

        String normalizedCode = code.trim().toUpperCase();

        for (Sexe sexe : Sexe.values()) {
            if (sexe.code.equals(normalizedCode)) {
                return sexe;
            }
        }

        // Supporter aussi les noms complets
        if ("MASCULIN".equals(normalizedCode) || "HOMME".equals(normalizedCode)) {
            return MASCULIN;
        }
        if ("FEMININ".equals(normalizedCode) || "FEMME".equals(normalizedCode)) {
            return FEMININ;
        }

        throw new IllegalArgumentException(
            "Sexe invalide: '" + code + "'. Valeurs acceptées: M, F, Masculin, Feminin"
        );
    }

    @Override
    public String toString() {
        return code;
    }
}
