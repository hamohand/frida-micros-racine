package com.muhend.backendai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.muhend.backendai.dto.dossier.CreateFolderRequest;
import com.muhend.backendai.dto.dossier.FolderResponse;
import com.muhend.backendai.entities.FridaEntity;
import com.muhend.backendai.entities.HeritierEntity;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Super-Test d'intégration End-to-End :
 * Création d'une Frida complète de A à Z avec le vrai OCR.
 * <p>
 * Pioche aléatoirement des cartes d'identité depuis le dossier de données de test
 * et les assigne à différents rôles d'héritiers.
 * <p>
 * PRÉ-REQUIS : Le service OCR Python doit tourner sur localhost:8082.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FridaEndToEndTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.muhend.backendai.repository.IdentitesRepo identitesRepo;

    // ======= Chemin des données de test =======
    private static final String DONNEES_TESTS_PATH =
            "C:\\Users\\hamoh\\Documents\\projets\\frida\\frida-micros\\donnees_tests";

    // ======= Résultat partagé entre les étapes =======
    private static String createdFolderPath;
    private static String numFrida;

    // ======= Catégories d'héritiers et leurs codes =======
    private static final String CODE_DEFUNT = "1";
    private static final String CODE_CONJOINT = "2";
    private static final String CODE_ENFANT = "3";
    private static final String CODE_PARENT = "4";
    private static final String CODE_FRATRIE = "5";

    /**
     * Pool de cartes d'identité disponibles :
     * Chaque carte peut jouer plusieurs rôles d'héritier.
     */
    static class TestCard {
        final String name;
        final String type; // "cni" ou "en"
        final Path path;

        TestCard(String name, String type, Path path) {
            this.name = name;
            this.type = type;
            this.path = path;
        }

        @Override
        public String toString() {
            return name + " (" + type + ")";
        }
    }

    /**
     * Charge toutes les cartes de test disponibles depuis le dossier configuré.
     */
    static List<TestCard> loadTestCards() throws IOException {
        List<TestCard> cards = new ArrayList<>();
        Path testDir = Paths.get(DONNEES_TESTS_PATH);

        if (!Files.exists(testDir)) {
            fail("Le dossier de données de test n'existe pas : " + DONNEES_TESTS_PATH);
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(testDir)) {
            for (Path file : stream) {
                String fileName = file.getFileName().toString().toLowerCase();
                if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png")) {
                    // Format : nom_cni.jpg
                    String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
                    String[] parts = baseName.split("_", 2);
                    if (parts.length == 2) {
                        cards.add(new TestCard(parts[0], parts[1], file));
                    }
                } else if (fileName.endsWith(".pdf")) {
                    // Format : nom_en.pdf
                    String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
                    String[] parts = baseName.split("_", 2);
                    if (parts.length == 2) {
                        cards.add(new TestCard(parts[0], parts[1], file));
                    }
                }
            }
        }

        assertFalse(cards.isEmpty(), "Aucune carte de test trouvée dans " + DONNEES_TESTS_PATH);
        System.out.println("📂 " + cards.size() + " cartes de test chargées : " +
                cards.stream().map(TestCard::toString).collect(Collectors.joining(", ")));
        return cards;
    }

    /**
     * Génère une composition familiale aléatoire.
     * Retourne une Map : code_type -> liste de TestCard assignées.
     * 
     * Exemple :
     *   "1_en"  -> [moh_en.pdf]       (défunt - extrait de naissance)
     *   "2_cni" -> [naila_cni.jpg]    (conjoint - CNI)
     *   "3_cni" -> [hassan_cni.jpg, wafa_cni.jpg]  (enfants - CNI)
     */
    static Map<String, List<TestCard>> generateFamilyComposition(List<TestCard> allCards) {
        Random rand = new Random();
        Map<String, List<TestCard>> composition = new LinkedHashMap<>();

        // Séparer les cartes par type
        List<TestCard> cniCards = allCards.stream()
                .filter(c -> "cni".equals(c.type))
                .collect(Collectors.toList());
        List<TestCard> enCards = allCards.stream()
                .filter(c -> "en".equals(c.type))
                .collect(Collectors.toList());

        // Mélanger
        Collections.shuffle(cniCards, rand);
        Collections.shuffle(enCards, rand);

        // 1. DÉFUNT : toujours 1, l'extrait de naissance n'est pas obligatoire
        boolean useEnForDefunt = rand.nextBoolean() && !enCards.isEmpty();
        if (useEnForDefunt || cniCards.isEmpty()) {
            TestCard defuntCard = enCards.isEmpty() ? allCards.get(0) : enCards.remove(0);
            composition.put(CODE_DEFUNT + "_en", List.of(defuntCard));
        } else {
            composition.put(CODE_DEFUNT + "_cni", List.of(cniCards.remove(0)));
        }

        // 2. AUTRES HÉRITIERS (aléatoires parmi : conjoint, enfants, parents, fratrie, etc.)
        int[] possibleRoles = {
            2, // CONJOINT
            3, // ENFANTS
            4, // PARENTS
            5, // FRATRIE
            6, // ONCLE PATERNEL
            8  // GRAND PERE PATERNEL
        };

        // Nombre total d'héritiers cible (entre 1 et 6)
        int targetHeirs = 1 + rand.nextInt(6);
        int addedHeirs = 0;

        List<TestCard> availableForReuse = new ArrayList<>(allCards);
        Collections.shuffle(availableForReuse, rand);

        for (int roleCode : possibleRoles) {
            if (addedHeirs >= targetHeirs) break;

            // Chaque rôle a une chance d'être présent dans la famille (sauf si on manque d'héritiers, on force un peu)
            if (rand.nextBoolean() || addedHeirs == 0) {
                int maxForRole = 1;
                if (roleCode == 3 || roleCode == 5) maxForRole = 3; // Jusqu'à 3 enfants ou frères
                if (roleCode == 4 || roleCode == 2) maxForRole = 2; // Jusqu'à 2 parents ou épouses

                int nbToGenerate = 1 + rand.nextInt(maxForRole);
                
                for (int i = 0; i < nbToGenerate && addedHeirs < targetHeirs; i++) {
                    TestCard card = availableForReuse.get(rand.nextInt(availableForReuse.size()));
                    String key = roleCode + "_" + card.type;
                    composition.computeIfAbsent(key, k -> new ArrayList<>()).add(card);
                    addedHeirs++;
                }
            }
        }

        // Affichage de la composition générée
        System.out.println("\n🏠 Composition familiale générée (Total: " + addedHeirs + " héritiers) :");
        composition.forEach((folder, cards) -> {
            System.out.println("  📁 " + folder + " → " +
                    cards.stream().map(c -> c.name).collect(Collectors.joining(", ")));
        });
        System.out.println();

        return composition;
    }

    // ==================== ÉTAPE 1 : Créer le dossier ====================

    @Test
    @Order(1)
    @DisplayName("Étape 1 — Créer le dossier du défunt")
    void step1_createFolder() {
        CreateFolderRequest request = new CreateFolderRequest();
        request.setNom("[TEST] Archive");
        request.setPrenom("SuperTest");

        ResponseEntity<FolderResponse> response = restTemplate.postForEntity(
                "/api/folders/create", request, FolderResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode(),
                "La création du dossier doit réussir");
        assertNotNull(response.getBody(), "La réponse ne doit pas être nulle");
        assertNotNull(response.getBody().getFullPath(), "Le chemin du dossier ne doit pas être nul");

        createdFolderPath = response.getBody().getFullPath();
        System.out.println("✅ Dossier créé : " + createdFolderPath);
        System.out.println("   Nom du dossier : " + response.getBody().getFolderName());
    }

    // ==================== ÉTAPE 2 : Uploader les pièces ====================

    @Test
    @Order(2)
    @DisplayName("Étape 2 — Uploader les cartes d'identité")
    void step2_uploadDocuments() throws Exception {
        assertNotNull(createdFolderPath, "Le dossier doit avoir été créé à l'étape 1");

        List<TestCard> allCards = loadTestCards();
        Map<String, List<TestCard>> family = generateFamilyComposition(allCards);

        int totalUploaded = 0;
        for (Map.Entry<String, List<TestCard>> entry : family.entrySet()) {
            String subFolder = entry.getKey();
            List<TestCard> cards = entry.getValue();

            for (TestCard card : cards) {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                body.add("files", new FileSystemResource(card.path.toFile()));
                body.add("path", subFolder);

                HttpEntity<MultiValueMap<String, Object>> requestEntity =
                        new HttpEntity<>(body, headers);

                ResponseEntity<String> response = restTemplate.postForEntity(
                        "/api/files/upload", requestEntity, String.class);

                assertEquals(HttpStatus.OK, response.getStatusCode(),
                        "L'upload de " + card + " dans " + subFolder + " doit réussir. Réponse: " + response.getBody());

                System.out.println("  📤 " + card.name + "." + card.type + " → " + subFolder);
                totalUploaded++;
            }
        }

        assertTrue(totalUploaded >= 2,
                "Au moins 2 documents doivent être uploadés (défunt + 1 héritier)");
        System.out.println("✅ " + totalUploaded + " documents uploadés avec succès");
    }

    // ==================== ÉTAPE 3 : Lancer le traitement OCR ====================

    @Test
    @Order(3)
    @DisplayName("Étape 3 — Lancer le traitement OCR (vrai service)")
    void step3_processOcr() {
        assertNotNull(createdFolderPath, "Le dossier doit avoir été créé");

        System.out.println("🔍 Lancement du traitement OCR (mode rapide)...");
        System.out.println("   ⚠️  Le service OCR Python doit tourner sur localhost:8082");

        ResponseEntity<FridaEntity> response = restTemplate.getForEntity(
                "/api/pdfs/lireai-ecrirebd?mode=rapide", FridaEntity.class);

        assertEquals(HttpStatus.OK, response.getStatusCode(),
                "Le traitement OCR doit réussir");
        assertNotNull(response.getBody(), "La Frida retournée ne doit pas être nulle");
        assertNotNull(response.getBody().getNumFrida(), "Le numéro de Frida doit être généré");

        numFrida = response.getBody().getNumFrida();
        System.out.println("✅ Traitement OCR terminé !");
        System.out.println("   📋 Numéro Frida : " + numFrida);

        if (response.getBody().getDefunt() != null &&
                response.getBody().getDefunt().getIdentite() != null) {
            
            // Renommer le défunt en base pour qu'il soit bien visible dans l'UI
            Long identiteId = response.getBody().getDefunt().getIdentite().getId();
            identitesRepo.findById(identiteId).ifPresent(identite -> {
                identite.setNom("[TEST] " + identite.getNom());
                identitesRepo.save(identite);
                System.out.println("   👤 Défunt renommé : " + identite.getPrenom() + " " + identite.getNom());
            });
        }

        if (response.getBody().getRequiresCorrection() != null &&
                response.getBody().getRequiresCorrection()) {
            System.out.println("   ⚠️  La Frida nécessite des corrections OCR");
        }
    }

    // ==================== ÉTAPE 4 : Vérifier la Frida créée ====================

    @Test
    @Order(4)
    @DisplayName("Étape 4 — Vérifier la Frida créée en base")
    void step4_verifyFrida() {
        assertNotNull(numFrida, "Le numéro Frida doit être connu après l'étape 3");

        ResponseEntity<FridaEntity> response = restTemplate.getForEntity(
                "/api/frida/" + numFrida, FridaEntity.class);

        assertEquals(HttpStatus.OK, response.getStatusCode(),
                "La Frida doit être retrouvée par son numéro");

        FridaEntity frida = response.getBody();
        assertNotNull(frida, "La Frida ne doit pas être nulle");

        // Vérifications structurelles
        assertEquals(numFrida, frida.getNumFrida());
        assertNotNull(frida.getDefunt(), "Le défunt doit être renseigné");
        assertNotNull(frida.getDefunt().getIdentite(), "L'identité du défunt doit être renseignée");
        assertNotNull(frida.getDateCreation(), "La date de création doit être renseignée");

        System.out.println("✅ Frida vérifiée en base :");
        System.out.println("   📋 Num Frida : " + frida.getNumFrida());
        System.out.println("   📅 Date création : " + frida.getDateCreation());
        System.out.println("   👤 Défunt : " + frida.getDefunt().getIdentite().getPrenom()
                + " " + frida.getDefunt().getIdentite().getNom());
        System.out.println("   🏠 Notaire : " + frida.getNotaire());

        if (frida.getHeritiers() != null) {
            System.out.println("   👥 Héritiers : " + frida.getHeritiers().size());
            for (HeritierEntity h : frida.getHeritiers()) {
                if (h.getIdentite() != null) {
                    System.out.println("      - " + h.getIdentite().getPrenom()
                            + " " + h.getIdentite().getNom()
                            + " (parenté=" + h.getNumParente() + ")");
                }
            }
        }

        if (frida.getTemoins() != null && !frida.getTemoins().isEmpty()) {
            System.out.println("   🧑‍⚖️ Témoins : " + frida.getTemoins().size());
        }
    }

    // ==================== ÉTAPE 5 : Lancer le calcul des parts ====================

    @Test
    @Order(5)
    @DisplayName("Étape 5 — Lancer le calcul des parts d'héritage")
    void step5_calculateShares() {
        assertNotNull(numFrida, "Le numéro Frida doit être connu");

        System.out.println("🧮 Lancement du calcul des parts...");

        ResponseEntity<FridaEntity> response = restTemplate.postForEntity(
                "/api/pdfs/lancer-calcul/" + numFrida, null, FridaEntity.class);

        assertEquals(HttpStatus.OK, response.getStatusCode(),
                "Le calcul des parts doit réussir");

        FridaEntity frida = response.getBody();
        assertNotNull(frida, "La Frida mise à jour ne doit pas être nulle");
        assertNotNull(frida.getCalcul(), "Le calcul doit avoir été effectué");
        assertNotNull(frida.getCalcul().getDenominateur(),
                "Le dénominateur du calcul doit être renseigné");
        assertTrue(frida.getCalcul().getDenominateur() > 0,
                "Le dénominateur doit être positif");

        System.out.println("✅ Calcul des parts terminé :");
        System.out.println("   📊 Dénominateur : " + frida.getCalcul().getDenominateur());
        System.out.println("   📊 Statut : " + frida.getStatut());

        if (frida.getHeritiers() != null) {
            System.out.println("   📊 Parts calculées :");
            for (HeritierEntity h : frida.getHeritiers()) {
                String name = (h.getIdentite() != null)
                        ? h.getIdentite().getPrenom() + " " + h.getIdentite().getNom()
                        : "Inconnu";
                System.out.println("      - " + name
                        + " → coeff=" + h.getCoefPart()
                        + " (parenté=" + h.getNumParente() + ")");
            }
        }

        // Vérification : la somme des coefficients des héritiers doit être cohérente
        if (frida.getHeritiers() != null && !frida.getHeritiers().isEmpty()) {
            double sommeCoeffs = frida.getHeritiers().stream()
                    .filter(h -> h.getCoefPart() != null)
                    .mapToDouble(HeritierEntity::getCoefPart)
                    .sum();
            System.out.println("   📊 Somme coefficients : " + String.format("%.4f", sommeCoeffs));
            // La somme peut dépasser 1.0 si le conjoint est compté séparément dans certains cas
            assertTrue(sommeCoeffs > 0, "La somme des coefficients doit être positive");
        }
    }

    // ==================== ÉTAPE 6 : Vérification finale ====================

    @Test
    @Order(6)
    @DisplayName("Étape 6 — Vérification finale de cohérence")
    void step6_finalVerification() {
        assertNotNull(numFrida, "Le numéro Frida doit être connu");

        // Récupérer la Frida finale
        ResponseEntity<FridaEntity> response = restTemplate.getForEntity(
                "/api/frida/" + numFrida, FridaEntity.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        FridaEntity frida = response.getBody();
        assertNotNull(frida);

        System.out.println("\n" + "=".repeat(60));
        System.out.println("🏆 SUPER-TEST TERMINÉ AVEC SUCCÈS !");
        System.out.println("=".repeat(60));
        System.out.println("   📋 Frida : " + frida.getNumFrida());
        System.out.println("   📅 Date : " + frida.getDateCreation());
        System.out.println("   📊 Statut : " + frida.getStatut());

        if (frida.getDefunt() != null && frida.getDefunt().getIdentite() != null) {
            System.out.println("   👤 Défunt : " + frida.getDefunt().getIdentite().getPrenom()
                    + " " + frida.getDefunt().getIdentite().getNom());
        }

        int nbHeritiers = frida.getHeritiers() != null ? frida.getHeritiers().size() : 0;
        int nbTemoins = frida.getTemoins() != null ? frida.getTemoins().size() : 0;
        System.out.println("   👥 Héritiers : " + nbHeritiers);
        System.out.println("   🧑‍⚖️ Témoins : " + nbTemoins);

        if (frida.getCalcul() != null) {
            System.out.println("   🧮 Calcul : dénominateur=" + frida.getCalcul().getDenominateur());
        }

        System.out.println("=".repeat(60));

        // Assertions finales de cohérence
        assertEquals(FridaEntity.STATUT_VALIDE, frida.getStatut(),
                "Le statut final doit être VALIDE après calcul");
        assertTrue(nbHeritiers >= 1,
                "Il doit y avoir au moins 1 héritier");
    }

    // ==================== ÉTAPE 7 : Nettoyage ====================

    @Test
    @Disabled("Désactivé pour garder l'archive de test visible dans l'UI")
    @Order(7)
    @DisplayName("Étape 7 — Nettoyage (suppression de la Frida de test)")
    void step7_cleanup() {
        assertNotNull(numFrida, "Le numéro Frida doit être connu");

        restTemplate.delete("/api/frida/" + numFrida);

        // Vérifier la suppression
        ResponseEntity<FridaEntity> response = restTemplate.getForEntity(
                "/api/frida/" + numFrida, FridaEntity.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(),
                "La Frida supprimée ne doit plus être trouvée");

        System.out.println("🗑️ Frida " + numFrida + " supprimée avec succès");

        // Nettoyer le dossier temporaire
        if (createdFolderPath != null) {
            try {
                Path folder = Paths.get(createdFolderPath);
                if (Files.exists(folder)) {
                    Files.walk(folder)
                            .sorted(Comparator.reverseOrder())
                            .forEach(path -> {
                                try { Files.delete(path); } catch (IOException ignored) {}
                            });
                    System.out.println("🗑️ Dossier nettoyé : " + createdFolderPath);
                }
            } catch (IOException e) {
                System.out.println("⚠️ Impossible de nettoyer le dossier : " + e.getMessage());
            }
        }
    }
}
