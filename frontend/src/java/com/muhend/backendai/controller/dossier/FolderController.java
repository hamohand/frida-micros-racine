package com.muhend.backendai.controller.dossier;

import com.muhend.backendai.dto.dossier.CreateFolderRequest;
import com.muhend.backendai.dto.dossier.FolderResponse;
import com.muhend.backendai.service.dossier.FolderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.units.qual.C;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/folders")
@RequiredArgsConstructor
@Tag(name = "Folder Management", description = "API pour la gestion des dossiers")
public class FolderController {
    private final FolderService folderService;

    @PostMapping("/create")
    @CrossOrigin(origins = "http://localhost:4200")
    @Operation(summary = "Créer un nouveau dossier", 
              description = "Crée un dossier basé sur le nom et prénom fournis")
    public ResponseEntity<FolderResponse> createFolder(
            @Valid @RequestBody CreateFolderRequest request) {
        return ResponseEntity.ok(folderService.createFolder(request));
    }
}