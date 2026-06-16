package com.enclumenumerique.licenseapi.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class LicenseDTO {
    private UUID id;
    private String licenseKey;
    private String notaryName;
    private LocalDateTime validUntil;
    private String hardwareId;
    private boolean isActive;
    private LocalDateTime createdAt;
}
