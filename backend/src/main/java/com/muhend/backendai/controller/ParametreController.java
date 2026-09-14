package com.muhend.backendai.controller;

import com.muhend.backendai.service.ParametreService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Réglages applicatifs de la page Paramètres.
 */
@Profile("!calc-only")
@RestController
@RequestMapping("/api/parametres")
public class ParametreController {

    /**
     * IPv4 ou nom d'hôte : ni schéma, ni port, ni chemin (voir ParametreService.ADRESSE_RESEAU_LOCALE).
     * Pas de ':' : une IPv6 littérale demanderait des crochets dans l'URL (http://[::1]/...), que la
     * construction de l'URL côté frontend (nfc-scanner-modal) ne gère pas.
     */
    private static final Pattern ADRESSE_VALIDE = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,60}");

    private final ParametreService parametreService;

    public ParametreController(ParametreService parametreService) {
        this.parametreService = parametreService;
    }

    @GetMapping
    public Map<String, Object> lire() {
        return Map.of(
                "verificationPhonetique", parametreService.isVerificationPhonetiqueActive(),
                "adresseReseauLocale", parametreService.getAdresseReseauLocale());
    }

    @PutMapping
    @PreAuthorize("hasRole('MAITRE')")
    public ResponseEntity<?> modifier(@RequestBody Map<String, Object> corps) {
        if (corps.containsKey("verificationPhonetique")) {
            if (!(corps.get("verificationPhonetique") instanceof Boolean active)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Le champ verificationPhonetique (booléen) est requis."));
            }
            parametreService.setVerificationPhonetiqueActive(active);
        }
        if (corps.containsKey("adresseReseauLocale")) {
            if (!(corps.get("adresseReseauLocale") instanceof String adresse)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Le champ adresseReseauLocale (texte) est requis."));
            }
            String nettoyee = adresse.strip();
            if (!nettoyee.isEmpty() && !ADRESSE_VALIDE.matcher(nettoyee).matches()) {
                return ResponseEntity.badRequest().body(Map.of("message",
                        "Adresse invalide : ni « http:// », ni port, ni chemin — juste l'adresse IP ou le nom du poste, ex. 192.168.1.50."));
            }
            parametreService.setAdresseReseauLocale(nettoyee);
        }
        return ResponseEntity.ok(lire());
    }
}
