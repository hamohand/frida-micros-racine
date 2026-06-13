package com.muhend.backendai.controller;

import com.muhend.backendai.calculs.model.FamilyRequest;
import com.muhend.backendai.calculs.model.HeritageResponse;
import com.muhend.backendai.calculs.model.Heritier;
import com.muhend.backendai.calculs.service.CalculPartsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/calculs")
@Tag(name = "Simulateur Calculs", description = "API pour simuler les parts d'héritage manuellement")
public class CalculController {

    private final CalculPartsService calculPartsService;

    public CalculController(CalculPartsService calculPartsService) {
        this.calculPartsService = calculPartsService;
    }

    @Operation(summary = "Simuler la répartition d'un héritage", description = "Calcule les parts d'héritage basées sur la composition familiale fournie manuellement.")
    @PostMapping("/simuler")
    public ResponseEntity<HeritageResponse> simulerCalcul(@Valid @RequestBody FamilyRequest request) {
        log.info("Requête de simulation reçue : {}", request);
        try {
            List<Heritier> heritiers = calculPartsService.calculParts(request);
            HeritageResponse response = HeritageResponse.fromCalculation(request, heritiers, "Calcul réussi");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur lors de la simulation : ", e);
            HeritageResponse errorResponse = new HeritageResponse();
            errorResponse.setMessage("Erreur lors du calcul : " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}
