package com.muhend.backendai.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })

@Entity
@Getter
@Setter
@Table(name = "heritier")
public class HeritierEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 255)
    @Column(name = "numFrida")
    private String numFrida = "";

    @Size(max = 255)
    @Column(name = "numParente")
    private String numParente = "";

    @Size(max = 255)
    @Column(name = "adresse")
    private String adresse = "";

    @Size(max = 255)
    @Column(name = "profession")
    private String profession = "";

    @Column(name = "coefPart")
    private Float coefPart;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "identite_id")
    private IdentitesEntity identite;

    // Relation vers FridaEntity avec clé étrangère
    /*
     * @ManyToOne(optional = true)
     * 
     * @JoinColumn(name = "frida_id", nullable = true) // "frida_id" correspond à la
     * FK dans la table heritier
     * private FridaEntity fridaEntity;
     */
    /*
     * @ManyToOne
     * 
     * @JoinColumn(name = "frida_id", nullable = false)
     * private FridaEntity fridaEntity;
     */

    /*
     * @ManyToOne(optional = true) // Relation obligatoire sans colonne explicite
     * private FridaEntity fridaEntity;
     */

}