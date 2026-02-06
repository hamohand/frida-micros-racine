package com.muhend.backendai.repository;


import com.muhend.backendai.entities.TemoinEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TemoinRepo extends JpaRepository<TemoinEntity, Long> {

    // Liste des témoins d'une frida par ordre de sexe asc, date naissance desc
//    @Query("""
//            SELECT a FROM TemoinEntity a INNER JOIN PieceIdentiteEntity b ON a.identite.id = b.id\s
//            where a.numFrida = :numFrida
//           """)
    @Query("""
            SELECT a FROM TemoinEntity a INNER JOIN ExtraitNaissanceEntity b ON a.extraitNaissance.id = b.id 
            where a.numFrida = :numFrida
            """)
    List<TemoinEntity> listeTemoins(String numFrida);

}
