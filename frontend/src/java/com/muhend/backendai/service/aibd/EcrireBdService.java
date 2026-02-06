package com.muhend.backendai.service.aibd;

import com.google.cloud.documentai.v1.Document;
import com.muhend.backendai.entities.*;
import com.muhend.backendai.repository.*;
import com.muhend.backendai.service.calculs_outils.CalculPartsService;
import com.muhend.backendai.service.calculs_outils.MethodesChaine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Service pour le traitement des fichiers d'extraits de naissance
 * et leur enregistrement en base de données avec calculs associés.
 * Utilise la classe 'LireAiService' qui extrait les données des extraits de naissance.
 */
@Service
public class EcrireBdService {

    private final LectureAiService lectureAiService;
    private final ExtraitNaissanceRepo extraitNaissanceRepo;
    private final FridaRepo fridaRepo;
    private final HeritierRepo heritierRepo;
    private final DefuntRepo defuntRepo;
    private final CalculRepo calculRepo;
    private final TemoinRepo temoinRepo;

    @Autowired
    public EcrireBdService(
            ExtraitNaissanceRepo extraitNaissanceRepo,
            LectureAiService lectureAiService,
            FridaRepo fridaRepo,
            HeritierRepo heritierRepo,
            DefuntRepo defuntRepo,
            CalculRepo calculRepo, TemoinRepo temoinRepo) {
        this.extraitNaissanceRepo = extraitNaissanceRepo;
        this.lectureAiService = lectureAiService;
        this.fridaRepo = fridaRepo;
        this.heritierRepo = heritierRepo;
        this.defuntRepo = defuntRepo;
        this.calculRepo = calculRepo;
        this.temoinRepo = temoinRepo;
    }

    private String numFrida = "0";
    private FridaEntity ficheFrida;
    private List<String> tableauNumParente;
    private List<HeritierEntity> listeHeritiers;
    private List<TemoinEntity> listeTemoins;
    private int nbConjoints, nbFilles, nbGarcons;

