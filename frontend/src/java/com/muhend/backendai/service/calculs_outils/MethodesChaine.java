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
}
