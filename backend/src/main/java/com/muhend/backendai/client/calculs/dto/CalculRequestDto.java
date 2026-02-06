package com.muhend.backendai.client.calculs.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalculRequestDto {

    private String sexeDefunt;

    private Integer nbConjoints;

    private boolean pereVivant;

    private boolean mereVivante;

    private Integer nbFilles;

    private Integer nbGarcons;

    private Integer nbSoeurs;

    private Integer nbFreres;
}
