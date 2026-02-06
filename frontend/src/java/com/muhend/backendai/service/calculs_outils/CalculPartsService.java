package com.muhend.backendai.service.calculs_outils;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor @Data
public class CalculPartsService {
    // les attributs
    private final int nbFilles;
    private final int nbGarcons;
    private final int nbConjoints;

    // la loi dit : constantes
    private final int NUMERATEUR_CONJOINT = 1;
    private final int NUMERATEUR_ENFANTS = 7;
    private final int DENOMINATEUR = 8;
    private final int COEF_FILLES = 1;
    private final int COEF_GARCONS = 2;

    //les variables : initialisations
    private int numerateurEnfants = NUMERATEUR_ENFANTS; //
    private int numerateurConjoint = NUMERATEUR_CONJOINT; // pas de conjoint
    private int numerateurFille = 0; // pas de filles
    private int numerateurGarcon = 0; // pas de garçons
    private int denominateur = DENOMINATEUR; //
    private int coefFilles = COEF_FILLES;
    private int coefGarcons = COEF_GARCONS;

    //constructeur
    public CalculPartsService(int nbFilles, int nbGarcons, int nbConjoints) {
        this.nbFilles = nbFilles;
        this.nbGarcons = nbGarcons;
        this.nbConjoints = nbConjoints;
    }

    public Map<String, Integer> getFractions() {
        return fractions;
    }

    //fractions
    private Map<String, Integer> fractions = new HashMap<>();

    public void calculPartsEnfants() {
        fractions.put("nbGarcons", nbGarcons);
        fractions.put("nbFilles", nbFilles);
        fractions.put("nbConjoints", nbConjoints);

        //Nombre total de parts des enfant : fille= 1 part, garçon= 2 parts
        int nbTotalPartsEnfants = (nbGarcons *2) + nbFilles;
        if(nbFilles == 0) { // pas de filles
            nbTotalPartsEnfants = nbGarcons;
            coefGarcons = 1;
        }

        // Existence du conjoint ?
        if(nbConjoints == 0) { // pas de conjoint
            numerateurConjoint = 0;
            numerateurEnfants = 1;
            denominateur = 1;
        } else {
            if (Math.floorMod(nbTotalPartsEnfants, numerateurEnfants) == 0) { // si multiple de 7
                nbTotalPartsEnfants = Math.floorDiv(nbTotalPartsEnfants, numerateurEnfants);
                numerateurEnfants = 1;
            }
            if (nbTotalPartsEnfants != 0){ //conjoint + enfants
                numerateurConjoint = nbTotalPartsEnfants;
            } else { // conjoint mais pas d'enfants
                numerateurConjoint = 1;
            }

        }

        // Fraction  ****
        //numérateur
        if(nbFilles != 0) {
            numerateurFille = numerateurEnfants * coefFilles;
        }
        if(nbGarcons != 0) {
            numerateurGarcon = numerateurEnfants * coefGarcons;
        }

        //dénominateur différent de zéro
        if (nbTotalPartsEnfants == 0 && nbConjoints==0 ){ // pas d'enfants
            denominateur = -1;
        } else if (nbTotalPartsEnfants != 0){
            denominateur = nbTotalPartsEnfants * denominateur;
        }

        //
        fractions.put("numerateurFille", numerateurFille);
        fractions.put("numerateurGarcon", numerateurGarcon);
        fractions.put("numerateurConjoint", numerateurConjoint);
        fractions.put("denominateur", denominateur);

        System.out.println("nbConjoints" + nbConjoints);

        System.out.println("nbFilles : " + nbFilles);
        System.out.println("nbGarcons : " + nbGarcons);
        System.out.println("nbTotalPartsEnfants : " + nbTotalPartsEnfants);

        System.out.println("fraction fille: " + fractions.get("numerateurFille")+"/"+fractions.get("denominateur"));
        System.out.println("fraction garcon: " + fractions.get("numerateurGarcon")+"/"+fractions.get("denominateur"));
        System.out.println("fraction conjoint: " + fractions.get("numerateurConjoint")+"/"+fractions.get("denominateur"));

        // Parts : facteur multiplicatif = nombre réel pour les futurs calculs des parts d'un bien (somme d'argent par exemple)
        float partFille = (float) numerateurFille / denominateur;
        float partGarcon = (float) numerateurGarcon / denominateur;
        float partConjoint = (float) numerateurConjoint / denominateur;
    }

}
