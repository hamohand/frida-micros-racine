package com.enclumenumerique.licenseapi.dto;

import lombok.Data;

@Data
public class VerifyRequest {
    private String licenseKey;
    private String hardwareId;
}
