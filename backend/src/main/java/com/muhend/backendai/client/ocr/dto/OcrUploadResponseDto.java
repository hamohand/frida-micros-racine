package com.muhend.backendai.client.ocr.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrUploadResponseDto {
    private boolean success;
    private String filename;
    private String saved_filename; // Note the snake_case from python API
    private String url;
    private String error;
}
