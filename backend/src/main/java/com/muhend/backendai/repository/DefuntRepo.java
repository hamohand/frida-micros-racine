package com.muhend.backendai.repository;

import com.muhend.backendai.dto.FridaDetailsDTO;
import com.muhend.backendai.entities.DefuntEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DefuntRepo extends JpaRepository<DefuntEntity, Long> {
    // recherche avec 'nomPrenm'
    @Query("SELECT new com.muhend.backendai.dto.FridaDetailsDTO(" +
            "f.numFrida, f.dateCreation, d.dateNaissance, e.nom) " +
            "FROM FridaEntity f " +
            "JOIN f.defunt d " +
            "JOIN d.extraitNaissance e " +
            "WHERE e.nom = :nom")
    List<FridaDetailsDTO> findByNom(@Param("nom") String nom);

}
