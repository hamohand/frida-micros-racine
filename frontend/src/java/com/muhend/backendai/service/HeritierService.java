package com.muhend.backendai.service;

import com.muhend.backendai.entities.HeritierEntity;
import com.muhend.backendai.repository.HeritierRepo;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class HeritierService {
    @Autowired
    private final HeritierRepo heritierRepo;

    public List<HeritierEntity> listeHeritiers(String numFrida) {

        return heritierRepo.listeHeritiers(numFrida);
    }
}
