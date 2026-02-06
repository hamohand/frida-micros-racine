package com.muhend.backendai.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "piece_identite")
public class PieceIdentiteEntity {
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(max = 255)
    @Column(name = "num_frida")
    private String numFrida;

    @Size(max = 255)
    @Column(name = "nom")
    private String nom;

    @Size(max = 255)
    @Column(name = "prenom")
    private String prenom;

    @Size(max = 255)
    @Column(name = "latines")
    private String latines;

    @Column(name = "date_naissance")
    private LocalDate dateNaissance;

    @Size(max = 255)
    @Column(name = "lieu_naissance")
    private String lieuNaissance;

    @Size(max = 10)
    @Column(name = "sexe", length = 10)
    private String sexe;

    @Size(max = 255)
    @Column(name = "nom_piece")
    private String nomPiece;

    @Size(max = 255)
    @Column(name = "numero_piece")
    private String numeroPiece;

    @Size(max = 255)
    @Column(name = "delivre_par")
    private String delivrePar;
    //
    @Size(max = 255)
    @Column(name = "delivre_le")
    private String delivreLe;

    @Size(max = 255)
    @Column(name = "expire_le")
    private String expireLe;

}