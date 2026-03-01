package com.muhend.backendai.service.calculs_outils;

public class MethodesChaine {

    public static String replaceTrimSpaces(String chaine, char caractere) {
        // Remplace tous les points par des espaces
        String replaced = chaine.replace(caractere, ' ');
        // Supprime les espaces en trop (plus d'un espace entre les mots) et les espaces
        // en début/fin de chaîne
        return replaced.trim().replaceAll("\\s+", " ");
    }

    // remplace les occurences du caractère par un espace dans la chaine et supprime
    // les espaces en trop
    public static String replaceAllTrimSpaces(String chaine, char caractere) {
        // Remplace tous les points par des espaces
        String replaced = chaine.replace(caractere, ' ');
        // Supprime les espaces en trop (plus d'un espace entre les mots) et les espaces
        // en début/fin de chaîne
        return replaced.trim().replaceAll("\\s+", "");
    }

    /**
     * Extrait les séquences de caractères séparées par des astérisques (*).
     * 
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

    /**
     * Tente de parser une date extraite par l'OCR en LocalDate.
     * Nettoie la chaîne et teste plusieurs formats.
     *
     * @param dateOcrExtraite La chaîne de date issue de l'OCR
     * @return LocalDate si parsing réussi, null sinon
     */
    public static java.time.LocalDate parseOcrDate(String dateOcrExtraite) {
        if (dateOcrExtraite == null || dateOcrExtraite.trim().isEmpty()) {
            return null;
        }

        // 1. Nettoyage potentiel (enlever les espaces, O au lieu de 0, etc.)
        String dateNettoyee = dateOcrExtraite.trim().replace(" ", "")
                .replace("O", "0")
                .replace("o", "0");

        // Remplacer certains séparateurs communs par des slashes
        dateNettoyee = dateNettoyee.replace("-", "/").replace(".", "/");

        // 2. Définition des formats attendus
        java.time.format.DateTimeFormatter[] formatters = {
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd"),
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yy")
        };

        // 3. Essai de conversion
        for (java.time.format.DateTimeFormatter formatter : formatters) {
            try {
                return java.time.LocalDate.parse(dateNettoyee, formatter);
            } catch (java.time.format.DateTimeParseException e) {
                // On ignore et on essaie le format suivant
            }
        }

        // L'OCR s'est probablement trompé ou le format est inconnu
        System.err.println("Erreur de parsing OCR pour la date : " + dateOcrExtraite);
        return null;
    }
}
