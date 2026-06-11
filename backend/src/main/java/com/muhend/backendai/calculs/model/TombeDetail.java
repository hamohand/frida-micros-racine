package com.muhend.backendai.calculs.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Détail du calcul de wasiyya wajiba pour une tombe spécifique")
public class TombeDetail {

    @Schema(description = "Identifiant de la tombe", example = "tombe_1")
    private String identifiant;

    @Schema(description = "Sexe de l'héritier pré-décédé", example = "M")
    private String sexeParentPredecede;

    @Schema(description = "Lien de parenté avec le défunt", example = "enfant")
    private String lienParente;

    @Schema(description = "Part théorique simulée (avant plafonnement)")
    private Fraction partSimulee;

    @Schema(description = "Part wasiyya effective (après plafonnement éventuel)")
    private Fraction wasiyyaEffective;

    @Schema(description = "Indique si la wasiyya a été plafonnée à 1/3", example = "false")
    private boolean plafonnee;

    @Schema(description = "Bénéficiaires de cette tombe (petits-enfants ou neveux/nièces)")
    private List<Heritier> beneficiaires;
}
