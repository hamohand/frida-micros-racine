package com.muhend.backendai.repository;

public interface IdentitesRepo extends
        org.springframework.data.jpa.repository.JpaRepository<com.muhend.backendai.entities.IdentitesEntity, Long> {
    java.util.List<com.muhend.backendai.entities.IdentitesEntity> findByNin(String nin);
    java.util.List<com.muhend.backendai.entities.IdentitesEntity> findByNomAndPrenom(String nom, String prenom);

}
