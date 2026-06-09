package com.muhend.backendai.entities;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@Entity
@Getter
@Setter
@Table(name = "frida")
public class FridaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String numFrida;

    private LocalDate dateCreation;
    private String notaire;

    @Column(columnDefinition = "boolean default false")
    private Boolean requiresCorrection = false;

    /**
     * Statut du dossier dans le cycle de vie.
     * VALIDE         : traitement terminé, frida consultable.
     * EN_ATTENTE_REVISION : traité par le batch, en attente de révision humaine.
     * ECHEC          : traitement OCR en échec, dossier à retraiter.
     */
    public static final String STATUT_VALIDE = "VALIDE";
    public static final String STATUT_EN_ATTENTE = "EN_ATTENTE_REVISION";
    public static final String STATUT_ECHEC = "ECHEC";

    @Column(name = "statut", nullable = false, columnDefinition = "varchar(255) default 'VALIDE'")
    private String statut = STATUT_VALIDE;


    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "calcul_id", referencedColumnName = "id")
    private CalculEntity calcul;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "defunt_id", referencedColumnName = "id")
    private DefuntEntity defunt;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "frida_id")
    private List<HeritierEntity> heritiers;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "frida_id")
    private List<TemoinEntity> temoins;

    @Column(name = "sexe_parent_predecede", length = 1)
    private String sexeParentPredecede;

}