package com.muhend.backendai.controller;

import com.muhend.backendai.entities.FridaEntity;
import com.muhend.backendai.service.aibd.EcrireBdService;
import com.muhend.backendai.service.dossier.FolderService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@AllArgsConstructor
@RequestMapping("/api/pdfs")
public class OcrProcessingController {

    @Autowired
    private EcrireBdService ecrireBdService;
    private final com.muhend.backendai.config.util.PathResolver pathResolver;

    /**
     * Extrait tous les pdf du dossier de base 'cheminDossierBase',
     * Lecture des pdf Par l'AI
     * Enregistrement des données extraites dans la BD.
     */
    @GetMapping("/lireai-ecrirebd")
    public FridaEntity ecrireBd(@org.springframework.web.bind.annotation.RequestParam(defaultValue = "rapide") String mode) throws IOException {
        java.nio.file.Path path = FolderService.getFolderPath();
        if (path == null) {
            path = pathResolver.getLatestFolder();
        }
        String cheminDossierBase = path.toString();
        System.out.println("cheminDossierBase : " + cheminDossierBase + "");
        return ecrireBdService.traiterExtraitsNaissance(cheminDossierBase, mode);
    }

    /**
     * Analyse partielle pour déterminer la composition familiale (f1 à f4)
     * sans sauvegarder la fiche Frida finale.
     */
    @GetMapping("/analyze-composition")
    public com.muhend.backendai.dto.CompositionAnalysisDto analyzeComposition(@org.springframework.web.bind.annotation.RequestParam(defaultValue = "rapide") String mode) throws IOException {
        java.nio.file.Path path = FolderService.getFolderPath();
        if (path == null) {
            path = pathResolver.getLatestFolder();
        }
        String cheminDossierBase = path.toString();
        System.out.println("analyzeComposition cheminDossierBase : " + cheminDossierBase);
        return ecrireBdService.analyzeComposition(cheminDossierBase, mode);
    }

    /**
     * Phase 2 : Met à jour la fiche Frida existante avec les nouveaux documents ajoutés au dossier.
     */
    @GetMapping("/update-frida/{numFrida}")
    public FridaEntity updateFrida(@org.springframework.web.bind.annotation.PathVariable String numFrida, @org.springframework.web.bind.annotation.RequestParam(defaultValue = "rapide") String mode) throws IOException {
        java.nio.file.Path path = FolderService.getFolderPath();
        if (path == null) {
            path = pathResolver.getLatestFolder();
        }
        String cheminDossierBase = path.toString();
        System.out.println("updateFrida cheminDossierBase : " + cheminDossierBase + " numFrida: " + numFrida);
        return ecrireBdService.updateFrida(cheminDossierBase, mode, numFrida);
    }
}
