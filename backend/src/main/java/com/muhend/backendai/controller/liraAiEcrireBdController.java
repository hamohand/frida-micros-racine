package com.muhend.backendai.controller;

import com.muhend.backendai.entities.FridaEntity;
import com.muhend.backendai.service.aibd.EcrireBdService;
import com.muhend.backendai.service.dossier.FolderService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@AllArgsConstructor
@RequestMapping("/api/pdfs")
public class liraAiEcrireBdController {

    // private ExempleDeTraitement exempleDeTraitement;
    @Autowired
    private EcrireBdService ecrireBdService;
    private final com.muhend.backendai.config.util.PathResolver pathResolver;

    /**
     * Extrait tous les pdf du dossier de base 'cheminDossierBase',
     * Lecture des pdf Par l'AI
     * Enregistrement des données extraites dans la BD.
     */
    @GetMapping("/lireai-ecrirebd")
    // @CrossOrigin(origins = "*") // Supprimé pour éviter conflit avec
    // allowCredentials(true)
    public FridaEntity ecrireBd() throws IOException {
        java.nio.file.Path path = FolderService.getFolderPath();
        if (path == null) {
            path = pathResolver.getLatestFolder();
        }
        String cheminDossierBase = path.toString();
        System.out.println("cheminDossierBase : " + cheminDossierBase + "");
        return ecrireBdService.traiterExtraitsNaissance(cheminDossierBase);
    }
}
