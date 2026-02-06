package com.muhend.backendai.service.calculs_outils;

public class MethodesChaine {

    public static String replaceTrimSpaces(String chaine, char caractere) {
        // Remplace tous les points par des espaces
        String replaced = chaine.replace(caractere, ' ');
        // Supprime les espaces en trop (plus d'un espace entre les mots) et les espaces en début/fin de chaîne
        return replaced.trim().replaceAll("\\s+", " ");
    }

    //remplace les occurences du caractère par un espace dans la chaine et supprime les espaces en trop
    public static String replaceAllTrimSpaces(String chaine, char caractere) {
        // Remplace tous les points par des espaces
        String replaced = chaine.replace(caractere, ' ');
        // Supprime les espaces en trop (plus d'un espace entre les mots) et les espaces en début/fin de chaîne
        return replaced.trim().replaceAll("\\s+", "");
    }

    /**
     * Extrait les séquences de caractères séparées par des astérisques (*).
     * @param input La chaîne contenant les données (ex: "1518*1956*...")
     * @return Une liste de chaînes (séquences)
     */
    public static java.util.List<String> extractSequences(String input) {
        if (input == null || input.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        
        // Split par astérisque
        String[] parts = input.split("\\*");
        
        // Filtrer les chaînes vides (ex: cas des **** successifs ou début/fin *)
        return java.util.Arrays.stream(parts)
                .filter(s -> s != null && !s.isEmpty())
                .collect(java.util.stream.Collectors.toList());
    }
}
