package com.enclumenumerique.licenseapi.controller;

import com.enclumenumerique.licenseapi.dto.CreateLicenseRequest;
import com.enclumenumerique.licenseapi.dto.LicenseDTO;
import com.enclumenumerique.licenseapi.service.LicenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/licenses")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // A affiner en prod
public class AdminController {

    private final LicenseService licenseService;

    @GetMapping
    public List<LicenseDTO> getAllLicenses() {
        return licenseService.getAllLicenses();
    }

    @PostMapping
    public LicenseDTO createLicense(@RequestBody CreateLicenseRequest request) {
        return licenseService.createLicense(request);
    }

    @PostMapping("/{id}/revoke")
    public void revokeLicense(@PathVariable UUID id) {
        licenseService.revokeLicense(id);
    }
}
