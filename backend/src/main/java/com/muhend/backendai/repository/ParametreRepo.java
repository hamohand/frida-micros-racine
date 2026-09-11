package com.muhend.backendai.repository;

import com.muhend.backendai.entities.ParametreEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParametreRepo extends JpaRepository<ParametreEntity, String> {
}
