package com.muhend.backendai.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Service
public class LicenseValidationService {

    private boolean isLicenseValid = false;
    private String licenseKey = null;

    @Value("${frida.license.api.url:https://licences.frida.enclume-numerique.com/api/licenses/verify}")
    private String licenseApiUrl;

    @Value("${frida.license.file.path:uploads/license.key}")
    private String licenseFilePath;

    private final RestTemplate restTemplate = new RestTemplate();

    @PostConstruct
    public void init() {
        loadLicenseAndVerify();
    }

    public void loadLicenseAndVerify() {
        try {
            Path path = Paths.get(licenseFilePath);
            if (Files.exists(path)) {
                this.licenseKey = new String(Files.readAllBytes(path)).trim();
                verifyWithServer();
            } else {
                this.isLicenseValid = false;
            }
        } catch (Exception e) {
            this.isLicenseValid = false;
            System.err.println("Erreur de chargement de la licence locale : " + e.getMessage());
        }
    }

    public boolean activateLicense(String key) {
        this.licenseKey = key.trim();
        boolean valid = verifyWithServer();
        if (valid) {
            try {
                Path path = Paths.get(licenseFilePath);
                Files.createDirectories(path.getParent());
                Files.write(path, this.licenseKey.getBytes());
            } catch (IOException e) {
                System.err.println("Erreur d'écriture de la licence : " + e.getMessage());
            }
        }
        return valid;
    }

    private boolean verifyWithServer() {
        if (licenseKey == null || licenseKey.isEmpty()) {
            this.isLicenseValid = false;
            return false;
        }

        try {
            String hardwareId = HardwareInfoService.getHardwareId();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = new HashMap<>();
            body.put("licenseKey", licenseKey);
            body.put("hardwareId", hardwareId);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(licenseApiUrl, request, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Boolean valid = (Boolean) response.getBody().get("valid");
                this.isLicenseValid = Boolean.TRUE.equals(valid);
            } else {
                this.isLicenseValid = false;
            }
        } catch (Exception e) {
            System.err.println("Erreur de communication avec le serveur de licences : " + e.getMessage());
            // En cas d'absence d'internet, on peut décider d'être permissif pendant X jours, 
            // mais pour l'instant on reste strict.
            this.isLicenseValid = false; 
        }
        
        return this.isLicenseValid;
    }

    public boolean isLicenseValid() {
        return isLicenseValid;
    }

    public String getLicenseKey() {
        return licenseKey;
    }
}
