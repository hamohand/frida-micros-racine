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
public class OcrZoneConfigDto {
    // [x1, y1, x2, y2]
    private List<Double> coords;
    // Langue OCR (ex: "ara", "fra", "ara+fra")
    private String lang;
    // Type de zone (ex: "text", "qrcode")
    private String type;
    // Mode de prétraitement (ex: "arabic_textured", "latin_simple")
    private String preprocess;
    // Valeurs attendues pour correction OCR
    private List<String> valeurs_attendues;
}
