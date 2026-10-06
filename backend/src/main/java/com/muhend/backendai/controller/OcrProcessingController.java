package com.muhend.backendai.controller;

import com.muhend.backendai.entities.FridaEntity;
import com.muhend.backendai.service.pipeline.DossierProcessingService;
import com.muhend.backendai.service.dossier.FolderService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@Profile("!calc-only")
@lombok.extern.slf4j.Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/api/pdfs")
public class OcrProcessingController {

    @Autowired
    private DossierProcessingService dossierProcessingService;
    private final com.muhend.backendai.config.util.PathResolver pathResolver;
    
    @Autowired
    private com.muhend.backendai.client.ocr.OcrApiClient ocrApiClient;
    
    @Autowired
    private com.muhend.backendai.service.pipeline.MrzService mrzService;

    @Autowired
    private com.muhend.backendai.repository.BrouillonRepo brouillonRepository;

    @org.springframework.web.bind.annotation.PostMapping("/extraire-noms-qr")
    public org.springframework.http.ResponseEntity<?> extraireNomsQr(@org.springframework.web.bind.annotation.RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        try {
            java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("qr_upload_", file.getOriginalFilename());
            java.nio.file.Files.write(tempFile, file.getBytes());

            // 1. Upload au service OCR Python
            com.muhend.backendai.client.ocr.dto.OcrUploadResponseDto uploadResponse = ocrApiClient.uploadFile(tempFile);
            java.nio.file.Files.deleteIfExists(tempFile);

            String filename = uploadResponse.getSaved_filename() != null ? uploadResponse.getSaved_filename() : uploadResponse.getFilename();

            String ocrApiUrl = ocrApiClient.getOcrApiUrl();
            org.springframework.web.client.RestTemplate rt = new org.springframework.web.client.RestTemplate();

            // 2. Passe générique (extrait de naissance et actes détectés auto par le moteur)
            String nomLatines = "";
            String prenomLatines = "";

            {
                java.util.Map<String, Object> body = new java.util.HashMap<>();
                body.put("filename", filename);
                java.util.Map<String, Object> zones = new java.util.HashMap<>();
                java.util.Map<String, Object> qrZone = new java.util.HashMap<>();
                qrZone.put("type", "qrcode");
                qrZone.put("coords", new double[]{0.0, 0.0, 1.0, 1.0});
                zones.put("qr_zone", qrZone);
                body.put("zones", zones);

                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> ocrResult = rt.postForObject(ocrApiUrl + "/api/analyser", body, java.util.Map.class);
                if (ocrResult != null && ocrResult.containsKey("resultats")) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> resultats = (java.util.Map<String, Object>) ocrResult.get("resultats");
                    nomLatines    = extraireTexte(resultats, "latines");
                    prenomLatines = extraireTexte(resultats, "prenomLatines");
                }
            }

            // 3. Fallback avec l'entité adc_01 si la passe générique n'a pas retourné de noms latins
            if (!estLatin(nomLatines) || !estLatin(prenomLatines)) {
                try {
                    com.muhend.backendai.client.ocr.dto.OcrEntityDefinitionDto entityDef =
                            ocrApiClient.getEntityDefinition("adc_01");
                    if (entityDef != null && entityDef.getZones() != null && !entityDef.getZones().isEmpty()) {
                        java.util.Map<String, Object> body = new java.util.HashMap<>();
                        body.put("filename", filename);
                        java.util.Map<String, Object> zones = new java.util.HashMap<>();
                        for (com.muhend.backendai.client.ocr.dto.OcrEntityZoneDto z : entityDef.getZones()) {
                            java.util.Map<String, Object> zone = new java.util.HashMap<>();
                            zone.put("type", z.getType());
                            zone.put("coords", z.getCoords());
                            if (z.getMappingSequences() != null) zone.put("mapping_sequences", z.getMappingSequences());
                            zones.put(z.getNom(), zone);
                        }
                        body.put("zones", zones);

                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> ocrResult2 = rt.postForObject(ocrApiUrl + "/api/analyser", body, java.util.Map.class);
                        if (ocrResult2 != null && ocrResult2.containsKey("resultats")) {
                            @SuppressWarnings("unchecked")
                            java.util.Map<String, Object> resultats2 = (java.util.Map<String, Object>) ocrResult2.get("resultats");
                            String nom2    = extraireTexte(resultats2, "latines");
                            String prenom2 = extraireTexte(resultats2, "prenomLatines");
                            if (estLatin(nom2))    nomLatines    = nom2;
                            if (estLatin(prenom2)) prenomLatines = prenom2;
                        }
                    }
                } catch (Exception ex) {
                    log.warn("Fallback adc_01 échoué : {}", ex.getMessage());
                }
            }

            if (!nomLatines.isEmpty() || !prenomLatines.isEmpty()) {
                return org.springframework.http.ResponseEntity.ok(java.util.Map.of("success", true, "nom", nomLatines, "prenom", prenomLatines));
            }

            return org.springframework.http.ResponseEntity.ok(java.util.Map.of("success", false, "message", "Aucun nom en lettres latines extrait depuis le QR code"));

        } catch (Exception e) {
            log.error("Erreur extraction QR: {}", e.getMessage(), e);
            return org.springframework.http.ResponseEntity.status(500).body(java.util.Map.of("success", false, "message", "Erreur serveur: " + e.getMessage()));
        }
    }

    @SuppressWarnings("unchecked")
    private String extraireTexte(java.util.Map<String, Object> resultats, String cle) {
        if (resultats == null || !resultats.containsKey(cle)) return "";
        Object obj = resultats.get(cle);
        if (!(obj instanceof java.util.Map)) return "";
        Object val = ((java.util.Map<String, Object>) obj).get("texte_final");
        return val != null ? val.toString().trim() : "";
    }

    private boolean estLatin(String s) {
        return s != null && !s.isEmpty() && s.matches("[A-Za-z ]+");
    }

    /**
     * Extrait tous les pdf du dossier de base 'cheminDossierBase',
     * Lecture des pdf Par l'AI
     * Enregistrement des données extraites dans la BD.
     */
    @GetMapping("/lireai-ecrirebd")
    public FridaEntity ecrireBd(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "rapide") String mode,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String folderName) throws IOException {
        java.nio.file.Path path = null;
        if (folderName != null && !folderName.isEmpty()) {
            // Mode brouillon : cibler un dossier spécifique
            path = java.nio.file.Paths.get(pathResolver.getLatestFolder().getParent().toString(), folderName);
        }
        if (path == null || !java.nio.file.Files.exists(path)) {
            path = FolderService.getFolderPath();
            if (path == null) {
                path = pathResolver.getLatestFolder();
            }
        }
        String cheminDossierBase = path.toString();
        System.out.println("cheminDossierBase : " + cheminDossierBase + "");
        FridaEntity result = dossierProcessingService.traiterExtraitsNaissance(cheminDossierBase, mode);
        try {
            String processedFolderName = path.getFileName().toString();
            brouillonRepository.findByFolderName(processedFolderName)
                .ifPresent(b -> { b.setStatut("TRAITE"); brouillonRepository.save(b); });
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(OcrProcessingController.class).warn("Impossible de mettre à jour le brouillon: {}", e.getMessage());
        }
        return result;
    }

    /**
     * Sauvegarde la fiche familiale (écrasement du brouillon OCR).
     */
    @PostMapping("/sauvegarder-fiche/{numFrida}")
    public void sauvegarderFiche(@PathVariable String numFrida, @org.springframework.web.bind.annotation.RequestBody com.muhend.backendai.dto.FicheUpdateDto dto) {
        dossierProcessingService.sauvegarderFicheCorrigee(numFrida, dto);
    }

    /**
     * Déclenche manuellement le calcul pour une Frida existante.
     * Appelée depuis l'interface de vérification de la Fiche.
     */
    @PostMapping("/lancer-calcul/{numFrida}")
    public org.springframework.http.ResponseEntity<?> lancerCalcul(@PathVariable String numFrida) {
        try {
            return org.springframework.http.ResponseEntity.ok(dossierProcessingService.lancerCalcul(numFrida));
        } catch (com.muhend.backendai.calculs.exception.InvalidFamilyCompositionException e) {
            return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage()));
        }
    }

    /**
     * Reçoit une image de carte d'identité (recto) du mobile, lance l'OCR,
     * et STOCKE les noms arabes côté serveur (pas de retour au mobile).
     * Les noms seront fusionnés avec les données NFC lors de l'upload.
     */
    @PostMapping("/ocr-cni-front")
    public org.springframework.http.ResponseEntity<?> extraireNomsArabes(@org.springframework.web.bind.annotation.RequestBody java.util.Map<String, String> payload) {
        try {
            String base64Image = payload.get("image");
            String sessionId = payload.get("sessionId");
            if (base64Image == null || base64Image.isEmpty()) {
                return org.springframework.http.ResponseEntity.badRequest().body("Image manquante");
            }
            if (base64Image.contains(",")) {
                base64Image = base64Image.split(",")[1];
            }
            
            byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Image);
            
            // Sauvegarder directement dans le répertoire persistant (évite les erreurs de permission /tmp → /app)
            java.nio.file.Path nfcDir = java.nio.file.Paths.get("/frida-storage/nfc-images");
            if (!java.nio.file.Files.exists(nfcDir)) {
                java.nio.file.Files.createDirectories(nfcDir);
            }
            String imgFilename = (sessionId != null && !sessionId.isEmpty()) ? sessionId + ".jpg" : "unknown_" + System.currentTimeMillis() + ".jpg";
            java.nio.file.Path persistentFile = nfcDir.resolve(imgFilename);
            java.nio.file.Files.write(persistentFile, imageBytes);
            String persistentImagePath = persistentFile.toAbsolutePath().toString();
            
            // 1. Upload de l'image vers l'API OCR Python
            com.muhend.backendai.client.ocr.dto.OcrUploadResponseDto uploadResponse = ocrApiClient.uploadFile(persistentFile);
            
            // 2. Appel PaddleOCR par mots-clés (cherche اللقب et الاسم)
            String ocrApiUrl = ocrApiClient.getOcrApiUrl();
            String filename = uploadResponse.getSaved_filename() != null ? uploadResponse.getSaved_filename() : uploadResponse.getFilename();
            
            org.springframework.web.client.RestTemplate rt = new org.springframework.web.client.RestTemplate();
            java.util.Map<String, String> body = java.util.Map.of("filename", filename);
            
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> ocrResult = rt.postForObject(
                ocrApiUrl + "/api/ocr-noms-arabes", body, java.util.Map.class);
            
            if (ocrResult == null || !Boolean.TRUE.equals(ocrResult.get("success"))) {
                return org.springframework.http.ResponseEntity.ok(java.util.Map.of("success", false, "message", "OCR: aucun nom détecté"));
            }
            
            java.util.Map<String, String> noms = new java.util.HashMap<>();
            noms.put("nom", (String) ocrResult.getOrDefault("nom", ""));
            noms.put("prenom", (String) ocrResult.getOrDefault("prenom", ""));
            if (!persistentImagePath.isEmpty()) {
                noms.put("imagePath", persistentImagePath);
            }
            
            // 3. Stocker côté serveur pour fusion avec NFC (si sessionId fourni)
            if (sessionId != null && !sessionId.isEmpty()) {
                NfcSessionController.storeOcrResults(sessionId, noms);
                System.out.println("📝 OCR stocké pour session " + sessionId + " : " + noms);
            }
            
            return org.springframework.http.ResponseEntity.ok(java.util.Map.of(
                "success", true,
                "nom", noms.get("nom"),
                "prenom", noms.get("prenom")
            ));
            
        } catch (Exception e) {
            e.printStackTrace();
            return org.springframework.http.ResponseEntity.internalServerError().body(java.util.Map.of("message", "Erreur OCR : " + e.getMessage()));
        }
    }
    
    @org.springframework.web.bind.annotation.PostMapping("/mobile-mrz")
    public org.springframework.http.ResponseEntity<?> extraireMrzMobile(@org.springframework.web.bind.annotation.RequestBody java.util.Map<String, String> payload) {
        try {
            String base64Image = payload.get("image");
            String sessionId = payload.get("sessionId");
            if (base64Image == null || base64Image.isEmpty()) {
                return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of("message", "Image manquante"));
            }
            if (sessionId == null || sessionId.isEmpty()) {
                return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of("message", "SessionId manquant"));
            }
            // Enlever l'en-tête data:image/jpeg;base64, si présent
            if (base64Image.contains(",")) {
                base64Image = base64Image.split(",")[1];
            }
            
            byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Image);
            java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("mobile_mrz_", ".jpg");
            java.nio.file.Files.write(tempFile, imageBytes);
            
            // 1. Upload au service OCR Python
            com.muhend.backendai.client.ocr.dto.OcrUploadResponseDto uploadResponse = ocrApiClient.uploadFile(tempFile);
            
            // 2. Extraire la MRZ
            com.muhend.backendai.dto.MrzResult mrzResult = mrzService.extractAndParse(uploadResponse.getFilename());
            
            // TODO: Appeler extract_cni pour Date et Lieu de délivrance
            
            // Nettoyage
            java.nio.file.Files.deleteIfExists(tempFile);
            
            if (mrzResult != null && mrzResult.isValid()) {
                // Envoyer le résultat au PC via SSE
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                String json = mapper.writeValueAsString(mrzResult);
                
                NfcSessionController.sendEvent(sessionId, "MRZ_DATA", json);
                
                return org.springframework.http.ResponseEntity.ok(java.util.Map.of("success", true));
            } else {
                return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of("message", "MRZ introuvable sur l'image"));
            }
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.internalServerError().body(java.util.Map.of("message", "Erreur lors de l'extraction MRZ : " + e.getMessage()));
        }
    }
}
