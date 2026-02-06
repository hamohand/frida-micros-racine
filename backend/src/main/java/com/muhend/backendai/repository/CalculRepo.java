package com.muhend.backendai.repository;

import com.muhend.backendai.entities.CalculEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalculRepo extends JpaRepository<CalculEntity, Long> {
}
