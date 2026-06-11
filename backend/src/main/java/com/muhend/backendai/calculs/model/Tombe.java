package com.muhend.backendai.calculs.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Représente un héritier pré-décédé (une 'tombe') et ses descendants")
public class Tombe {

    @Schema(description = "Identifiant de la tombe (ex: tombe_1)", example = "tombe_1")
    private String identifiant;

    @NotNull(message = "Le sexe du parent pré-décédé est obligatoire")
    @Schema(description = "Sexe de l'héritier pré-décédé", example = "M", allowableValues = {"M", "F"})
    private String sexeParentPredecede;

    @NotNull(message = "Le lien de parenté est obligatoire")
    @Schema(description = "Lien de parenté avec le défunt : 'enfant' ou 'frere_soeur'",
            example = "enfant", allowableValues = {"enfant", "frere_soeur"})
    private String lienParente;

    @Min(value = 0, message = "Le nombre de descendants mâles ne peut pas être négatif")
    @Max(value = 50)
    @Schema(description = "Nombre de descendants mâles (petits-fils ou neveux)", example = "1", defaultValue = "0")
    private Integer nbDescendantsMales;

    @Min(value = 0, message = "Le nombre de descendantes femelles ne peut pas être négatif")
    @Max(value = 50)
    @Schema(description = "Nombre de descendantes femelles (petites-filles ou nièces)", example = "1", defaultValue = "0")
    private Integer nbDescendantesFemelles;

    /**
     * Vérifie si cette tombe a au moins un descendant
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean hasDescendants() {
        int males = nbDescendantsMales != null ? nbDescendantsMales : 0;
        int femelles = nbDescendantesFemelles != null ? nbDescendantesFemelles : 0;
        return (males + femelles) > 0;
    }
}
