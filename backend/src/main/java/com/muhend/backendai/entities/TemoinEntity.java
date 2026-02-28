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
    private String numFrida = "";

    @Size(max = 255)
    @Column(name = "adresse")
    private String adresse = "";

    @Size(max = 255)
    @Column(name = "profession")
    private String profession = "";

    @Size(max = 255)
    @Column(name = "numParente")
    private String numParente = "";

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "identite_id")
    private IdentitesEntity identite;

}