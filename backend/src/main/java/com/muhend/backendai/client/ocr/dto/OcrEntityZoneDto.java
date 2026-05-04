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
public class OcrEntityZoneDto {
    private String nom;
    private List<Double> coords;
    private String lang;
    private String type;
    private String preprocess;
    private String expected_format;
    private String char_filter;
    private Integer margin;
    private List<String> valeurs_attendues;
}
