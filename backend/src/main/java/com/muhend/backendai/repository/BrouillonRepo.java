package com.muhend.backendai.repository;

import com.muhend.backendai.entities.BrouillonEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BrouillonRepo extends JpaRepository<BrouillonEntity, Long> {
    List<BrouillonEntity> findByStatutOrderByDateCreationDesc(String statut);
    Optional<BrouillonEntity> findByFolderName(String folderName);
}
