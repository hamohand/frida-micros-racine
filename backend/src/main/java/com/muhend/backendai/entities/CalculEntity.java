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


}