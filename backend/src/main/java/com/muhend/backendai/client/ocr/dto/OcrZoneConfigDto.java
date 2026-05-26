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
public class OcrZoneConfigDto {
    // [x1, y1, x2, y2]
    private List<Double> coords;
    // Langue OCR (ex: "ara", "fra", "ara+fra")
    private String lang;
    // Type de zone (ex: "text", "qrcode")
    private String type;
    // Mode de prétraitement (ex: "arabic_textured", "latin_simple")
    private String preprocess;
    // Format attendu pour la regex
    @JsonProperty("expected_format")
    private String expected_format;
    // Filtre de caractères post-OCR
    @JsonProperty("char_filter")
    private String char_filter;
    // Marge ajoutée au recadrage
    private Integer margin;
    // Valeurs attendues pour correction OCR
    @JsonProperty("valeurs_attendues")
    private List<String> valeurs_attendues;
}
