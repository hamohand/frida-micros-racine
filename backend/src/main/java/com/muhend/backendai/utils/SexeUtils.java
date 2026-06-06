package com.muhend.backendai.utils;

public class SexeUtils {
    public static boolean isMasculin(String sexe) {
        if (sexe == null) return false;
        String s = sexe.trim();
        return "ذكر".equals(s) || "ذ".equals(s) || "M".equalsIgnoreCase(s);
    }

    public static boolean isFeminin(String sexe) {
        if (sexe == null) return false;
        String s = sexe.trim();
        return "أنثى".equals(s) || "انثى".equals(s) || "ا".equals(s) || "أ".equals(s) || "F".equalsIgnoreCase(s);
    }
}