    /**
     * Point d'entrée principal pour traiter les extraits de naissance par l'IA.
     *
     * @param folderPath Chemin vers le dossier contenant les fichiers des extraits de naissance.
     */
    public FridaEntity traiterExtraitsNaissance(String folderPath) {
        try {
            initialiserVariables(folderPath);

            List<Document> reponsesIA = lectureAiService.lecturePdfsAi();
            if (reponsesIA.isEmpty()) {
                System.out.println("Aucun document trouvé dans le dossier : " + folderPath);
                return null;
            }

            processDocuments(reponsesIA);

            effectuerCalculsEtSauvegarderFrida();

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
    }

    /**
     * Traite les documents récupérés dans le dossier.
     *
     * @param documents Liste des documents à traiter.
     */
    private void processDocuments(List<Document> documents) {
        int indiceParente = 0;

        for (Document document : documents) {
            ExtraitNaissanceEntity extraitEntity = LectureExtraitAi.ficheExtraitNaissanceEntity(document);
            // Numéro de frida
            if (indiceParente == 0) {
                numFrida = genererIdentifiantFrida(String.valueOf(extraitEntity.getDateNaissance()));
                //numFrida = extraitEntity.getDateNaissance() + Date.valueOf(LocalDate.now()).toLocalDate();
            }
            // extrait de naissance
            extraitEntity.setNumFrida(numFrida);
            extraitNaissanceRepo.save(extraitEntity);
            // héritiers
            creerEntitesAssociees(extraitEntity, indiceParente);
            indiceParente++;
        }
        //Provisoire
        /*listeTemoins.add(ficheTemoin(2L));
        listeTemoins.add(ficheTemoin(3L));*/
    }

    /**
     * Effectue les calculs de parts et sauvegarde la fiche frida.
     */
    private void effectuerCalculsEtSauvegarderFrida() {
        //calcul
        CalculEntity calcul = calculerParts();
        calculRepo.save(calcul);
        for (HeritierEntity heritier : listeHeritiers) { // coefficient part de chaque héritier
            int numerateur = 0;
            switch (heritier.getNumParente()) {
                case "2": numerateur = calcul.getNumerateurConjoint(); break;
                case "3":
                    if (Objects.equals(heritier.getExtraitNaissance().getSexe(), "ذكر")){
                        numerateur = calcul.getNumerateurGarcons();
                    }else {
                        numerateur = calcul.getNumerateurFilles();
                    }
                    break;
            }
            float part = Math.round((float) numerateur / calcul.getDenominateur() * 100) / 100.0f;
            heritier.setCoefPart(part);
        }
        //frida suite
        //ficheFrida.setHeritiers(listeHeritier);
        ficheFrida.setDateCreation(Date.valueOf(LocalDate.now()).toLocalDate()); //DATE CREATION
        ficheFrida.setNotaire("محمد قثوم الموثق بالجزاىر شارع الانتصار،"); //NOTAIRE
        ficheFrida.setCalcul(calcul);
        ficheFrida.setHeritiers(listeHeritiers); // les heritiés
        ficheFrida.setTemoins(listeTemoins); // les témoins
        fridaRepo.save(ficheFrida);
    }

    /**
     * Crée les entités liées à un extrait, notamment le défunt ou héritiers.
     */
    private void creerEntitesAssociees(ExtraitNaissanceEntity extrait, int indiceParente) {
        String typeParente = tableauNumParente.get(indiceParente);
        if ("1".equals(typeParente)) { // 1 = Défunt
            DefuntEntity defunt = ficheDefunt(extrait);
            ficheFrida.setNumFrida(numFrida);
            defunt.setNumFrida(numFrida);
            defuntRepo.save(defunt);
            ficheFrida.setDefunt(defunt);
        } else if ("11".equals(typeParente)) { // témoins
            TemoinEntity temoin = ficheTemoin(extrait, indiceParente);
            temoinRepo.save(temoin);
            listeTemoins.add(temoin);
        } else { // Héritiers : conjoint ou enfants
            HeritierEntity heritier = ficheHeritier(extrait, indiceParente);
            heritierRepo.save(heritier);
            listeHeritiers.add(heritier);
        }
    }

    /**
     * Crée une entité représentant un défunt.
     */
    private DefuntEntity ficheDefunt(ExtraitNaissanceEntity extrait) {
        DefuntEntity defunt = new DefuntEntity();
        //defunt.setNumFrida(numFrida);
        defunt.setExtraitNaissance(extrait);
        return defunt;
    }

    /**
     * Crée une entité représentant un héritier.
     */
    private HeritierEntity ficheHeritier(ExtraitNaissanceEntity extrait, int indiceParente) {
        HeritierEntity heritier = new HeritierEntity();
        heritier.setNumFrida(numFrida);
        heritier.setNumParente(tableauNumParente.get(indiceParente));
        heritier.setExtraitNaissance(extrait);

        // Calcul de la répartition entre garçons, filles et conjoints
        if ("2".equals(heritier.getNumParente())) {
            nbConjoints++;
        } else if ("3".equals(heritier.getNumParente())) {
            if ("ذكر".equals(heritier.getExtraitNaissance().getSexe())) {
                nbGarcons++;
            } else {
                nbFilles++;
            }
        }
        return heritier;
    }

    /**
     * Crée une entité représentant un témoin. //PROVISOIRE
     */
    private TemoinEntity ficheTemoin(ExtraitNaissanceEntity extrait, int indiceParente) {
        TemoinEntity temoin = new TemoinEntity();
        temoin.setNumFrida(numFrida);
        temoin.setExtraitNaissance(extrait);
        temoin.setNumParente(tableauNumParente.get(indiceParente));

        return temoin;
    }

    /**
     * Calcule les parts des héritiers.
     */
    private CalculEntity calculerParts() {
        CalculPartsService calculService = new CalculPartsService(nbFilles, nbGarcons, nbConjoints);
        calculService.calculPartsEnfants();

        CalculEntity calcul = new CalculEntity();
        calcul.setNumFrida(numFrida);
        calcul.setNbConjoints(nbConjoints);
        calcul.setNbFilles(nbFilles);
        calcul.setNbGarcons(nbGarcons);
        calcul.setDenominateur(calculService.getFractions().get("denominateur"));
        calcul.setNumerateurConjoint(calculService.getFractions().get("numerateurConjoint"));
        calcul.setNumerateurFilles(calculService.getFractions().get("numerateurFille"));
        calcul.setNumerateurGarcons(calculService.getFractions().get("numerateurGarcon"));

        return calcul;
    }

    /**
     * Génère un identifiant unique pour une fiche Frida.
     */
    private String genererIdentifiantFrida(String dateNaissance) {
        //String base = dateNaissance + Date.valueOf(LocalDate.now()).toLocalDate();
        String base = dateNaissance;
        base = MethodesChaine.replaceTrimSpaces(base, '.');
        base = MethodesChaine.replaceAllTrimSpaces(base, '/');
        base = MethodesChaine.replaceAllTrimSpaces(base, '-');
        return base + genererIdentifiantFrida(); //on ajoute la date et l'heure du moment
    }
    public static String genererIdentifiantFrida() {
        // Obtenir la date et l'heure actuelles
        LocalDateTime maintenant = LocalDateTime.now();

        // Formater la date et l'heure pour les intégrer dans l'ID
        DateTimeFormatter formatteur = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

        // Générer un UUID pour ajouter à l'identifiant
        //String uuid = UUID.randomUUID().toString().substring(0, 12); // Garder seulement les 12 premiers caractères (enlever les secondes)

        // Construire l'identifiant
        //return horodatage + "_" + uuid;
        return maintenant.format(formatteur);
    }
}
