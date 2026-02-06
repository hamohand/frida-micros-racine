package com.muhend.backendai.repository;

import com.muhend.backendai.dto.FridaDetailsDTO;
import com.muhend.backendai.entities.FridaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FridaRepo extends JpaRepository<FridaEntity, Long> {

    // Méthode pour récupérer une fiche par numFrida
    @Query("SELECT f FROM FridaEntity f WHERE f.numFrida = :numFrida")
    Optional<FridaEntity> findByNumFrida(@Param("numFrida") String numFrida);

    // Affiche certains champs de toutes les fridas, dans l'ordre décroissant des
    // dates de création
    @Query("""
                    SELECT new com.muhend.backendai.dto.FridaDetailsDTO(f.numFrida, f.dateCreation, e.dateNaissance, e.nom)
                        FROM FridaEntity f
                        JOIN f.defunt d
                        JOIN d.extraitNaissance e
                        ORDER BY f.dateCreation DESC
            """)
    List<FridaDetailsDTO> findAllFridas();
}