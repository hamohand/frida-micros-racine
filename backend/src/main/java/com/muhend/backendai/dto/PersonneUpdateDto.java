package com.muhend.backendai.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class PersonneUpdateDto {
    private String nom;
    private String prenom;
    private LocalDate dateNaissance;
    private String sexe;
    private String numParente;
    private String nin;
}
