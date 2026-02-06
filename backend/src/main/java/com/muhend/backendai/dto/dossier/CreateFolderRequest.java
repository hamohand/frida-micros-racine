package com.muhend.backendai.dto.dossier;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreateFolderRequest {
    @Pattern(regexp = "^[a-zA-Z]+$", message = "Le nom doit contenir uniquement des lettres")
    private String nom;
    
    @Pattern(regexp = "^[a-zA-Z]+$", message = "Le prénom doit contenir uniquement des lettres")
    private String prenom;
}