package com.muhend.backendai.controller;

import com.muhend.backendai.dto.FridaDetailsDTO;
import com.muhend.backendai.entities.FridaEntity;
import com.muhend.backendai.entities.HeritierEntity;
import com.muhend.backendai.entities.TemoinEntity;

import com.muhend.backendai.service.DefuntService;
import com.muhend.backendai.service.FridaService;
import com.muhend.backendai.service.HeritierService;
import com.muhend.backendai.service.TemoinService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/frida")
public class FridaController {
    @Autowired
    private final FridaService fridaService;

    @Autowired
    private HeritierService heritierService;
    @Autowired
    private TemoinService temoinService;
    @Autowired
    private DefuntService defuntService;

    public FridaController(FridaService fridaService) {
        this.fridaService = fridaService;
    }

    /**
     * Endpoint pour récupérer une fiche par numFrida
     * 
     * @param numFrida correspond au champ à rechercher
     * @return La fiche correspondante ou un statut 404 si elle n'existe pas
     */
    @GetMapping("/{numFrida}")
    public ResponseEntity<FridaEntity> getFridaByNumFrida(@PathVariable String numFrida) {
        Optional<FridaEntity> fridaEntity = fridaService.getFridaByNumFrida(numFrida);
        return fridaEntity.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Affichage de certains champs (voir FridaDetailsDTO) de toutes les fridas
    @GetMapping("/fridas")
    public ResponseEntity<List<FridaDetailsDTO>> getFridas() {
        List<FridaDetailsDTO> fridaDetails = fridaService.getFridas();
        return ResponseEntity.ok(fridaDetails);
    }

    // Affiche les heritiers d'une frida par numFrida
    @GetMapping("/listeHeritiers/{numFrida}")
    public List<HeritierEntity> listeHeritiers(@PathVariable("numFrida") String numFrida) {
        return heritierService.listeHeritiers(numFrida);
    }

    // Affiche les témoins d'une frida par numFrida
    @GetMapping("/listeTemoins/{numFrida}")
    public List<TemoinEntity> listeTemoins(@PathVariable("numFrida") String numFrida) {
        return temoinService.listeTemoins(numFrida);
    }

    // Affiche les témoins d'une frida par nom
    @GetMapping("/listeDefunts/{nom}")
    public Optional<List<FridaDetailsDTO>> listeDefunts(@PathVariable("nom") String nom) {
        return defuntService.listeDefunts(nom);
    }

    /* ------------Non encore utilisés dans l'application---------------------- */
    @GetMapping("/search")
    public List<FridaDetailsDTO> getDefuntsByNom(@RequestParam String nom) {
        return fridaService.getFridaByNom(nom).orElse(List.of());
    }

    /**
     * Endpoint pour récupérer tout le contenu de la table Frida.
     * 
     * @return Liste des entités Frida.
     */
    @GetMapping("/all")
    public List<FridaEntity> getAllFridaEntities() {
        return fridaService.getAllFridaEntities();
    }

}
