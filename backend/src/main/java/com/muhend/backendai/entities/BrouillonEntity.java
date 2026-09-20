package com.muhend.backendai.entities;

import jakarta.persistence.*;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Getter
@Setter
@Table(name = "brouillon")
public class BrouillonEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String folderName;
    private String folderPath;
    private String nomDefunt;
    private String prenomDefunt;
    private LocalDate dateCreation;
    
    @Column(nullable = false)
    private String statut = "EN_COURS"; // EN_COURS, TRAITE
}
