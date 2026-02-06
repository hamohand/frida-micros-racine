package com.muhend.backendai.controller.dossier;

import com.muhend.backendai.dto.dossier.FileUploadResponse;
import com.muhend.backendai.service.dossier.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "File Management", description = "API pour la gestion des fichiers")
public class FileController {
    private final FileStorageService fileStorageService;

    @PostMapping("/upload")
    @CrossOrigin(origins = "*")
    @Operation(summary = "Téléverser des fichiers", 
              description = "Téléverse un ou plusieurs fichiers dans un sous-dossier spécifique")
    public ResponseEntity<FileUploadResponse> uploadFiles(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("path") String path) {
        List<String> savedFiles = fileStorageService.storeFiles(files, path);
        return ResponseEntity.ok(new FileUploadResponse(savedFiles));
    }
}