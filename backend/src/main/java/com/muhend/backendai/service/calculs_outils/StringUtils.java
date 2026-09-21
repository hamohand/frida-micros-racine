package com.muhend.backendai.service.calculs_outils;

public class StringUtils {

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
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"),
                java.time.format.DateTimeFormatter.ofPattern("yyMMdd"), // Format MRZ standard
                java.time.format.DateTimeFormatter.ofPattern("ddMMyyyy"),
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yy"),
                new java.time.format.DateTimeFormatterBuilder()
                    .appendPattern("yyyy")
                    .parseDefaulting(java.time.temporal.ChronoField.MONTH_OF_YEAR, 1)
                    .parseDefaulting(java.time.temporal.ChronoField.DAY_OF_MONTH, 1)
                    .toFormatter()
        };

        // 3. Essai de conversion
        java.time.LocalDate date = null;
        for (java.time.format.DateTimeFormatter formatter : formatters) {
            try {
                date = java.time.LocalDate.parse(dateNettoyee, formatter);
                break;
            } catch (java.time.format.DateTimeParseException e) {
                // On ignore et on essaie le format suivant
            }
        }
        
        if (date != null) {
            // Puisqu'il s'agit d'une date de naissance (ou d'un évènement passé),
            // elle ne peut pas être dans le futur.
            // Si l'année extraite est supérieure à l'année actuelle, c'est que l'année à 2 chiffres
            // (ex: 56) a été interprétée par défaut comme 2056 au lieu de 1956.
            if (date.getYear() > java.time.LocalDate.now().getYear()) {
                date = date.minusYears(100);
            } else if (date.getYear() == java.time.LocalDate.now().getYear() && 
                       date.isAfter(java.time.LocalDate.now())) {
                // Si la date est plus tard dans l'année courante, c'est aussi le siècle dernier
                date = date.minusYears(100);
            }
            
            // Si la personne a 0 ans (née l'année en cours) et qu'on traite un défunt ou héritier majeur,
            // il y a de fortes chances que ce soit 1926 et non 2026.
            // Mais pour ne pas complexifier on va s'en tenir à une vérification stricte : si > aujd.
            return date;
        }

        // L'OCR s'est probablement trompé ou le format est inconnu
        System.err.println("Erreur de parsing OCR pour la date : " + dateOcrExtraite);
        return null;
    }
}
