package com.muhend.backendai.client.calculs.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalculHeritierDto {
    /**
     * heritier : représente un type d'héritier membre de la famille du défunt.
     * part : fraction représentant l'éventuelle part de l'héritage sinon 0.
     */
    private String heritier;
    private CalculFractionDto part;

    public CalculHeritierDto(String heritier) {
        this.heritier = heritier;
    }
}
