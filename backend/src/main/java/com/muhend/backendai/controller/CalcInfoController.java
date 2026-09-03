package com.muhend.backendai.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Page d'accueil de la Composante 3 (Calculs SaaS).
 * Actif uniquement en profil calc-only pour ne pas polluer les autres déploiements.
 */
@Profile("calc-only")
@RestController
public class CalcInfoController {

    @GetMapping("/")
    public Map<String, Object> info() {
        return Map.of(
                "service", "Frida Calculs API",
                "description", "Calcul des parts d'héritage selon la loi islamique (Fara'id)",
                "version", "1.0",
                "docs", "/swagger-ui.html",
                "endpoints", List.of(
                        Map.of("method", "POST", "path", "/api/calculs/simuler", "description", "Simule la répartition d'un héritage à partir d'une composition familiale")
                ),
                "health", "/actuator/health"
        );
    }
}
