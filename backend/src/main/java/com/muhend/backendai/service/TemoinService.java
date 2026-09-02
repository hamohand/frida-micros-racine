package com.muhend.backendai.service;


import com.muhend.backendai.entities.TemoinEntity;
import com.muhend.backendai.repository.TemoinRepo;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;


@Profile("!calc-only")
@Service
@AllArgsConstructor
public class TemoinService {

    @Autowired
    private final TemoinRepo temoinRepo;

    public List<TemoinEntity> listeTemoins(String numFrida) {
        return temoinRepo.listeTemoins(numFrida);
    }
}
