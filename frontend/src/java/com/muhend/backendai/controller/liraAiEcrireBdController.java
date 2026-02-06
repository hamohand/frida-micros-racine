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

    //private ExempleDeTraitement exempleDeTraitement;
    @Autowired
    private EcrireBdService ecrireBdService;

    /**
     * Extrait tous les pdf du dossier de base 'cheminDossierBase',
     * Lecture des pdf Par l'AI
     * Enregistrement des données extraites dans la BD.
    */
    @GetMapping("/lireai-ecrirebd")
    @CrossOrigin(origins = "*")
    public FridaEntity ecrireBd() throws IOException {
    //public ResponseEntity<String> ecrireBd() throws IOException {
        //ecrireBdService.traiterExtraitsNaissance("C:/frida/hamrounkaci");
        String cheminDossierBase = String.valueOf(FolderService.getFolderPath());
        System.out.println("cheminDossierBase : " + cheminDossierBase + "");
        return ecrireBdService.traiterExtraitsNaissance(cheminDossierBase);
        //return ResponseEntity.ok("Lecture AI et écriture en BD effectuées !");
    }
}
