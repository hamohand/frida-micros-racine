package com.enclumenumerique.licenseapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerifyResponse {
    private boolean valid;
    private String message;
    private String notaryName;
    private LocalDateTime validUntil;
}
