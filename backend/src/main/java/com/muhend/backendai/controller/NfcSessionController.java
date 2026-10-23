package com.muhend.backendai.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Profile("!calc-only")
@RestController
@RequestMapping("/api")
public class NfcSessionController {

    // Stockage en mémoire des sessions SSE actives
    private static final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    
    // Stockage des résultats OCR (nom/prénom arabes) en attente de fusion avec NFC
    private static final Map<String, Map<String, String>> ocrResults = new ConcurrentHashMap<>();

    public static boolean sendEvent(String sessionId, String eventName, String data) {
        SseEmitter emitter = emitters.get(sessionId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
                return true;
            } catch (Exception e) {
                emitters.remove(sessionId);
            }
        }
        return false;
    }

    /** Appelé par OcrProcessingController pour stocker les noms arabes OCR */
    public static void storeOcrResults(String sessionId, Map<String, String> noms) {
        ocrResults.put(sessionId, noms);
    }

    /**
     * Frontend : Le navigateur ouvre une connexion SSE pour écouter les données NFC
     * qui arriveront plus tard.
     */
    @GetMapping(value = "/nfc-session/{sessionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
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
     * Mobile (Douchette MRZ) : Réception de la MRZ seule avec normalisation des chiffres OCR (inspiré de dz-eid MobileRelay).
     */
    @PostMapping("/nfc-session/{sessionId}/mrz-only")
    public ResponseEntity<String> uploadMrzOnly(@PathVariable String sessionId, org.springframework.http.HttpEntity<String> httpEntity) {
        log.info("📱 Réception de MRZ (seule) pour la session : {}", sessionId);
        String json = httpEntity.getBody();
        
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> map = mapper.readValue(json, Map.class);
            
            Map<String, Object> data = map;
            if (map.get("mrz") instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> mrzMap = (Map<String, Object>) map.get("mrz");
                data = mrzMap;
            } else if (map.get("data") instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> dataMap = (Map<String, Object>) map.get("data");
                data = dataMap;
            }
            
            // Auto-parse MRZ if keys are missing but 'raw' is present
            if (data.containsKey("raw") && (!data.containsKey("documentNumber") || String.valueOf(data.get("documentNumber")).trim().isEmpty())) {
                String raw = String.valueOf(data.get("raw")).toUpperCase(Locale.ROOT).trim();
                String[] lines = raw.split("\r?\n");
                if (lines.length >= 2) {
                    if (lines[0].length() >= 14 && lines[1].length() >= 14 && lines[0].startsWith("I")) { // TD1 (ID Card)
                        String docRaw = lines[0].substring(5, 14).replaceAll("<", "");
                        data.put("documentNumber", lines[0].startsWith("IDDZA") ? normalizeDigits(docRaw) : docRaw);
                        data.put("dateOfBirth", normalizeDigits(lines[1].substring(0, 6)));
                        data.put("expiryDate", normalizeDigits(lines[1].substring(8, 14)));
                    } else if (lines[1].length() >= 27 && lines[0].startsWith("P")) { // TD3 (Passport)
                        data.put("documentNumber", lines[1].substring(0, 9).replaceAll("<", ""));
                        data.put("dateOfBirth", normalizeDigits(lines[1].substring(13, 19)));
                        data.put("expiryDate", normalizeDigits(lines[1].substring(21, 27)));
                    }
                }
            } else {
                if (data.containsKey("dateOfBirth") && data.get("dateOfBirth") != null) {
                    data.put("dateOfBirth", normalizeDigits(String.valueOf(data.get("dateOfBirth"))));
                }
                if (data.containsKey("expiryDate") && data.get("expiryDate") != null) {
                    data.put("expiryDate", normalizeDigits(String.valueOf(data.get("expiryDate"))));
                }
            }
            
            // On le renvoie au Desktop avec le type MRZ_DATA
            sendEvent(sessionId, "MRZ_DATA", mapper.writeValueAsString(data));
            return ResponseEntity.ok("{\"status\":\"ok\"}");
        } catch(Exception e) {
            log.error("Erreur parse MRZ", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Relais E2EE compatible dz-eid Mobile Standard (POST /api/send_card_data).
     * Reçoit { sessionId, encryptedData: { mobPubKeyB64, ivB64, ciphertextB64 } } et le transmet au navigateur via SSE.
     */
    @PostMapping({"/send_card_data", "/nfc-session/send_card_data"})
    public ResponseEntity<String> receiveEncryptedCardData(org.springframework.http.HttpEntity<String> httpEntity) {
        String body = httpEntity.getBody();
        if (body == null || body.isBlank()) {
            return ResponseEntity.badRequest().body("{\"error\":\"Missing payload\"}");
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(body);
            String sessionId = root.path("sessionId").asText("");
            com.fasterxml.jackson.databind.JsonNode encryptedData = root.path("encryptedData");
            if (sessionId.isEmpty() || encryptedData.isMissingNode() || !encryptedData.isObject()) {
                return ResponseEntity.badRequest().body("{\"error\":\"Missing sessionId or encryptedData\"}");
            }
            SseEmitter emitter = emitters.get(sessionId);
            if (emitter == null) {
                log.warn("Session E2EE introuvable ou expirée : {}", sessionId);
                return ResponseEntity.status(404).body("{\"error\":\"Session not found or expired\"}");
            }
            String encJson = mapper.writeValueAsString(encryptedData);
            emitter.send(SseEmitter.event().name("ENCRYPTED_NFC_DATA").data(encJson));
            emitter.complete();
            emitters.remove(sessionId);
            log.info("🔐 Données NFC chiffrées E2EE transmises au navigateur pour la session : {}", sessionId);
            return ResponseEntity.ok("{\"success\":true}");
        } catch (Exception e) {
            log.error("Erreur lors du relais E2EE /api/send_card_data", e);
            return ResponseEntity.internalServerError().body("{\"error\":\"Internal error\"}");
        }
    }

    @PostMapping("/nfc-session/{sessionId}/upload")
    public ResponseEntity<String> uploadNfcData(@PathVariable String sessionId, org.springframework.http.HttpEntity<String> httpEntity) {
        log.info("📱 Réception de données NFC pour la session : {}", sessionId);
        
        String nfcJsonData = httpEntity.getBody();
        if (nfcJsonData == null) {
            log.error("Corps de requête vide !");
            return ResponseEntity.badRequest().body("Le corps de la requête est vide.");
        }

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> map = mapper.readValue(nfcJsonData, Map.class);
            if (map.containsKey("data")) {
                Object dataObj = map.get("data");
                if (dataObj instanceof Map) {
                    nfcJsonData = mapper.writeValueAsString(dataObj);
                    log.info("📦 Enveloppe 'data' détectée et extraite du JSON NFC.");
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ Impossible de parser le JSON pour extraire 'data' : {}", e.getMessage());
        }

        // Fusionner les résultats OCR (noms arabes) dans le JSON NFC
        Map<String, String> storedOcr = ocrResults.remove(sessionId);
        if (storedOcr != null && !storedOcr.isEmpty()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                @SuppressWarnings("unchecked")
                Map<String, Object> nfcMap = mapper.readValue(nfcJsonData, Map.class);
                
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

    /** Corrige les confusions OCR classiques (O/0, I/1, S/5, B/8, Z/2, G/6, T/7) dans les champs numériques MRZ. */
    private static String normalizeDigits(String s) {
        if (s == null) return "";
        String u = s.toUpperCase(Locale.ROOT).trim();
        StringBuilder sb = new StringBuilder(u.length());
        for (int i = 0; i < u.length(); i++) {
            char c = u.charAt(i);
            switch (c) {
                case 'O': case 'Q': case 'D': case 'U': sb.append('0'); break;
                case 'I': case 'L': case 'J': sb.append('1'); break;
                case 'Z': sb.append('2'); break;
                case 'S': sb.append('5'); break;
                case 'G': sb.append('6'); break;
                case 'T': sb.append('7'); break;
                case 'B': sb.append('8'); break;
                default: sb.append(c); break;
            }
        }
        return sb.toString();
    }
}
