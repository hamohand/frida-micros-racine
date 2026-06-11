package com.muhend.backendai.dto.dossier;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreateFolderRequest {
    @Pattern(regexp = "^[a-zA-ZÀ-ÿ\\s\\-]+$", message = "Le nom contient des caractères non autorisés")
    private String nom;
    
    @Pattern(regexp = "^[a-zA-ZÀ-ÿ\\s\\-]+$", message = "Le prénom contient des caractères non autorisés")
    private String prenom;
}