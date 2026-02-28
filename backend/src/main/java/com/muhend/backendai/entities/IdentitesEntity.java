package com.muhend.backendai.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;

@Setter
@Getter
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })

@Entity
@Table(name = "identites")
public class IdentitesEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "num_frida")
    private String numFrida = "";
    @Column(name = "nom")
    private String nom = "";
    @Column(name = "prenom")
    private String prenom = "";
    @Column(name = "latines")
    private String latines = "";
    @Size(max = 255)
    @Column(name = "prenom_latines")
    private String prenomLatines = "";
    @Column(name = "date_naissance")
    private LocalDate dateNaissance;
    @Column(name = "lieu_naissance")
    private String lieuNaissance = "";
    @Column(name = "sexe", length = 10)
    private String sexe = "";

    @Size(max = 255)
    @Column(name = "nom_piece")
    private String nomPiece = "";

    @Column(name = "numero_piece")
    private String numeroPiece = "";

    @Size(max = 255)
    @Column(name = "delivre_par")
    private String delivrePar = "";

    @Size(max = 255)
    @Column(name = "delivre_le")
    private String delivreLe = "";

    // piece identite
    @Size(max = 255)
    @Column(name = "expire_le")
    private String expireLe = "";

    // extrait naissance
    private String dateNaissanceLettres = "";
    private String baladia = "";
    private String wilaya = "";
    private String pere = "";
    private String mere = "";
    private String marge = "";
}
