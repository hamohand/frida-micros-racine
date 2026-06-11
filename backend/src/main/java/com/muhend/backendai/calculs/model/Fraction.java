package com.muhend.backendai.calculs.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Fraction {
    /**
     * Objet mathématique, avec les opérations de base.
     */
    private int numerateur;
    private int denominateur;

    public Fraction() {
    }


    public Fraction(int numerateur, int denominateur) { // avec simplification de la fraction
        if (denominateur == 0) {
            throw new IllegalArgumentException("Le dénominateur ne peut pas être nul.");
        }
        this.numerateur = numerateur;
        this.denominateur = denominateur;
            reduire(); // On réduit la fraction dès l'instanciation

    }
    public Fraction(int numerateur, int denominateur, boolean nonReduire) {
        if (denominateur == 0) {
            throw new IllegalArgumentException("Le dénominateur ne peut pas être nul.");
        }
        this.numerateur = numerateur;
        this.denominateur = denominateur;
        if (!nonReduire) {
            reduire(); // On réduit la fraction dès l'instanciation
        }
    }
    public Fraction(int numerateur) {
        this(numerateur, 1);
    }

    // Addition de deux fractions
    public Fraction ajouter(Fraction autre) {
        int num = this.numerateur * autre.denominateur + autre.numerateur * this.denominateur;
        int den = this.denominateur * autre.denominateur;
        return new Fraction(num, den);
    }

    // Soustraction
    public Fraction soustraire(Fraction autre) {
        int num = this.numerateur * autre.denominateur - autre.numerateur * this.denominateur;
        int den = this.denominateur * autre.denominateur;
        return new Fraction(num, den);
    }

    // Multiplication
    public Fraction multiplier(Fraction autre) {
        int num = this.numerateur * autre.numerateur;
        int den = this.denominateur * autre.denominateur;
        return new Fraction(num, den);
    }
    public Fraction multiplier(int nombre){ //multiplier par un nombre
        int num = this.numerateur * nombre;
        return new Fraction(num, this.denominateur);
    }

    // Division
    public Fraction diviser(Fraction autre) {
        if (autre.numerateur == 0) {
            throw new ArithmeticException("Division par zéro impossible.");
        }
        int num = this.numerateur * autre.denominateur;
        int den = this.denominateur * autre.numerateur;
        return new Fraction(num, den);
    }
    public Fraction diviser(int nombre) { //division par un nombre
        if (nombre == 0) {
            throw new ArithmeticException("Division par zéro impossible.");
        }
        int den = this.denominateur * nombre;
        return new Fraction(numerateur, den);
    }

    // Réduction (simplification) de la fraction
    private void reduire() {
        int gcd = calculPGCD(Math.abs(numerateur), Math.abs(denominateur));
        numerateur /= gcd;
        denominateur /= gcd;
        // Gestion du signe pour avoir un dénominateur positif
        if (denominateur < 0) {
            numerateur = -numerateur;
            denominateur = -denominateur;
        }
    }

    //Réduction au même dénominateur
    public static List<Fraction> reduireAuMemDenominateur(List<Fraction> fractions) {
        // Étape 1 : Récupérer tous les dénominateurs
        List<Integer> denominateurs = new ArrayList<>();
        for (Fraction f : fractions) {
            denominateurs.add(f.getDenominateur());
        }
        //log.info("denominateurs : " + denominateurs);

        // Étape 2 : Calculer le plus petit multiple commun (PMC) de tous les dénominateurs
        int acc = 1;
        for (Integer i : denominateurs) {
            acc = calculPPCM(acc, i);
        }
        int pmc = acc;
        //log.info("pmc : " + pmc);

        // Étape 3 : Convertir chaque fraction pour qu'elle ait ce dénominateur commun
        List<Fraction> resultat = new ArrayList<>();
        for (Fraction f : fractions) {
            int facteur = pmc / f.getDenominateur();
            int nouveauNum = f.getNumerateur() * facteur;
            resultat.add(new Fraction(nouveauNum, pmc, true));
        }
        //log.info("- Dénominateur commun : " + pmc);
        // Retourne la liste de fractions modifiées
        return resultat;
    }

    // Calcul du plus grand diviseur commun
    private static int calculPGCD(int a, int b) {
        if (b == 0) {
            return a;
        }
        return calculPGCD(b, a % b);
    }

    /**
     * Calcule le Plus Petit Multiple Commun (PPCM) entre deux entiers
     * en utilisant le PGCD.
     */
    private static int calculPPCM(int a, int b) {
        return a * (b / calculPGCD(a, b));
    }
    // ------------- Fin réduire au même dénominateur -----------------------------------

    @Override
    public String toString() {
        return numerateur + "/" + denominateur;
    }
}
