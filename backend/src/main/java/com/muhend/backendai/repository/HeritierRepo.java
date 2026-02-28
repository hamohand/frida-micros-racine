package com.muhend.backendai.repository;

import com.muhend.backendai.entities.HeritierEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface HeritierRepo extends JpaRepository<HeritierEntity, Long> {

    // Liste des heritiers d'une frida par ordre de sexe asc, date naissance desc
    @Query("""
            SELECT a FROM HeritierEntity a INNER JOIN IdentitesEntity b ON a.identite.id = b.id
            where a.numFrida = :numFrida
            order by a.numParente asc , b.sexe desc, a.identite.dateNaissance asc
            """)
    List<HeritierEntity> listeHeritiers(String numFrida);

    // EXEMPLE SQL
    /*
     * select public.heritier_entity.num_parente,
     * extrait_naissance_entity.nom_prenom
     * from heritier_entity, extrait_naissance_entity
     * where extrait_naissance_entity.id=extrait_naissance_id
     * order by num_parente asc, extrait_naissance_entity.sexe desc,
     * extrait_naissance_entity.date_naissance asc;
     */
    //

    // Nombre de garçons
    /*
     * @Query("""
     * SELECT count(*) FROM HeritierEntity e WHERE e.numParente='3' AND
     * e.extraitNaissance.sexe = 'ذكر'
     * """)
     * int nbGarcons();
     * // Nombre de filles
     * 
     * @Query("""
     * SELECT count(*) FROM HeritierEntity e WHERE e.numParente='3' AND
     * e.extraitNaissance.sexe = 'أنثى'
     * """)
     */
}
