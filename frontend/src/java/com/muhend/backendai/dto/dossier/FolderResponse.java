package com.muhend.backendai.dto.dossier;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FolderResponse {
    private String folderName;
    private String fullPath;
}