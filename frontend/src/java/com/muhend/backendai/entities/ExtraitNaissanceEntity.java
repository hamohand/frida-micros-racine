package com.muhend.backendai.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;

@Setter
@Getter
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})

@Entity
@Table(name = "extrait-naissance")
public class ExtraitNaissanceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String numFrida;

    private String nomPrenom;
    private String latines;
    private String dateNaissanceLettres;
    private LocalDate dateNaissance;
    private String sexe;
    private String lieuNaissance;
    private String baladia;
    private String wilaya;
    private String pere;
    private String mere;
    private String numExtrait;
    private String marge;

}