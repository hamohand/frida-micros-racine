package com.enclumenumerique.licenseapi.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CreateLicenseRequest {
    private String notaryName;
    private int validMonths;
}
