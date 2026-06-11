package com.muhend.backendai.calculs.model;

import com.muhend.backendai.calculs.enums.Sexe;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Requête pour calculer les parts d'héritage selon la loi islamique")
public class FamilyRequest {

    @NotNull(message = "Le sexe du défunt est obligatoire")
    @Schema(description = "Sexe du défunt", example = "M", allowableValues = { "M",
            "F" }, requiredMode = Schema.RequiredMode.REQUIRED)
    private String sexeDefunt;

    @Min(value = 0, message = "Le nombre de conjoints ne peut pas être négatif")
    @Max(value = 4, message = "Le nombre de conjoints ne peut pas dépasser 4")
    @Schema(description = "Nombre de conjoints vivants (de 0 à 4)", example = "1", defaultValue = "0", minimum = "0", maximum = "4")
    private Integer nbConjoints;

    @Schema(description = "Le père est-il vivant?", example = "false", defaultValue = "false")
    private boolean pereVivant;

    @Schema(description = "La mère est-elle vivante?", example = "false", defaultValue = "false")
    private boolean mereVivante;

    @Schema(description = "Le grand-père paternel est-il vivant?", example = "false", defaultValue = "false")
    private boolean grandPerePaternelVivant;

    @Schema(description = "La grand-mère paternelle est-elle vivante?", example = "false", defaultValue = "false")
    private boolean grandMerePaternelleVivante;

    @Min(value = 0, message = "Le nombre de filles ne peut pas être négatif")
    @Max(value = 50, message = "Le nombre de filles ne peut pas dépasser 50")
    @Schema(description = "Nombre de filles", example = "1", defaultValue = "0", minimum = "0", maximum = "50")
    private Integer nbFilles;

    @Min(value = 0, message = "Le nombre de garçons ne peut pas être négatif")
    @Max(value = 50, message = "Le nombre de garçons ne peut pas dépasser 50")
    @Schema(description = "Nombre de garçons", example = "1", defaultValue = "0", minimum = "0", maximum = "50")
    private Integer nbGarcons;

    @Min(value = 0, message = "Le nombre de soeurs ne peut pas être négatif")
    @Max(value = 50, message = "Le nombre de soeurs ne peut pas dépasser 50")
    @Schema(description = "Nombre de soeurs", example = "0", defaultValue = "0", minimum = "0", maximum = "50")
    private Integer nbSoeurs;

    @Min(value = 0, message = "Le nombre de frères ne peut pas être négatif")
    @Max(value = 50, message = "Le nombre de frères ne peut pas dépasser 50")
    @Schema(description = "Nombre de frères", example = "0", defaultValue = "0", minimum = "0", maximum = "50")
    private Integer nbFreres;

    @Min(value = 0, message = "Le nombre d'oncles ne peut pas être négatif")
    @Max(value = 50, message = "Le nombre d'oncles ne peut pas dépasser 50")
    @Schema(description = "Nombre d'oncles paternels", example = "0", defaultValue = "0", minimum = "0", maximum = "50")
    private Integer nbOncles;

    @Min(value = 0, message = "Le nombre de cousins ne peut pas être négatif")
    @Max(value = 50, message = "Le nombre de cousins ne peut pas dépasser 50")
    @Schema(description = "Nombre de cousins paternels", example = "0", defaultValue = "0", minimum = "0", maximum = "50")
    private Integer nbCousins;

    @Min(value = 0, message = "Le nombre de petits-fils ne peut pas être négatif")
    @Max(value = 50, message = "Le nombre de petits-fils ne peut pas dépasser 50")
    @Schema(description = "Nombre de petits-fils (issus d'un enfant pré-décédé)", example = "0", defaultValue = "0", minimum = "0", maximum = "50")
    private Integer nbPetitsFils;

    @Min(value = 0, message = "Le nombre de petites-filles ne peut pas être négatif")
    @Max(value = 50, message = "Le nombre de petites-filles ne peut pas dépasser 50")
    @Schema(description = "Nombre de petites-filles (issues d'un enfant pré-décédé)", example = "0", defaultValue = "0", minimum = "0", maximum = "50")
    private Integer nbPetitesFilles;

    @Schema(description = "Sexe du parent pré-décédé des petits-enfants (M pour fils, F pour fille)", example = "M", allowableValues = { "M", "F" })
    private String sexeParentPredecede;

    /**
     * Validation métier : au moins un héritier doit être présent
     */
    public boolean hasAtLeastOneHeir() {
        return (nbConjoints != null && nbConjoints > 0) || pereVivant || mereVivante || grandPerePaternelVivant || grandMerePaternelleVivante ||
                (nbFilles != null && nbFilles > 0) ||
                (nbGarcons != null && nbGarcons > 0) ||
                (nbSoeurs != null && nbSoeurs > 0) ||
                (nbFreres != null && nbFreres > 0) ||
                (nbOncles != null && nbOncles > 0) ||
                (nbCousins != null && nbCousins > 0) ||
                (nbPetitsFils != null && nbPetitsFils > 0) ||
                (nbPetitesFilles != null && nbPetitesFilles > 0);
    }

    /**
     * Convertit le sexe String en enum Sexe
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public Sexe getSexeDefuntEnum() {
        return Sexe.fromCode(sexeDefunt);
    }
}
