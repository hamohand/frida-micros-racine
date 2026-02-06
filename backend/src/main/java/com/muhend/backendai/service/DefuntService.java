package com.muhend.backendai.service;

import com.muhend.backendai.dto.FridaDetailsDTO;
import com.muhend.backendai.repository.DefuntRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DefuntService {
    @Autowired
    private DefuntRepo defuntRepo;

    // Méthode pour récupérer une liste de fridas par nom
    public Optional<List<FridaDetailsDTO>> listeDefunts(String nom) {
        return Optional.ofNullable(defuntRepo.findByNom(nom));
    }
}
