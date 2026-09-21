package com.muhend.backendai.controller.dossier;

import com.muhend.backendai.dto.dossier.FileUploadResponse;
import com.muhend.backendai.service.dossier.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;

@Slf4j
@Profile("!calc-only")
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
            @RequestParam("path") String path,
            @RequestParam(required = false) String folderName) {
        List<String> savedFiles;
        if (folderName != null && !folderName.isEmpty()) {
            savedFiles = fileStorageService.storeFiles(files, path, folderName);
        } else {
            savedFiles = fileStorageService.storeFiles(files, path);
        }
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

    @GetMapping("/files/view")
    @Operation(summary = "Voir une image", description = "Renvoie l'image d'un document à partir de son chemin absolu")
    public ResponseEntity<org.springframework.core.io.Resource> viewFile(@RequestParam String path) {
        try {
            java.nio.file.Path filePath = java.nio.file.Paths.get(path);
            if (!java.nio.file.Files.exists(filePath) || !java.nio.file.Files.isReadable(filePath)) {
                return ResponseEntity.notFound().build();
            }
            org.springframework.core.io.Resource resource = new org.springframework.core.io.UrlResource(filePath.toUri());
            
            String contentType = java.nio.file.Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (Exception e) {
            log.error("Erreur lors de la lecture de l'image {}: {}", path, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}