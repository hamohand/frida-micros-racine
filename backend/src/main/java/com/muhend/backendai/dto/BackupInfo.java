package com.muhend.backendai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupInfo {
    private String fileName;
    private long sizeBytes;
    private LocalDateTime createdAt;
}
