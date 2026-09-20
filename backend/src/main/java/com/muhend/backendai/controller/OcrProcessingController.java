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

    /**
     * Extrait tous les pdf du dossier de base 'cheminDossierBase',
     * Lecture des pdf Par l'AI
     * Enregistrement des données extraites dans la BD.
     */
    @GetMapping("/lireai-ecrirebd")
    public FridaEntity ecrireBd(@org.springframework.web.bind.annotation.RequestParam(defaultValue = "rapide") String mode) throws IOException {
        java.nio.file.Path path = FolderService.getFolderPath();
        if (path == null) {
            path = pathResolver.getLatestFolder();
        }
        String cheminDossierBase = path.toString();
        System.out.println("cheminDossierBase : " + cheminDossierBase + "");
        FridaEntity result = dossierProcessingService.traiterExtraitsNaissance(cheminDossierBase, mode);
        try {
            String folderName = pathResolver.getLatestFolder().getFileName().toString();
            brouillonRepository.findByFolderName(folderName)
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
            java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("cni_front_", ".jpg");
            java.nio.file.Files.write(tempFile, imageBytes);
            
            // 1. Upload de l'image vers l'API OCR Python
            com.muhend.backendai.client.ocr.dto.OcrUploadResponseDto uploadResponse = ocrApiClient.uploadFile(tempFile);
            java.nio.file.Files.deleteIfExists(tempFile);
            
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
    public org.springframework.http.ResponseEntity<?> extraireMrzWebcam(@org.springframework.web.bind.annotation.RequestBody java.util.Map<String, String> payload) {
        try {
            String base64Image = payload.get("image");
            if (base64Image == null || base64Image.isEmpty()) {
                return org.springframework.http.ResponseEntity.badRequest().body("Image manquante");
            }
            // Enlever l'en-tête data:image/jpeg;base64, si présent
            if (base64Image.contains(",")) {
                base64Image = base64Image.split(",")[1];
            }
            
            byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Image);
            java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("webcam_mrz_", ".jpg");
            java.nio.file.Files.write(tempFile, imageBytes);
            
            // 1. Upload au service OCR Python
            com.muhend.backendai.client.ocr.dto.OcrUploadResponseDto uploadResponse = ocrApiClient.uploadFile(tempFile);
            
            // 2. Extraire la MRZ
            com.muhend.backendai.dto.MrzResult mrzResult = mrzService.extractAndParse(uploadResponse.getFilename());
            
            // Nettoyage
            java.nio.file.Files.deleteIfExists(tempFile);
            
            if (mrzResult != null && mrzResult.isValid()) {
                return org.springframework.http.ResponseEntity.ok(mrzResult);
            } else {
                return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of("message", "MRZ introuvable ou invalide sur l'image fournie."));
            }
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.internalServerError().body(java.util.Map.of("message", "Erreur lors de l'extraction MRZ : " + e.getMessage()));
        }
    }
}
