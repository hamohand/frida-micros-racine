package com.muhend.backendai.client.ocr.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrAnalysisRequestDto {
    private String filename;
    // Map of zone name to zone configuration (coords)
    private Map<String, OcrZoneConfigDto> zones;
}
