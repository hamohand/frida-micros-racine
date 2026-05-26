package com.muhend.backendai.client.ocr.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

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
    @JsonProperty("expected_format")
    private String expected_format;
    @JsonProperty("char_filter")
    private String char_filter;
    private Integer margin;
    @JsonProperty("valeurs_attendues")
    private List<String> valeurs_attendues;
}
