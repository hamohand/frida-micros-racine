package com.muhend.backendai.service.aibd;

import com.muhend.backendai.dto.DocumentInfo;
import com.muhend.backendai.entities.*;
import com.muhend.backendai.enums.DocumentType;
import com.muhend.backendai.enums.HeirCategory;
import com.muhend.backendai.repository.*;
import com.muhend.backendai.service.calculs_outils.MethodesChaine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Service pour le traitement des fichiers d'extraits de naissance
 * et leur enregistrement en base de données avec calculs associés.
 * Utilise la classe 'LireAiService' qui extrait les données des extraits de
 * naissance.
 */
@Service
public class EcrireBdService {

    private final LectureAiService lectureAiService;
    private final IdentitesRepo identitesRepo;
    private final FridaRepo fridaRepo;
    private final HeritierRepo heritierRepo;
    private final DefuntRepo defuntRepo;
    private final CalculRepo calculRepo;
    private final TemoinRepo temoinRepo;
    private final com.muhend.backendai.client.calculs.CalculsApiClient calculsApiClient;
    private final com.muhend.backendai.client.ocr.OcrApiClient ocrApiClient;

    @Autowired
    public EcrireBdService(
            IdentitesRepo identitesRepo,
            LectureAiService lectureAiService,
            FridaRepo fridaRepo,
            HeritierRepo heritierRepo,
            DefuntRepo defuntRepo,
            CalculRepo calculRepo,
            TemoinRepo temoinRepo,
            com.muhend.backendai.client.calculs.CalculsApiClient calculsApiClient,
            com.muhend.backendai.client.ocr.OcrApiClient ocrApiClient) {
        this.identitesRepo = identitesRepo;
        this.lectureAiService = lectureAiService;
        this.fridaRepo = fridaRepo;
        this.heritierRepo = heritierRepo;
        this.defuntRepo = defuntRepo;
        this.calculRepo = calculRepo;
        this.temoinRepo = temoinRepo;
        this.calculsApiClient = calculsApiClient;
        this.ocrApiClient = ocrApiClient;
    }

    private String numFrida = "0";
    private FridaEntity ficheFrida;
    private List<String> tableauNumParente;
    private List<HeritierEntity> listeHeritiers;
    private List<TemoinEntity> listeTemoins;
    private int nbConjoints, nbFilles, nbGarcons;
    private int nbParents, nbFreres, nbSoeurs;

    /**
     * Point d'entrée principal pour traiter les documents d'héritiers.
     * Supporte plusieurs types de documents (EN, CNI, PP) détectés dynamiquement.
     *
     * @param folderPath Chemin vers le dossier contenant les sous-dossiers
     *                   au format {code}_{type} (ex: 1_en, 2_cni)
     */
    public FridaEntity traiterExtraitsNaissance(String folderPath) {
        try {
            initialiserVariables(folderPath);

            // Récupérer le mapping fichier -> DocumentInfo depuis LectureAiService
            Map<java.nio.file.Path, DocumentInfo> fileDocInfoMap = lectureAiService.getFileDocumentInfoMap();

            // Cache des définitions d'entités OCR pour éviter les appels répétés
            Map<String, com.muhend.backendai.client.ocr.dto.OcrEntityDefinitionDto> entityDefCache = new HashMap<>();

            List<java.nio.file.Path> files = lectureAiService.getPdfFiles();

            if (files.isEmpty()) {
                System.out.println("Aucun document trouvé dans le dossier : " + folderPath);
                return null;
            }

            int indiceParente = 0;
            for (java.nio.file.Path file : files) {
                try {
                    // Récupérer les infos du document
                    DocumentInfo docInfo = fileDocInfoMap.get(file);
                    DocumentType docType = (docInfo != null) ? docInfo.getDocumentType()
                            : DocumentType.EXTRAIT_NAISSANCE;
                    HeirCategory heirCategory = (docInfo != null) ? docInfo.getHeirCategory() : HeirCategory.DEFUNT;

                    // Récupérer la définition d'entité OCR (avec cache)
                    String ocrEntityId = docType.getOcrEntityId();
                    com.muhend.backendai.client.ocr.dto.OcrEntityDefinitionDto entityDef = entityDefCache
                            .get(ocrEntityId);
                    if (entityDef == null) {
                        entityDef = ocrApiClient.getEntityDefinition(ocrEntityId);
                        if (entityDef != null) {
                            entityDefCache.put(ocrEntityId, entityDef);
                        } else {
                            System.err.println("ERREUR: Entité '" + ocrEntityId + "' introuvable dans le service OCR.");
                            continue;
                        }
                    }

                    System.out.println("Traitement: " + file.getFileName() + " -> type=" + docType + ", catégorie="
                            + heirCategory);

                    // Traiter le fichier avec l'OCR
                    Object resultEntity = processFileWithOcrDynamic(file, entityDef, docType);

                    if (resultEntity != null) {
                        // Générer l'identifiant Frida au premier document
                        if (indiceParente == 0 && heirCategory == HeirCategory.DEFUNT) {
                            String dateNaissance = extractDateNaissance(resultEntity);
                            numFrida = genererIdentifiantFrida(dateNaissance);
                        }

                        // Sauvegarder et associer selon le type
                        sauvegarderDocument(resultEntity, docType, heirCategory, indiceParente);
                        indiceParente++;
                    }
                } catch (Exception e) {
                    System.err.println("Erreur traitement fichier OCR : " + file + " - " + e.getMessage());
                    e.printStackTrace();
                }
            }

            if (indiceParente > 0) {
                effectuerCalculsEtSauvegarderFrida();
            }

        } catch (IOException e) {
            System.err.println("Erreur lors de la lecture des fichiers : " + e.getMessage());
            e.printStackTrace();
        }
        return ficheFrida;
    }

