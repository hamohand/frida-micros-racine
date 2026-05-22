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

    @ColumnDefault("0")
    @Column(name = "numerateurPere")
    private Integer numerateurPere;

    @ColumnDefault("0")
    @Column(name = "numerateurMere")
    private Integer numerateurMere;

    @ColumnDefault("0")
    @Column(name = "numerateurFreres")
    private Integer numerateurFreres;

    @ColumnDefault("0")
    @Column(name = "numerateurSoeurs")
    private Integer numerateurSoeurs;

    @ColumnDefault("0")
    @Column(name = "nbOnclesPaternels")
    private Integer nbOnclesPaternels;

    @ColumnDefault("0")
    @Column(name = "nbCousinsPaternels")
    private Integer nbCousinsPaternels;

    @ColumnDefault("0")
    @Column(name = "numerateurOnclesPaternels")
    private Integer numerateurOnclesPaternels;

    @ColumnDefault("0")
    @Column(name = "numerateurCousinsPaternels")
    private Integer numerateurCousinsPaternels;

    @ColumnDefault("false")
    @Column(name = "grandPerePaternelVivant")
    private boolean grandPerePaternelVivant;

    @ColumnDefault("0")
    @Column(name = "numerateurGrandPerePaternel")
    private Integer numerateurGrandPerePaternel;

    @Column(name = "denominateur")
    private Integer denominateur;


}