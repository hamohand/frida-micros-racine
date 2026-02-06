package com.muhend.backendai.service;

import com.muhend.backendai.dto.FridaDetailsDTO;
import com.muhend.backendai.entities.FridaEntity;
import com.muhend.backendai.repository.DefuntRepo;
import com.muhend.backendai.repository.FridaRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class FridaService {
    private final FridaRepo fridaRepo;
    private final DefuntRepo defuntRepo;

    @Autowired
    public FridaService(FridaRepo fridaRepository, DefuntRepo defuntRepo) {
        this.fridaRepo = fridaRepository;
        this.defuntRepo = defuntRepo;
    }

    // Méthode pour récupérer toutes les fridas
    public List<FridaEntity> getAllFridaEntities() {
        return fridaRepo.findAll();
    }

    // Méthode pour récupérer une fiche par numFrida
    public Optional<FridaEntity> getFridaByNumFrida(String numFrida) {
        return fridaRepo.findByNumFrida(numFrida);
    }

    // Méthode pour récupérer une liste de fridas par nom
    public Optional<List<FridaDetailsDTO>> getFridaByNom(String nom) {
        return Optional.ofNullable(defuntRepo.findByNom(nom));
    }

    // ai
    public List<FridaDetailsDTO> getFridas() {
        return fridaRepo.findAllFridas();
    }
}