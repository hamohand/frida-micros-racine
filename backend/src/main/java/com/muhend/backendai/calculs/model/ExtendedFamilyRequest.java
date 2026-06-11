package com.muhend.backendai.calculs.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Requête étendue pour le calcul multi-tombes (plusieurs héritiers pré-décédés)")
public class ExtendedFamilyRequest extends FamilyRequest {

    @Schema(description = "Liste des héritiers pré-décédés (tombes) avec leurs descendants")
    private List<Tombe> tombes;
}
