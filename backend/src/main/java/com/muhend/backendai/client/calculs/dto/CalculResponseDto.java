package com.muhend.backendai.client.calculs.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CalculResponseDto {

    private String calculId;
    private LocalDateTime timestamp;
    private List<CalculHeritierDto> heritiers;
    private Integer nombreHeritiers;
    private Integer denominateurCommun;
    private CalculFractionDto partRestante;
    private String message;
    private Boolean calculComplet;
    private CompositionFamilialeDto composition;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompositionFamilialeDto {
        private String sexeDefunt;
        private int nbConjoints;
        private boolean pereVivant;
        private boolean mereVivante;
        private int nbFilles;
        private int nbGarcons;
        private int nbSoeurs;
        private int nbFreres;
    }
}