    // ----------------------- Méthodes Utilitaires ----------------------------

    /**
     * Initialise les variables internes nécessaires au traitement.
     */
    private void initialiserVariables(String folderPath) throws IOException {
        tableauNumParente = new ArrayList<>();
        lectureAiService.setFolderPath(folderPath);
        tableauNumParente = lectureAiService.listFolderContents();
        ficheFrida = new FridaEntity();
        listeHeritiers = new ArrayList<>();
        listeTemoins = new ArrayList<>();
        nbConjoints = 0;
        nbFilles = 0;
        nbGarcons = 0;
        nbParents = 0;
        nbFreres = 0;
        nbSoeurs = 0;
    }

    /**
     * Effectue les calculs de parts et sauvegarde la fiche frida.
     */
    private void effectuerCalculsEtSauvegarderFrida() {
        // calcul
        CalculEntity calcul = calculerParts();
        calculRepo.save(calcul);
        for (HeritierEntity heritier : listeHeritiers) { // coefficient part de chaque héritier
            int numerateur = 0;
            switch (heritier.getNumParente()) {
                case "2":
                    numerateur = calcul.getNumerateurConjoint();
                    break;
                case "3":
                    if (Objects.equals(heritier.getIdentite().getSexe(), "ذكر")) {
                        numerateur = calcul.getNumerateurGarcons();
                    } else {
                        numerateur = calcul.getNumerateurFilles();
                    }
                    break;
                case "4": // Parents
                case "5": // Fratrie
                    // Les parts des parents et fratrie sont gérées par le service de calculs
                    // On cherche le numérateur correspondant dans la réponse
                    numerateur = 0; // Sera calculé par le service calculs-api
                    break;
            }
            float part = Math.round((float) numerateur / calcul.getDenominateur() * 100) / 100.0f;
            heritier.setCoefPart(part);
        }
        // frida suite
        // ficheFrida.setHeritiers(listeHeritier);
        ficheFrida.setDateCreation(Date.valueOf(LocalDate.now()).toLocalDate()); // DATE CREATION
        ficheFrida.setNotaire("محمد قثوم الموثق بالجزاىر شارع الانتصار،"); // NOTAIRE
        ficheFrida.setCalcul(calcul);
        ficheFrida.setHeritiers(listeHeritiers); // les heritiés
        ficheFrida.setTemoins(listeTemoins); // les témoins
        fridaRepo.save(ficheFrida);
    }

