package com.muhend.backendai.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Profile("!calc-only")
@RestController
@RequestMapping("/api/nfc-session")
public class NfcSessionController {

    // Stockage en mémoire des sessions SSE actives
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    
    // Stockage des résultats OCR (nom/prénom arabes) en attente de fusion avec NFC
    private static final Map<String, Map<String, String>> ocrResults = new ConcurrentHashMap<>();

    /** Appelé par OcrProcessingController pour stocker les noms arabes OCR */
    public static void storeOcrResults(String sessionId, Map<String, String> noms) {
        ocrResults.put(sessionId, noms);
    }

    /**
     * Frontend : Le navigateur ouvre une connexion SSE pour écouter les données NFC
     * qui arriveront plus tard.
     */
    @GetMapping(value = "/{sessionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamSseMvc(@PathVariable String sessionId) {
        log.info("📡 Nouvelle connexion SSE pour la session NFC : {}", sessionId);
        
        SseEmitter emitter = new SseEmitter(300_000L);
        
        emitters.put(sessionId, emitter);

        emitter.onCompletion(() -> {
            log.info("🔌 Connexion SSE terminée pour : {}", sessionId);
            emitters.remove(sessionId);
            ocrResults.remove(sessionId);
        });
        emitter.onTimeout(() -> {
            log.warn("⏱️ Timeout SSE pour : {}", sessionId);
            emitters.remove(sessionId);
            ocrResults.remove(sessionId);
        });
        emitter.onError((e) -> {
            log.error("❌ Erreur SSE pour : {}", sessionId, e);
            emitters.remove(sessionId);
            ocrResults.remove(sessionId);
        });

        try {
            emitter.send(SseEmitter.event().name("INIT").data("Connected"));
        } catch (IOException e) {
            emitters.remove(sessionId);
        }

        return emitter;
    }

    /**
     * Mobile : L'application Flutter envoie les données lues depuis la CNI.
     * Le serveur fusionne automatiquement les noms arabes OCR (stockés précédemment).
     */
    @PostMapping("/{sessionId}/upload")
    public ResponseEntity<String> uploadNfcData(@PathVariable String sessionId, org.springframework.http.HttpEntity<String> httpEntity) {
        log.info("📱 Réception de données NFC pour la session : {}", sessionId);
        
        String nfcJsonData = httpEntity.getBody();
        if (nfcJsonData == null) {
            log.error("Corps de requête vide !");
            return ResponseEntity.badRequest().body("Le corps de la requête est vide.");
        }

        // Fusionner les résultats OCR (noms arabes) dans le JSON NFC
        Map<String, String> storedOcr = ocrResults.remove(sessionId);
        if (storedOcr != null && !storedOcr.isEmpty()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                @SuppressWarnings("unchecked")
                Map<String, Object> nfcMap = mapper.readValue(nfcJsonData, Map.class);
                
                if (storedOcr.containsKey("nom")) nfcMap.put("nomArabe", storedOcr.get("nom"));
                if (storedOcr.containsKey("prenom")) nfcMap.put("prenomArabe", storedOcr.get("prenom"));
                if (storedOcr.containsKey("imagePath")) nfcMap.put("imagePath", storedOcr.get("imagePath"));
                
                nfcJsonData = mapper.writeValueAsString(nfcMap);
                log.info("✨ Noms arabes OCR fusionnés : nom='{}', prenom='{}', image='{}'", 
                    storedOcr.get("nom"), storedOcr.get("prenom"), storedOcr.containsKey("imagePath") ? "oui" : "non");
            } catch (Exception e) {
                log.warn("⚠️ Impossible de fusionner les noms OCR : {}", e.getMessage());
            }
        } else {
            log.info("ℹ️ Pas de résultats OCR stockés pour cette session");
        }

        SseEmitter emitter = emitters.get(sessionId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name("NFC_DATA").data(nfcJsonData));
                emitter.complete();
                emitters.remove(sessionId);
                
                log.info("✅ Données transférées au navigateur avec succès !");
                return ResponseEntity.ok("Données transmises avec succès au poste de travail.");
            } catch (Exception e) {
                log.error("❌ Erreur lors de l'envoi des données au navigateur", e);
                emitters.remove(sessionId);
                return ResponseEntity.internalServerError().body("Erreur interne : " + e.getMessage());
            }
        } else {
            log.warn("Aucun navigateur n'écoute sur la session : {}", sessionId);
            return ResponseEntity.status(404).body("Session introuvable ou expirée.");
        }
    }
}
