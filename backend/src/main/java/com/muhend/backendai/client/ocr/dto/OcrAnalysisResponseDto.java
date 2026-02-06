package com.muhend.backendai.client.ocr.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.util.Map;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrAnalysisResponseDto {
    private boolean success;
    // Map of zone name to result
    private Map<String, OcrResultDto> resultats;
    private List<String> alertes;
    private Map<String, Integer> stats_moteurs;
    private String error;
}
