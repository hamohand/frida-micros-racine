package com.muhend.backendai.dto.dossier;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class FileUploadResponse {
    private List<String> savedFiles;
}