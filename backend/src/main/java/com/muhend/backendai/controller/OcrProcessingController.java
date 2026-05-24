package com.muhend.backendai.controller;

import com.muhend.backendai.entities.FridaEntity;
import com.muhend.backendai.service.aibd.EcrireBdService;
import com.muhend.backendai.service.dossier.FolderService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
     * Sauvegarde la fiche familiale (écrasement du brouillon OCR).
     */
    @PostMapping("/sauvegarder-fiche/{numFrida}")
    public void sauvegarderFiche(@PathVariable String numFrida, @org.springframework.web.bind.annotation.RequestBody com.muhend.backendai.dto.FicheUpdateDto dto) {
        ecrireBdService.sauvegarderFicheCorrigee(numFrida, dto);
    }

    /**
     * Déclenche manuellement le calcul pour une Frida existante.
     * Appelée depuis l'interface de vérification de la Fiche.
     */
    @PostMapping("/lancer-calcul/{numFrida}")
    public FridaEntity lancerCalcul(@PathVariable String numFrida) {
        return ecrireBdService.lancerCalcul(numFrida);
    }

}
