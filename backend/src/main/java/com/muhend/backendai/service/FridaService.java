package com.muhend.backendai.service;

import com.muhend.backendai.dto.FridaDetailsDTO;
import com.muhend.backendai.entities.FridaEntity;
import com.muhend.backendai.entities.CalculEntity;
import com.muhend.backendai.repository.DefuntRepo;
import com.muhend.backendai.repository.FridaRepo;
import com.muhend.backendai.repository.CalculRepo;
import com.muhend.backendai.service.aibd.HeirPartCalculatorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class FridaService {
    private final FridaRepo fridaRepo;
    private final DefuntRepo defuntRepo;
    private final CalculRepo calculRepo;
    private final HeirPartCalculatorService heirPartCalculatorService;

    @Autowired
    public FridaService(FridaRepo fridaRepository, DefuntRepo defuntRepo, 
                        CalculRepo calculRepo, HeirPartCalculatorService calculatorService) {
        this.fridaRepo = fridaRepository;
        this.defuntRepo = defuntRepo;
        this.calculRepo = calculRepo;
        this.heirPartCalculatorService = calculatorService;
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

    public FridaEntity corrigerEtRecalculerFrida(String numFrida, FridaEntity corrections) {
        FridaEntity existing = fridaRepo.findByNumFrida(numFrida)
                .orElseThrow(() -> new RuntimeException("Frida non trouvée"));

        // Copy corrections (Here we do a full overwrite to keep it simple, assuming the UI sends the whole structure)
        // Ensure ID is same to avoid detached entity error
        corrections.setId(existing.getId());
        if (corrections.getDefunt() != null) corrections.getDefunt().setId(existing.getDefunt().getId());
        if (corrections.getCalcul() != null) corrections.getCalcul().setId(existing.getCalcul().getId());

        // Remove the warning state
        corrections.setRequiresCorrection(false);

        // Recalculate Parts! Python Call
        try {
            CalculEntity nouveauCalcul = heirPartCalculatorService.recalculerParts(corrections);
            if (existing.getCalcul() != null) {
                nouveauCalcul.setId(existing.getCalcul().getId()); // prevent duplicate row Insert
            }
            corrections.setCalcul(nouveauCalcul);
        } catch (Exception e) {
            log.error("Failed to recalculate parts after correction", e);
            throw new RuntimeException("Erreur de recalcule des parts");
        }

        return fridaRepo.save(corrections);
    }

    @Transactional
    public boolean deleteFrida(String numFrida) {
        Optional<FridaEntity> fridaOpt = fridaRepo.findByNumFrida(numFrida);
        if (fridaOpt.isPresent()) {
            fridaRepo.delete(fridaOpt.get());
            log.info("Frida supprimée avec succès: {}", numFrida);
            return true;
        }
        log.warn("Tentative de suppression échouée : Frida {} introuvable", numFrida);
        return false;
    }
}