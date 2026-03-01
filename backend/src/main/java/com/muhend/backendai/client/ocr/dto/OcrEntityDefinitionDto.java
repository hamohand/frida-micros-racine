package com.muhend.backendai.client.ocr.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrEntityDefinitionDto {
    private String nom;
    private String description;
    private List<OcrEntityZoneDto> zones;
    private String image_reference;
    // Cadre de référence pour le recalage géométrique
    private Object cadre_reference;
}
