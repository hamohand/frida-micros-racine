package com.muhend.backendai.service.aibd;

import com.google.cloud.documentai.v1.Document;
import com.muhend.backendai.entities.IdentitesEntity;
import com.muhend.backendai.service.calculs_outils.MethodesChaine;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Service
public class LectureExtraitAi {

    /*
     * private final ExtraitNaissanceRepo extraitNaissanceRepo;
     * 
     * public LecturePdfAi(ExtraitNaissanceRepo extraitNaissanceRepo) {
     * this.extraitNaissanceRepo = extraitNaissanceRepo;
     * }
     * 
     * @Autowired
     */

    /**
     * Constructeur pour l'injection des dépendances.
     *
     * @param docPdfResponseAi document réponse de l'IA.
     */

    // Création d'une fiche Extrait de naissance champ par champ
    public static IdentitesEntity ficheExtraitNaissanceEntity(Document docPdfResponseAi) {
        String type;
        String mentionText;
        IdentitesEntity fiche = new IdentitesEntity();
        int nbChamps = docPdfResponseAi.getEntitiesCount(); // nombre de champs de la table

        for (int i_entitie = 0; i_entitie < nbChamps; i_entitie++) {

            type = docPdfResponseAi.getEntities(i_entitie).getType();
            mentionText = docPdfResponseAi.getEntities(i_entitie).getMentionText();
            mentionText = MethodesChaine.replaceTrimSpaces(mentionText, '.'); // supprime les éventuels points qui se
                                                                              // rajoutent aux données

            switch (type) {
                case "nom":
                    // nomPrenom
                    fiche.setNom(mentionText);
                    break;
                case "ancien":
                    // latines;
                    fiche.setLatines(mentionText);
                    break;
                case "sexe":
                    // sexe
                    fiche.setSexe(mentionText);
                    break;
                case "date":
                    // dateNaissance;
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
                    try {
                        fiche.setDateNaissance(LocalDate.parse(mentionText, formatter));
                    } catch (DateTimeParseException e) {
                        System.err.println("Erreur lors du parsing de la date : " + mentionText);
                        fiche.setDateNaissance(null); // ou gérer autrement
                    }
                    break;
                case "date-lettres":
                    // dateNaissanceLettres
                    fiche.setDateNaissanceLettres(mentionText);
                    break;
                case "lieu":
                    // lieuNaissance
                    fiche.setLieuNaissance(mentionText);
                    break;
                case "baladia":
                    // baladia;
                    fiche.setBaladia(mentionText);
                    break;
                case "wilaya":
                    // wilaya
                    fiche.setWilaya(mentionText);
                    break;
                case "mere":
                    // mere
                    fiche.setMere(mentionText);
                    break;
                case "pere":
                    // pere
                    fiche.setPere(mentionText);
                    break;
                case "numExtrait":
                    fiche.setNumeroPiece(mentionText);
                    break;
                case "marge":
                    fiche.setMarge(mentionText);
                    break;
            }
        }
        // Enregistrement dans la base de données
        // extraitNaissanceRepo.save(fiche);
        return fiche;
    }
}
