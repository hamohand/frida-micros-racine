package com.enclumenumerique.licenseapi.controller;

import com.enclumenumerique.licenseapi.dto.VerifyRequest;
import com.enclumenumerique.licenseapi.dto.VerifyResponse;
import com.enclumenumerique.licenseapi.service.LicenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/licenses")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Autorise les appels des clients locaux
public class PublicController {

    private final LicenseService licenseService;

    @PostMapping("/verify")
    public VerifyResponse verifyLicense(@RequestBody VerifyRequest request) {
        return licenseService.verifyLicense(request);
    }
}
