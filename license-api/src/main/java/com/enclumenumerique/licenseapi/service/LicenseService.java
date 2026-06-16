package com.enclumenumerique.licenseapi.service;

import com.enclumenumerique.licenseapi.dto.CreateLicenseRequest;
import com.enclumenumerique.licenseapi.dto.LicenseDTO;
import com.enclumenumerique.licenseapi.dto.VerifyRequest;
import com.enclumenumerique.licenseapi.dto.VerifyResponse;
import com.enclumenumerique.licenseapi.entity.FridaLicense;
import com.enclumenumerique.licenseapi.repository.LicenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LicenseService {

    private final LicenseRepository licenseRepository;

    @Transactional(readOnly = true)
    public List<LicenseDTO> getAllLicenses() {
        return licenseRepository.findAll().stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Transactional
    public LicenseDTO createLicense(CreateLicenseRequest request) {
        FridaLicense license = FridaLicense.builder()
                .licenseKey(generateKey())
                .notaryName(request.getNotaryName())
                .validUntil(LocalDateTime.now().plusMonths(request.getValidMonths()))
                .isActive(true)
                .build();
        
        FridaLicense saved = licenseRepository.save(license);
        return mapToDTO(saved);
    }

    @Transactional
    public void revokeLicense(UUID id) {
        licenseRepository.findById(id).ifPresent(license -> {
            license.setActive(false);
            licenseRepository.save(license);
        });
    }

    @Transactional
    public VerifyResponse verifyLicense(VerifyRequest request) {
        Optional<FridaLicense> optLicense = licenseRepository.findByLicenseKey(request.getLicenseKey());
        
        if (optLicense.isEmpty()) {
            return new VerifyResponse(false, "Clé de licence introuvable", null, null);
        }
        
        FridaLicense license = optLicense.get();
        
        if (!license.isActive()) {
            return new VerifyResponse(false, "Licence révoquée ou inactive", license.getNotaryName(), license.getValidUntil());
        }
        
        if (license.getValidUntil().isBefore(LocalDateTime.now())) {
            return new VerifyResponse(false, "Licence expirée", license.getNotaryName(), license.getValidUntil());
        }

        // Vérification du Hardware ID
        if (license.getHardwareId() == null || license.getHardwareId().isEmpty()) {
            // Première activation
            license.setHardwareId(request.getHardwareId());
            licenseRepository.save(license);
        } else if (!license.getHardwareId().equals(request.getHardwareId())) {
            return new VerifyResponse(false, "Hardware ID invalide. Cette licence est déjà liée à une autre machine.", license.getNotaryName(), license.getValidUntil());
        }

        return new VerifyResponse(true, "Licence valide", license.getNotaryName(), license.getValidUntil());
    }

    private String generateKey() {
        return "FRIDA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase() + "-" + UUID.randomUUID().toString().substring(9, 13).toUpperCase();
    }

    private LicenseDTO mapToDTO(FridaLicense license) {
        LicenseDTO dto = new LicenseDTO();
        dto.setId(license.getId());
        dto.setLicenseKey(license.getLicenseKey());
        dto.setNotaryName(license.getNotaryName());
        dto.setValidUntil(license.getValidUntil());
        dto.setHardwareId(license.getHardwareId());
        dto.setActive(license.isActive());
        dto.setCreatedAt(license.getCreatedAt());
        return dto;
    }
}
