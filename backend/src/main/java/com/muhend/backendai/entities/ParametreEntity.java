package com.muhend.backendai.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Réglage applicatif clé/valeur, modifiable depuis la page Paramètres.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "parametres")
public class ParametreEntity {

    @Id
    @Column(length = 100)
    private String cle;

    @Column(nullable = false)
    private String valeur;
}
