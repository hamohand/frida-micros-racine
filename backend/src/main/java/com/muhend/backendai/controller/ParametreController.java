package com.muhend.backendai.controller;

import com.muhend.backendai.service.ParametreService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Réglages applicatifs de la page Paramètres.
 */
@Profile("!calc-only")
@RestController
@RequestMapping("/api/parametres")
public class ParametreController {

    private final ParametreService parametreService;

    public ParametreController(ParametreService parametreService) {
        this.parametreService = parametreService;
    }

    @GetMapping
    public Map<String, Object> lire() {
        return Map.of("verificationPhonetique", parametreService.isVerificationPhonetiqueActive());
    }

    @PutMapping
    @PreAuthorize("hasRole('MAITRE')")
    public ResponseEntity<?> modifier(@RequestBody Map<String, Object> corps) {
        if (!(corps.get("verificationPhonetique") instanceof Boolean active)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Le champ verificationPhonetique (booléen) est requis."));
        }
        parametreService.setVerificationPhonetiqueActive(active);
        return ResponseEntity.ok(lire());
    }
}
