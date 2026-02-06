package com.muhend.backendai.entities;

import jakarta.persistence.*;
import lombok.Data;

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

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "calcul_id", referencedColumnName = "id")
    private CalculEntity calcul;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "defunt_id", referencedColumnName = "id")
    private DefuntEntity defunt;

    // @OneToMany(mappedBy = "fridaEntity", cascade = CascadeType.ALL, orphanRemoval
    // = true)
    @OneToMany
    private List<HeritierEntity> heritiers;

    @OneToMany
    private List<TemoinEntity> temoins;

}