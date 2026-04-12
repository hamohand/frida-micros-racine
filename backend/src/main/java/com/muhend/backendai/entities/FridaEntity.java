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

}