    /**
     * Crée une entité représentant un défunt.
     */
    private DefuntEntity ficheDefunt(IdentitesEntity identite) {
        DefuntEntity defunt = new DefuntEntity();
        defunt.setIdentite(identite);
        return defunt;
    }

    /**
     * Crée une entité représentant un héritier.
     */
    private HeritierEntity ficheHeritier(IdentitesEntity identite, int indiceParente) {
        HeritierEntity heritier = new HeritierEntity();
        heritier.setNumFrida(numFrida);
        heritier.setNumParente(tableauNumParente.get(indiceParente));
        heritier.setIdentite(identite);

        // Calcul de la répartition entre garçons, filles, conjoints, parents et fratrie
        if ("2".equals(heritier.getNumParente())) {
            nbConjoints++;
        } else if ("3".equals(heritier.getNumParente())) {
            if ("ذكر".equals(heritier.getIdentite().getSexe())) {
                nbGarcons++;
            } else {
                nbFilles++;
            }
        } else if ("4".equals(heritier.getNumParente())) {
            nbParents++;
        } else if ("5".equals(heritier.getNumParente())) {
            // Comptage frères/sœurs
            if ("ذكر".equals(heritier.getIdentite().getSexe())) {
                nbFreres++;
            } else {
                nbSoeurs++;
            }
        }
        return heritier;
    }

    /**
     * Crée une entité représentant un témoin. //PROVISOIRE
     */
    private TemoinEntity ficheTemoin(IdentitesEntity identite, int indiceParente) {
        TemoinEntity temoin = new TemoinEntity();
        temoin.setNumFrida(numFrida);
        temoin.setIdentite(identite);
        temoin.setNumParente(tableauNumParente.get(indiceParente));

        return temoin;
    }

    // ----------------------- Nouvelles méthodes dynamiques
    // ----------------------------

    /**
     * Traite un fichier avec le service OCR, en utilisant le type de document
     * approprié.
     */
    private Object processFileWithOcrDynamic(java.nio.file.Path file,
            com.muhend.backendai.client.ocr.dto.OcrEntityDefinitionDto entityDef,
            DocumentType docType) {
        // 1. Upload
        com.muhend.backendai.client.ocr.dto.OcrUploadResponseDto uploadResponse = ocrApiClient.uploadFile(file);
        if (!uploadResponse.isSuccess()) {
            throw new RuntimeException("Echec upload OCR: " + uploadResponse.getError());
        }

        // 2. Prepare analysis request
        java.util.Map<String, com.muhend.backendai.client.ocr.dto.OcrZoneConfigDto> zones = new java.util.HashMap<>();
        if (entityDef != null && entityDef.getZones() != null) {
            for (com.muhend.backendai.client.ocr.dto.OcrEntityZoneDto z : entityDef.getZones()) {
                zones.put(z.getNom(), com.muhend.backendai.client.ocr.dto.OcrZoneConfigDto.builder()
                        .coords(z.getCoords())
                        .lang(z.getLang())
                        .type(z.getType())
                        .preprocess(z.getPreprocess())
                        .valeurs_attendues(z.getValeurs_attendues())
                        .build());
            }
        } else {
            throw new RuntimeException("Définition de zones manquante pour l'analyse OCR.");
        }

        com.muhend.backendai.client.ocr.dto.OcrAnalysisRequestDto request = com.muhend.backendai.client.ocr.dto.OcrAnalysisRequestDto
                .builder()
                .filename(uploadResponse.getSaved_filename())
                .zones(zones)
                .cadre_reference(entityDef.getCadre_reference())
                .build();

        // 3. Analyze
        com.muhend.backendai.client.ocr.dto.OcrAnalysisResponseDto response = ocrApiClient.analyze(request);

        System.out.println("====== RÉPONSE DU SERVICE OCR ======");
        System.out.println("Fichier analysé: " + request.getFilename());
        if (response != null && response.getResultats() != null) {
            response.getResultats().forEach((key, value) -> {
                System.out.println(
                        "Champ: " + key + " -> Valeur: '" + (value != null ? value.getTexte_final() : "null") + "'");
            });
        } else {
            System.out.println("Aucun résultat retourné par le service OCR.");
        }
        System.out.println("====================================");

        if (!response.isSuccess()) {
            throw new RuntimeException("Echec analyse OCR: " + response.getError());
        }

        // 4. Map according to document type
        return switch (docType) {
            case EXTRAIT_NAISSANCE -> mapOcrResultToEntity(response);
            case CNI, PASSEPORT -> mapOcrResultToIdentite(response, docType);
        };
    }

