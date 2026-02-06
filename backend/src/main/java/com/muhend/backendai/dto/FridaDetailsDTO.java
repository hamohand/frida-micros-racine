package com.muhend.backendai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class FridaDetailsDTO {
    private String numFrida; // frida
    private LocalDate dateCreation; // frida
    private LocalDate dateNaissance; // extrait de naissance du défunt
    private String nom; // extrait de naissance du défunt

}
