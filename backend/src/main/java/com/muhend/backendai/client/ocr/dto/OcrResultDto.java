package com.muhend.backendai.client.ocr.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrResultDto {
    private String texte_final;
    private String texte_corrige_manuel;
    private String statut;
    private Double confiance;
    private String moteur;
    private String temps_execution;
}
