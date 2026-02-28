package com.muhend.backendai.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Setter
@Getter
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "defunt")
public class DefuntEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String numFrida;
    private String adresse;
    private String profession;
    private LocalDate dateNaissance;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "identite_id")
    private IdentitesEntity identite;
}