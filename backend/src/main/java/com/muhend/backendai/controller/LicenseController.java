package com.muhend.backendai.controller;

import com.muhend.backendai.service.HardwareInfoService;
import com.muhend.backendai.service.LicenseValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/license")
public class LicenseController {

    @Autowired
    private LicenseValidationService licenseValidationService;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getLicenseStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("valid", licenseValidationService.isLicenseValid());
        response.put("hardwareId", HardwareInfoService.getHardwareId());
        if (licenseValidationService.isLicenseValid()) {
            response.put("key", licenseValidationService.getLicenseKey());
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/activate")
    public ResponseEntity<Map<String, Object>> activateLicense(@RequestBody Map<String, String> body) {
        String key = body.get("licenseKey");
        if (key == null || key.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Clé vide"));
        }

        boolean success = licenseValidationService.activateLicense(key);
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("valid", success);
        if (!success) {
            response.put("message", "Clé invalide, expirée ou déjà associée à un autre PC.");
        }
        return ResponseEntity.ok(response);
    }
}
