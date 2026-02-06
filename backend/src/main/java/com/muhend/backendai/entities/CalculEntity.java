package com.muhend.backendai.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "calcul")
public class CalculEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 255)
    @Column(name = "numFrida")
    private String numFrida;

    @ColumnDefault("0")
    @Column(name = "nbConjoints")
    private Integer nbConjoints;

    @ColumnDefault("0")
    @Column(name = "nbGarcons")
    private Integer nbGarcons;

    @ColumnDefault("0")
    @Column(name = "nbFilles")
    private Integer nbFilles;

    @ColumnDefault("0")
    @Column(name = "numerateurConjoint")
    private Integer numerateurConjoint;

    @ColumnDefault("0")
    @Column(name = "numerateurGarcons")
    private Integer numerateurGarcons;

    @ColumnDefault("0")
    @Column(name = "numerateurFilles")
    private Integer numerateurFilles;

    @Column(name = "denominateur")
    private Integer denominateur;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumFrida() {
        return numFrida;
    }

    public void setNumFrida(String numFrida) {
        this.numFrida = numFrida;
    }

    public Integer getNbConjoints() {
        return nbConjoints;
    }

    public void setNbConjoints(Integer nbConjoints) {
        this.nbConjoints = nbConjoints;
    }

    public Integer getNbGarcons() {
        return nbGarcons;
    }

    public void setNbGarcons(Integer nbGarcons) {
        this.nbGarcons = nbGarcons;
    }

    public Integer getNbFilles() {
        return nbFilles;
    }

    public void setNbFilles(Integer nbFilles) {
        this.nbFilles = nbFilles;
    }

    public Integer getNumerateurConjoint() {
        return numerateurConjoint;
    }

    public void setNumerateurConjoint(Integer numerateurConjoint) {
        this.numerateurConjoint = numerateurConjoint;
    }

    public Integer getNumerateurGarcons() {
        return numerateurGarcons;
    }

    public void setNumerateurGarcons(Integer numerateurGarcons) {
        this.numerateurGarcons = numerateurGarcons;
    }

    public Integer getNumerateurFilles() {
        return numerateurFilles;
    }

    public void setNumerateurFilles(Integer numerateurFilles) {
        this.numerateurFilles = numerateurFilles;
    }

    public Integer getDenominateur() {
        return denominateur;
    }

    public void setDenominateur(Integer denominateur) {
        this.denominateur = denominateur;
    }
}