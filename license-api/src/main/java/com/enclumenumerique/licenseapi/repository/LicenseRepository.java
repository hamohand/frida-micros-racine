package com.enclumenumerique.licenseapi.repository;

import com.enclumenumerique.licenseapi.entity.FridaLicense;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface LicenseRepository extends JpaRepository<FridaLicense, UUID> {
    Optional<FridaLicense> findByLicenseKey(String licenseKey);
}