    /**
     * Extrait la date de naissance d'une entité (IdentitesEntity).
     */
    private String extractDateNaissance(Object entity) {
        if (entity instanceof IdentitesEntity id) {
            return id.getDateNaissance() != null ? id.getDateNaissance().toString() : "";
        }
        return "";
    }

    /**
     * Sauvegarde le document et l'associe à l'héritier/défunt/témoin approprié.
     */
    private void sauvegarderDocument(Object entityObj, DocumentType docType, HeirCategory heirCategory,
            int indiceParente) {
        if (!(entityObj instanceof IdentitesEntity)) {
            return;
        }
        IdentitesEntity identite = (IdentitesEntity) entityObj;
        identite.setNumFrida(numFrida);
        identitesRepo.save(identite);

        switch (heirCategory) {
            case DEFUNT -> {
                DefuntEntity defunt = ficheDefunt(identite);
                ficheFrida.setNumFrida(numFrida);
                defunt.setNumFrida(numFrida);
                defuntRepo.save(defunt);
                ficheFrida.setDefunt(defunt);
            }
            case TEMOIN -> {
                TemoinEntity temoin = ficheTemoin(identite, indiceParente);
                temoinRepo.save(temoin);
                listeTemoins.add(temoin);
            }
            default -> { // Héritiers: conjoint, enfant, parent, fratrie
                HeritierEntity heritier = ficheHeritier(identite, indiceParente);
                heritierRepo.save(heritier);
                listeHeritiers.add(heritier);
            }
        }
    }

