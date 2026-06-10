package com.muhend.backendai.controller.dossier;

import com.muhend.backendai.dto.dossier.FileUploadResponse;
import com.muhend.backendai.service.dossier.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "File Management", description = "API pour la gestion des fichiers")
public class FileController {
    private final FileStorageService fileStorageService;
    private final RestTemplate restTemplate;

    @Value("${services.ocr.url}")
    private String ocrApiUrl;

    @PostMapping("/files/upload")
    @Operation(summary = "Téléverser des fichiers", description = "Téléverse un ou plusieurs fichiers dans un sous-dossier spécifique")
    public ResponseEntity<FileUploadResponse> uploadFiles(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("path") String path) {
        List<String> savedFiles = fileStorageService.storeFiles(files, path);
        return ResponseEntity.ok(new FileUploadResponse(savedFiles));
    }

    @GetMapping("/entites")
    @Operation(summary = "Liste des entités OCR", description = "Proxy vers le service OCR Python pour récupérer les entités disponibles")
    public ResponseEntity<?> getOcrEntities() {
        String url = ocrApiUrl + "/api/entites";
        log.info("Proxy OCR entités → {}", url);
        try {
            Object result = restTemplate.getForObject(url, Object.class);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Erreur proxy OCR entités ({}): {}", url, e.getMessage());
            return ResponseEntity.ok(Collections.emptyList());
        }
    }
}