package com.muhend.backendai.repository;

import com.muhend.backendai.entities.UtilisateurEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UtilisateurRepo extends JpaRepository<UtilisateurEntity, Long> {
    Optional<UtilisateurEntity> findByUsername(String username);
    boolean existsByUsername(String username);
}
