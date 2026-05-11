package com.muhend.backendai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArchiveInfo {
    private String fileName;
    private String numFrida;
    private String nomDefunt;
    private String prenomDefunt;
    private LocalDate dateCreationFrida;
    private LocalDateTime dateArchivage;
    private long sizeBytes;
    private boolean includesFiles;
}
