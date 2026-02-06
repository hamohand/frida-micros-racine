package com.muhend.backendai.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "temoin")
public class TemoinEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 255)
    @Column(name = "num_frida")
    private String numFrida;

    @Size(max = 255)
    @Column(name = "adresse")
    private String adresse;

    @Size(max = 255)
    @Column(name = "profession")
    private String profession;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pieceIdentite_id")
    private PieceIdentiteEntity identite;

    // Provisoire
    @Size(max = 255)
    @Column(name = "numParente")
    private String numParente;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "extraitNaissance_id")
    private ExtraitNaissanceEntity extraitNaissance;

}