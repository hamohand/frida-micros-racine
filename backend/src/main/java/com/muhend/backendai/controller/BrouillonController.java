package com.muhend.backendai.controller;

import com.muhend.backendai.entities.BrouillonEntity;
import com.muhend.backendai.repository.BrouillonRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/brouillons")
@RequiredArgsConstructor
@Slf4j
public class BrouillonController {

    private final BrouillonRepo brouillonRepository;

    @GetMapping
    public ResponseEntity<List<BrouillonEntity>> getBrouillonsEnCours() {
        return ResponseEntity.ok(brouillonRepository.findByStatutOrderByDateCreationDesc("EN_COURS"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BrouillonEntity> getBrouillon(@PathVariable Long id) {
        return brouillonRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBrouillon(@PathVariable Long id) {
        if (!brouillonRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        brouillonRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/fichiers")
    public ResponseEntity<Map<String, Object>> getBrouillonFiles(@PathVariable Long id) {
        return brouillonRepository.findById(id).map(brouillon -> {
            Map<String, Object> response = new HashMap<>();
            Map<String, List<String>> subfolders = new HashMap<>();
            int[] totalFiles = {0};

            try {
                Path folderPath = Paths.get(brouillon.getFolderPath());
                if (Files.exists(folderPath)) {
                    try (Stream<Path> stream = Files.walk(folderPath)) {
                        stream.filter(Files::isRegularFile).forEach(path -> {
                            Path parent = path.getParent();
                            if (parent != null) {
                                String subFolder = parent.getFileName().toString();
                                subfolders.computeIfAbsent(subFolder, k -> new ArrayList<>())
                                          .add(path.getFileName().toString());
                                totalFiles[0]++;
                            }
                        });
                    }
                }
            } catch (IOException e) {
                log.error("Erreur lors de la lecture des fichiers du brouillon {}", id, e);
                return ResponseEntity.internalServerError().<Map<String, Object>>build();
            }

            response.put("subfolders", subfolders);
            response.put("totalFiles", totalFiles[0]);
            return ResponseEntity.ok(response);
        }).orElse(ResponseEntity.notFound().build());
    }
}
