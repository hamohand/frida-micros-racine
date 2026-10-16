package com.muhend.backendai.controller;

import com.muhend.backendai.dto.dossier.CreateFolderRequest;
import com.muhend.backendai.dto.dossier.FolderResponse;
import com.muhend.backendai.entities.BrouillonEntity;
import com.muhend.backendai.repository.BrouillonRepo;
import com.muhend.backendai.service.dossier.FolderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Endpoints dédiés à la démonstration en ligne : permet de créer ou pré-remplir
 * un dossier avec le jeu d'actes fictifs (Famille KACEM Ahmed) sans téléversement
 * de fichiers personnels depuis le poste du visiteur.
 */
@Profile("!calc-only")
@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
@Slf4j
public class DemoController {

    private static final String BASE_SPECIMENS = "demo-specimens/famille-kacem/";

    /** Correspondance sous-dossier -> fichiers spécimens à copier. */
    private static final Map<String, List<String>> FICHIERS_PAR_DOSSIER = createFichiersMap();

    private static Map<String, List<String>> createFichiersMap() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put("01_ad_adc_01", List.of("deces_defunt.png"));
        map.put("01_en_en_01_qrcode_01", List.of("epoux.png"));
        map.put("02_en_en_01_qrcode_01", List.of("epouse.png"));
        map.put("03_en_en_01_qrcode_01", List.of("fils_1.png", "fils_2.png", "fille_1.png", "fille_2.png"));
        map.put("04_en_en_01_qrcode_01", List.of("pere.png", "mere.png"));
        map.put("04_ad_adc_01", List.of("deces_pere.png", "deces_mere.png"));
        map.put("00_en_en_01_qrcode_01", List.of("temoin_1.png", "temoin_2.png"));
        return map;
    }

    private final FolderService folderService;
    private final BrouillonRepo brouillonRepository;

    /**
     * Crée un nouveau dossier d'exemple (Défunt : KACEM Ahmed / أحمد قاسم)
     * et y copie l'ensemble des actes fictifs de la famille KACEM.
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

    /**
     * Copie les actes fictifs de la famille KACEM dans le dernier dossier créé
     * (ou dans le dossier identifié par folderName).
     */
    @PostMapping("/charger-exemple")
    public ResponseEntity<?> chargerExemple(@RequestParam(required = false) String folderName) {
        try {
            BrouillonEntity brouillon = null;
            if (folderName != null && !folderName.isBlank()) {
                brouillon = brouillonRepository.findByFolderName(folderName).orElse(null);
            }
            if (brouillon == null) {
                List<BrouillonEntity> enCours = brouillonRepository.findByStatutOrderByDateCreationDesc("EN_COURS");
                if (!enCours.isEmpty()) {
                    brouillon = enCours.get(0);
                }
            }
            if (brouillon == null) {
                return creerDossierExemple();
            }

            Path dossierCible = Paths.get(brouillon.getFolderPath());
            int nbFichiers = copierSpecimensDansDossier(dossierCible);

            Map<String, Object> reponse = new LinkedHashMap<>();
            reponse.put("brouillonId", brouillon.getId());
            reponse.put("folderName", brouillon.getFolderName());
            reponse.put("fullPath", brouillon.getFolderPath());
            reponse.put("totalFiles", nbFichiers);
            return ResponseEntity.ok(reponse);
        } catch (Exception e) {
            log.error("Erreur lors du chargement des actes d'exemple", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Impossible de charger les actes d'exemple : " + e.getMessage()));
        }
    }

    private int copierSpecimensDansDossier(Path dossierCible) throws IOException {
        int total = 0;
        for (Map.Entry<String, List<String>> entry : FICHIERS_PAR_DOSSIER.entrySet()) {
            Path sousDossier = dossierCible.resolve(entry.getKey());
            Files.createDirectories(sousDossier);
            for (String nomFichier : entry.getValue()) {
                ClassPathResource resource = new ClassPathResource(BASE_SPECIMENS + nomFichier);
                try (InputStream in = resource.getInputStream()) {
                    Files.copy(in, sousDossier.resolve(nomFichier), StandardCopyOption.REPLACE_EXISTING);
                    total++;
                }
            }
        }
        log.info("{} actes fictifs copiés dans {}", total, dossierCible);
        return total;
    }
}
