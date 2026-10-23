package com.muhend.backendai.controller;

import com.muhend.backendai.dto.dossier.CreateFolderRequest;
import com.muhend.backendai.dto.dossier.FolderResponse;
import com.muhend.backendai.entities.BrouillonEntity;
import com.muhend.backendai.repository.BrouillonRepo;
import com.muhend.backendai.service.demo.DemoFamilleService;
import com.muhend.backendai.service.dossier.FolderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Endpoints dédiés à la démonstration en ligne : expose le dossier en ligne « famille »
 * contenant les actes fictifs autorisés.
 */
@Profile("!calc-only")
@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
@Slf4j
public class DemoController {

    private static final Map<String, List<String>> FICHIERS_PAR_DOSSIER = createFichiersMap();

    private static Map<String, List<String>> createFichiersMap() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put("01_ad_adc_01", List.of("epoux_deces.png"));
        map.put("01_en_en_01_qrcode_01", List.of("epoux.png"));
        map.put("02_en_en_01_qrcode_01", List.of("epouse.png"));
        map.put("03_en_en_01_qrcode_01", List.of("fils_1.png", "fils_2.png", "fils_3.png", "fille_1.png", "fille_2.png"));
        map.put("04_en_en_01_qrcode_01", List.of("pere.png", "mere.png"));
        map.put("05_en_en_01_qrcode_01", List.of("frere.png", "soeur.png"));
        map.put("00_en_en_01_qrcode_01", List.of("temoin_1.png", "temoin_2.png"));
        return map;
    }

    private final DemoFamilleService demoFamilleService;
    private final FolderService folderService;
    private final BrouillonRepo brouillonRepository;

    /**
     * Liste les actes fictifs disponibles dans le dossier en ligne « famille ».
     */
    @GetMapping("/famille")
    public ResponseEntity<List<DemoFamilleService.FichierFamilleInfo>> listerFichiersFamille() {
        return ResponseEntity.ok(demoFamilleService.listerFichiers());
    }

    /**
     * Télécharge ou prévisualise un acte fictif du dossier en ligne « famille ».
     */
    @GetMapping("/famille/{nomFichier:.+}")
    public ResponseEntity<byte[]> ObtenirFichierFamille(@PathVariable String nomFichier) {
        byte[] bytes = demoFamilleService.lireFichier(nomFichier);
        if (bytes == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(bytes);
    }

    /**
     * Crée un dossier d'exemple (utilisé notamment par le script de réinitialisation nocturne).
     */
    @PostMapping("/creer-dossier-exemple")
    public ResponseEntity<?> creerDossierExemple() {
        try {
            CreateFolderRequest req = new CreateFolderRequest();
            req.setNom("KACEM");
            req.setPrenom("AHMED");
            req.setNomArabe("قاسم");
            req.setPrenomArabe("أحمد");

            FolderResponse folder = folderService.createFolder(req);
            Path dossierCible = Paths.get(folder.getFullPath());
            int nbFichiers = copierSpecimensDansDossier(dossierCible);

            Long brouillonId = brouillonRepository.findByFolderName(folder.getFolderName())
                    .map(BrouillonEntity::getId)
                    .orElse(null);

            Map<String, Object> reponse = new LinkedHashMap<>();
            reponse.put("brouillonId", brouillonId);
            reponse.put("folderName", folder.getFolderName());
            reponse.put("fullPath", folder.getFullPath());
            reponse.put("nom", "KACEM");
            reponse.put("prenom", "AHMED");
            reponse.put("nomArabe", "قاسم");
            reponse.put("prenomArabe", "أحمد");
            reponse.put("totalFiles", nbFichiers);
            return ResponseEntity.ok(reponse);
        } catch (Exception e) {
            log.error("Erreur lors de la création du dossier d'exemple", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Impossible de créer le dossier d'exemple : " + e.getMessage()));
        }
    }

    private int copierSpecimensDansDossier(Path dossierCible) throws IOException {
        int total = 0;
        for (Map.Entry<String, List<String>> entry : FICHIERS_PAR_DOSSIER.entrySet()) {
            Path sousDossier = dossierCible.resolve(entry.getKey());
            Files.createDirectories(sousDossier);
            for (String nomFichier : entry.getValue()) {
                byte[] bytes = demoFamilleService.lireFichier(nomFichier);
                if (bytes != null) {
                    Files.write(sousDossier.resolve(nomFichier), bytes);
                    total++;
                }
            }
        }
        log.info("{} actes fictifs copiés dans {}", total, dossierCible);
        return total;
    }
}
