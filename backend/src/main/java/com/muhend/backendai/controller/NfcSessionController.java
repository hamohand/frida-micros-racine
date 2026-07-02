package com.muhend.backendai.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestController
@RequestMapping("/api/nfc-session")
public class NfcSessionController {

    // Stockage en mémoire des sessions SSE actives
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * Frontend : Le navigateur ouvre une connexion SSE pour écouter les données NFC
     * qui arriveront plus tard.
     */
    @GetMapping(value = "/{sessionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamSseMvc(@PathVariable String sessionId) {
        log.info("📡 Nouvelle connexion SSE pour la session NFC : {}", sessionId);
        
        // Timeout de 5 minutes (300 000 ms)
        SseEmitter emitter = new SseEmitter(300_000L);
        
        emitters.put(sessionId, emitter);

        emitter.onCompletion(() -> {
            log.info("🔌 Connexion SSE terminée pour : {}", sessionId);
            emitters.remove(sessionId);
        });
        emitter.onTimeout(() -> {
            log.warn("⏱️ Timeout SSE pour : {}", sessionId);
            emitters.remove(sessionId);
        });
        emitter.onError((e) -> {
            log.error("❌ Erreur SSE pour : {}", sessionId, e);
            emitters.remove(sessionId);
        });

        try {
            // Envoi d'un événement d'initialisation pour forcer l'ouverture de la connexion
            emitter.send(SseEmitter.event().name("INIT").data("Connected"));
        } catch (IOException e) {
            emitters.remove(sessionId);
        }

        return emitter;
    }

    /**
     * Mobile : L'application Flutter envoie les données lues depuis la CNI.
     */
    @PostMapping("/{sessionId}/upload")
    public ResponseEntity<String> uploadNfcData(@PathVariable String sessionId, org.springframework.http.HttpEntity<String> httpEntity) {
        log.info("📱 Réception de données NFC pour la session : {}", sessionId);
        
        String nfcJsonData = httpEntity.getBody();
        if (nfcJsonData == null) {
            log.error("Corps de requête vide !");
            return ResponseEntity.badRequest().body("Le corps de la requête est vide.");
        }

        SseEmitter emitter = emitters.get(sessionId);
        if (emitter != null) {
            try {
                // On pousse le JSON brut directement au navigateur connecté
                emitter.send(SseEmitter.event().name("NFC_DATA").data(nfcJsonData));
                
                // On ferme la connexion proprement
                emitter.complete();
                emitters.remove(sessionId);
                
                log.info("✅ Données transférées au navigateur avec succès !");
                return ResponseEntity.ok("Données transmises avec succès au poste de travail.");
            } catch (Exception e) {
                log.error("❌ Erreur lors de l'envoi des données au navigateur", e);
                emitters.remove(sessionId);
                return ResponseEntity.internalServerError().body("Erreur interne lors de la transmission: " + e.getMessage());
            }
        } else {
            log.warn("Aucun navigateur n'écoute sur la session : {}", sessionId);
            return ResponseEntity.status(404).body("Session introuvable ou expirée.");
        }
    }
}
