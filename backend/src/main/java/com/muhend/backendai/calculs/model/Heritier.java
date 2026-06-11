package com.muhend.backendai.calculs.model;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
public class Heritier {
    /**
     * heritier : représente un type d'héritier membre de la famille du défunt :
     * conjoint, père, mère, fille, garcon, soeur, frère
     * et une éventuelle part de l'héritage restant non distribuée par
     * l'application.
     * part : fraction représentant l'éventuelle part de l'héritage sinon 0.
     */
    private String heritier;
    
    @Schema(description = "Base de la fraction (ex: de la totalité, du reste)")
    private String baseCalcul;
    
    private Fraction part;
    
    @Schema(description = "Part légale théorique du type d'héritier")
    private Fraction partLegale;
    
    @Schema(description = "Cadre légal du partage en arabe (الفرض, العصبة, العول, الرد)")
    private String cadreLegal;
    
    @Schema(description = "Fraction irréductible représentant la part légale du type d'héritier")
    private Fraction partIrreductible;

    @com.fasterxml.jackson.annotation.JsonProperty("baseCalcul")
    public String getBaseCalcul() {
        return this.baseCalcul;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("cadreLegal")
    public String getCadreLegal() {
        return this.cadreLegal;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("partLegale")
    public Fraction getPartLegale() {
        return this.partLegale;
    }

    public Heritier(String heritier, Fraction part) {
        this.heritier = heritier;
        this.part = part;
        this.partLegale = part;
    }

    public Heritier(com.muhend.backendai.calculs.enums.HeirType type, Fraction part) {
        this.heritier = type.getLabel();
        this.baseCalcul = type.getBaseCalcul();
        this.part = part;
        this.partLegale = part;
    }

    public Heritier(String heritier) {
        this.heritier = heritier;
    }

    public Heritier(com.muhend.backendai.calculs.enums.HeirType type) {
        this.heritier = type.getLabel();
        this.baseCalcul = type.getBaseCalcul();
    }

    public Heritier() {
    }

    @com.fasterxml.jackson.annotation.JsonProperty("partIrreductible")
    @Schema(description = "Fraction irréductible représentant la part légale du type d'héritier")
    public Fraction getPartIrreductible() {
        if (this.part != null && this.part.getDenominateur() != 0) {
            // Le constructeur de Fraction applique automatiquement la réduction (simplification)
            return new Fraction(this.part.getNumerateur(), this.part.getDenominateur());
        }
        return null;
    }
}