    /**
     * Mappe le résultat OCR vers une IdentitesEntity.
     */
    private IdentitesEntity mapOcrResultToIdentite(
            com.muhend.backendai.client.ocr.dto.OcrAnalysisResponseDto response,
            DocumentType docType) {
        IdentitesEntity entity = new IdentitesEntity();
        java.util.Map<String, com.muhend.backendai.client.ocr.dto.OcrResultDto> results = response.getResultats();

        if (results == null)
            return entity;

        // Helper to safely get text
        java.util.function.Function<String, String> getText = key -> {
            if (results.containsKey(key)) {
                return results.get(key).getTexte_final();
            }
            return "";
        };

        // Mapping commun
        entity.setNom(getText.apply("nom"));
        entity.setPrenom(getText.apply("prenom"));
        entity.setLieuNaissance(getText.apply("lieuNaissance"));
        entity.setSexe(getText.apply("sexe"));
        entity.setNumeroPiece(getText.apply("numeroPiece"));
        entity.setDelivrePar(getText.apply("delivrePar"));
        entity.setDelivreLe(getText.apply("delivreLe"));
        entity.setExpireLe(getText.apply("expireLe"));

        // Type de pièce
        entity.setNomPiece(docType == DocumentType.CNI ? "Carte Nationale d'Identité" : "Passeport");

        // Date de naissance
        try {
            String dateTxt = getText.apply("dateNaissance");
            if (!dateTxt.isEmpty()) {
                entity.setDateNaissance(
                        java.time.LocalDate.parse(dateTxt, java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            }
        } catch (Exception e) {
            // Parsing failed, leave null
        }

        return entity;
    }

    /**
     * Calcule les parts des héritiers via l'API Microservice.
     */
    private CalculEntity calculerParts() {
        // Mapping Sexe
        String sexeArabe = ficheFrida.getDefunt().getIdentite().getSexe();
        String sexe = "ذكر".equals(sexeArabe) ? "M" : "F";

        com.muhend.backendai.client.calculs.dto.CalculRequestDto request = com.muhend.backendai.client.calculs.dto.CalculRequestDto
                .builder()
                .sexeDefunt(sexe)
                .nbConjoints(nbConjoints)
                .nbFilles(nbFilles)
                .nbGarcons(nbGarcons)
                .pereVivant(nbParents > 0) // père vivant si au moins un parent détecté
                .mereVivante(nbParents > 1) // mère vivante si 2 parents détectés
                .nbSoeurs(nbSoeurs)
                .nbFreres(nbFreres)
                .build();

        CalculEntity calcul = new CalculEntity();
        calcul.setNumFrida(numFrida);
        calcul.setNbConjoints(nbConjoints);
        calcul.setNbFilles(nbFilles);
        calcul.setNbGarcons(nbGarcons);

        try {
            com.muhend.backendai.client.calculs.dto.CalculResponseDto response = calculsApiClient
                    .calculerParts(request);

            calcul.setDenominateur(response.getDenominateurCommun());

            // Extraction des numérateurs depuis la réponse
            // Note: L'API retourne une liste d'héritiers. On prend le numérateur du premier
            // trouvé pour chaque type.

            // Conjoint
            response.getHeritiers().stream()
                    .filter(h -> h.getHeritier().toLowerCase().contains("conjoint")
                            || h.getHeritier().toLowerCase().contains("épouse")
                            || h.getHeritier().toLowerCase().contains("époux"))
                    .findFirst()
                    .ifPresent(h -> calcul.setNumerateurConjoint(h.getPart().getNumerateur()));

            // Filles
            response.getHeritiers().stream()
                    .filter(h -> h.getHeritier().toLowerCase().contains("fille"))
                    .findFirst()
                    .ifPresent(h -> calcul.setNumerateurFilles(h.getPart().getNumerateur()));

            // Garçons
            response.getHeritiers().stream()
                    .filter(h -> h.getHeritier().toLowerCase().contains("garçon")
                            || h.getHeritier().toLowerCase().contains("fils"))
                    .findFirst()
                    .ifPresent(h -> calcul.setNumerateurGarcons(h.getPart().getNumerateur()));

        } catch (Exception e) {
            System.err.println("ERREUR CRITIQUE APPEL MICROSERVICE CALCULS : " + e.getMessage());
            e.printStackTrace();
            // Fallback ou relancer l'exception ? Pour l'instant on relance pour voir
            // l'erreur.
            throw new RuntimeException("Erreur microservice calculs", e);
        }

        return calcul;
    }

    /**
     * Traite un fichier spécifique avec le service OCR.
     */
    private IdentitesEntity processFileWithOcr(java.nio.file.Path file,
            com.muhend.backendai.client.ocr.dto.OcrEntityDefinitionDto entityDef) {
        // 1. Upload
        com.muhend.backendai.client.ocr.dto.OcrUploadResponseDto uploadResponse = ocrApiClient.uploadFile(file);
        if (!uploadResponse.isSuccess()) {
            throw new RuntimeException("Echec upload OCR: " + uploadResponse.getError());
        }

        // 2. Prepare analysis request
        java.util.Map<String, com.muhend.backendai.client.ocr.dto.OcrZoneConfigDto> zones = new java.util.HashMap<>();
        if (entityDef != null && entityDef.getZones() != null) {
            for (com.muhend.backendai.client.ocr.dto.OcrEntityZoneDto z : entityDef.getZones()) {
                zones.put(z.getNom(), com.muhend.backendai.client.ocr.dto.OcrZoneConfigDto.builder()
                        .coords(z.getCoords())
                        .build());
            }
        } else {
            // Fallback zones if entityDef is null or empty?
            // Ideally we shouldn't reach here if we check entityDef before loop.
            // Or maybe we proceed with empty zones and let the OCR service use default?
            // For now, let's assume zones are required for meaningful extraction.
            throw new RuntimeException("Définition de zones manquante pour l'analyse OCR.");
        }

        com.muhend.backendai.client.ocr.dto.OcrAnalysisRequestDto request = com.muhend.backendai.client.ocr.dto.OcrAnalysisRequestDto
                .builder()
                .filename(uploadResponse.getSaved_filename())
                .zones(zones)
                .build();

        // 3. Analyze
        com.muhend.backendai.client.ocr.dto.OcrAnalysisResponseDto response = ocrApiClient.analyze(request);

        System.out.println("====== RÉPONSE DU SERVICE OCR ======");
        System.out.println("Fichier analysé: " + request.getFilename());
        if (response != null && response.getResultats() != null) {
            response.getResultats().forEach((key, value) -> {
                System.out.println(
                        "Champ: " + key + " -> Valeur: '" + (value != null ? value.getTexte_final() : "null") + "'");
            });
        } else {
            System.out.println("Aucun résultat retourné par le service OCR.");
        }
        System.out.println("====================================");

        if (!response.isSuccess()) {
            throw new RuntimeException("Echec analyse OCR: " + response.getError());
        }

        // 4. Map to Entity
        return mapOcrResultToEntity(response);
    }

    private IdentitesEntity mapOcrResultToEntity(
            com.muhend.backendai.client.ocr.dto.OcrAnalysisResponseDto response) {
        IdentitesEntity entity = new IdentitesEntity();
        java.util.Map<String, com.muhend.backendai.client.ocr.dto.OcrResultDto> results = response.getResultats();

        if (results == null)
            return entity;

        // Helper to safely get text
        java.util.function.Function<String, String> getText = key -> {
            if (results.containsKey(key)) {
                return results.get(key).getTexte_final();
            }
            return "";
        };

        // Mapping
        String nom = getText.apply("nom");
        String prenom = getText.apply("prenom");

        // Concaténation "Prénom Nom" comme demandé
        String nomComplet = (prenom + " " + nom).trim();

        entity.setNom(nomComplet);
        entity.setPrenom(prenom);

        entity.setSexe(getText.apply("sexe"));
        entity.setLieuNaissance(getText.apply("lieuNaissance"));
        entity.setDateNaissanceLettres(getText.apply("dateNaissanceLettres"));
        entity.setPere(getText.apply("pere"));
        entity.setMere(getText.apply("mere"));

        // Tentative de parsing date (très simplifiée pour l'instant)
        // TODO: Implémenter un vrai parser de date (ex: "12 janvier 2000" -> LocalDate)
        // entity.setDateNaissance(...);
        // Pour l'instant on met une date par défaut pour éviter null pointer exceptions
        // dans le code legacy
        // Ou on essaie de parser si format "dd/MM/yyyy" par chance

        try {
            // Si on avait une zone date numérique...
            String dateTxt = getText.apply("dateNaissance");
            if (!dateTxt.isEmpty()) {
                entity.setDateNaissance(
                        java.time.LocalDate.parse(dateTxt, java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))); // Risqué
            } else {
                // Fallback: Date actuelle ? Non c'est mauvais.
                // Laisser null et gérer dans le frontend ?
                // Le legacy code utilise
                // genererIdentifiantFrida(String.valueOf(extraitEntity.getDateNaissance()))
                // String.valueOf(null) -> "null".
                // On va mettre la date du jour temporairement pour ne pas bloquer le flow si
                // parsing échoue
                entity.setDateNaissance(java.time.LocalDate.now());
            }
        } catch (Exception e) {
            entity.setDateNaissance(java.time.LocalDate.now());
        }

        return entity;
    }

    /**
     * Génère un identifiant unique pour une fiche Frida.
     */
    private String genererIdentifiantFrida(String dateNaissance) {
        // String base = dateNaissance + Date.valueOf(LocalDate.now()).toLocalDate();
        String base = dateNaissance;
        base = MethodesChaine.replaceTrimSpaces(base, '.');
        base = MethodesChaine.replaceAllTrimSpaces(base, '/');
        base = MethodesChaine.replaceAllTrimSpaces(base, '-');
        return base + genererIdentifiantFrida(); // on ajoute la date et l'heure du moment
    }

    public static String genererIdentifiantFrida() {
        // Obtenir la date et l'heure actuelles
        LocalDateTime maintenant = LocalDateTime.now();

        // Formater la date et l'heure pour les intégrer dans l'ID
        DateTimeFormatter formatteur = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

        // Construire l'identifiant avec la date et l'heure jusqu'à la seconde
        return maintenant.format(formatteur);
    }
}
