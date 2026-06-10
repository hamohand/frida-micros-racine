package com.muhend.backendai.service.pipeline;

import com.muhend.backendai.dto.FicheUpdateDto;
import com.muhend.backendai.dto.PersonneUpdateDto;
import com.muhend.backendai.entities.*;
import com.muhend.backendai.enums.HeirCategory;
import com.muhend.backendai.repository.*;
import com.muhend.backendai.utils.SexeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Service dédié à la persistance des entités Frida et de leurs composants.
 * <p>
 * Gère : sauvegarde des documents OCR, brouillons, corrections humaines,
 * et lancement du calcul des parts d'héritage.
 */
@Slf4j
@Service
public class FridaPersistenceService {

    private final PersonneFactory personneFactory;
    private final HeirPartCalculatorService heirPartCalculatorService;

    private final IdentitesRepo identitesRepo;
    private final FridaRepo fridaRepo;
    private final HeritierRepo heritierRepo;
    private final DefuntRepo defuntRepo;
    private final CalculRepo calculRepo;
    private final TemoinRepo temoinRepo;

    public FridaPersistenceService(
            PersonneFactory personneFactory,
            HeirPartCalculatorService heirPartCalculatorService,
            IdentitesRepo identitesRepo,
            FridaRepo fridaRepo,
            HeritierRepo heritierRepo,
            DefuntRepo defuntRepo,
            CalculRepo calculRepo,
            TemoinRepo temoinRepo) {
        this.personneFactory = personneFactory;
        this.heirPartCalculatorService = heirPartCalculatorService;
        this.identitesRepo = identitesRepo;
        this.fridaRepo = fridaRepo;
        this.heritierRepo = heritierRepo;
        this.defuntRepo = defuntRepo;
        this.calculRepo = calculRepo;
        this.temoinRepo = temoinRepo;
    }

    // ======================= Sauvegarde d'un document OCR =======================

    /**
     * Sauvegarde l'identité et crée l'entité appropriée (défunt, héritier, témoin).
     */
    public void sauvegarderDocument(TraitementContext ctx, IdentitesEntity identite,
                                    HeirCategory heirCategory, int indiceParente) {
        identite.setNumFrida(ctx.getNumFrida());
        identitesRepo.save(identite);

        // Si l'identité nécessite une correction, marquer toute la Frida comme nécessitant correction
        if (identite.getRequiresCorrection() != null && identite.getRequiresCorrection()) {
            ctx.getFicheFrida().setRequiresCorrection(true);
        }

        switch (heirCategory) {
            case DEFUNT -> {
                DefuntEntity defunt = personneFactory.creerDefunt(ctx, identite);
                defuntRepo.save(defunt);
                ctx.getFicheFrida().setNumFrida(ctx.getNumFrida());
                ctx.getFicheFrida().setDefunt(defunt);
            }
            case TEMOIN -> {
                TemoinEntity temoin = personneFactory.creerTemoin(ctx, identite, indiceParente);
                temoinRepo.save(temoin);
                ctx.getListeTemoins().add(temoin);
            }
            default -> {
                HeritierEntity heritier = personneFactory.creerHeritier(ctx, identite, indiceParente);
                heritierRepo.save(heritier);
                ctx.getListeHeritiers().add(heritier);
            }
        }
    }

    // ======================= Sauvegarde brouillon =======================

    /**
     * Persiste la fiche Frida à l'état de brouillon (sans lancer les calculs).
     */
    public void sauvegarderBrouillonFrida(TraitementContext ctx) {
        String finalNumFrida = ctx.getNumFrida();

        // Mettre à jour numFrida pour les entités traitées avant le défunt (ex: témoins '0_cni')
        for (TemoinEntity t : ctx.getListeTemoins()) {
            if ("0".equals(t.getNumFrida()) || (t.getNumFrida() != null && !t.getNumFrida().equals(finalNumFrida))) {
                t.setNumFrida(finalNumFrida);
                if (t.getIdentite() != null) {
                    t.getIdentite().setNumFrida(finalNumFrida);
                    identitesRepo.save(t.getIdentite());
                }
                temoinRepo.save(t);
            }
        }
        
        for (HeritierEntity h : ctx.getListeHeritiers()) {
            if ("0".equals(h.getNumFrida()) || (h.getNumFrida() != null && !h.getNumFrida().equals(finalNumFrida))) {
                h.setNumFrida(finalNumFrida);
                if (h.getIdentite() != null) {
                    h.getIdentite().setNumFrida(finalNumFrida);
                    identitesRepo.save(h.getIdentite());
                }
                heritierRepo.save(h);
            }
        }

        // Constituer et sauvegarder la fiche Frida sans le calcul
        FridaEntity ficheFrida = ctx.getFicheFrida();
        ficheFrida.setDateCreation(LocalDate.now());
        ficheFrida.setNotaire("محمد قثوم الموثق بالجزاىر شارع الانتصار،"); // TODO: rendre configurable
        ficheFrida.setHeritiers(ctx.getListeHeritiers());
        ficheFrida.setTemoins(ctx.getListeTemoins());
        
        // Si aucun calcul n'a encore été fait, on s'assure qu'il reste null
        // On le fera a posteriori depuis l'interface de validation
        fridaRepo.save(ficheFrida);
    }

    // ======================= Corrections humaines =======================

    /**
     * Ecrase le brouillon de l'IA avec la version corrigée par l'humain.
     */
    @Transactional
    public void sauvegarderFicheCorrigee(String numFrida, FicheUpdateDto dto) {
        FridaEntity frida = fridaRepo.findByNumFrida(numFrida)
                .orElseThrow(() -> new RuntimeException("Frida non trouvée: " + numFrida));
        
        // 1. Mettre à jour le défunt
        if (dto.getDefunt() != null) {
            DefuntEntity defunt = frida.getDefunt();
            if (defunt != null && defunt.getIdentite() != null) {
                defunt.getIdentite().setNom(dto.getDefunt().getNom());
                defunt.getIdentite().setPrenom(dto.getDefunt().getPrenom());
                defunt.getIdentite().setDateNaissance(dto.getDefunt().getDateNaissance());
                defunt.getIdentite().setSexe(dto.getDefunt().getSexe());
                identitesRepo.save(defunt.getIdentite());
            }
        }
        
        if (dto.getSexeParentPredecede() != null) {
            frida.setSexeParentPredecede(dto.getSexeParentPredecede());
        }
        
        // 2. Remplacer les héritiers
        // Nettoyage de l'ancienne liste
        if (frida.getHeritiers() != null && !frida.getHeritiers().isEmpty()) {
            heritierRepo.deleteAll(frida.getHeritiers());
            frida.getHeritiers().clear();
        }
        
        // Insertion des nouveaux
        if (dto.getHeritiers() != null) {
            for (PersonneUpdateDto p : dto.getHeritiers()) {
                IdentitesEntity identite = new IdentitesEntity();
                identite.setNumFrida(numFrida);
                identite.setNom(p.getNom());
                identite.setPrenom(p.getPrenom());
                identite.setDateNaissance(p.getDateNaissance());
                identite.setSexe(p.getSexe());
                identite.setNin(p.getNin());
                identite = identitesRepo.save(identite);
                
                HeritierEntity heritier = new HeritierEntity();
                heritier.setNumFrida(numFrida);
                heritier.setNumParente(p.getNumParente());
                heritier.setIdentite(identite);
                
                heritier = heritierRepo.save(heritier);
                frida.getHeritiers().add(heritier);
            }
        }
        
        fridaRepo.save(frida);
    }

    // ======================= Calcul des parts =======================

    /**
     * Lance le calcul des parts sur une Frida existante et la met à jour.
     * Cette méthode est appelée manuellement après validation de la Fiche.
     */
    @Transactional
    public FridaEntity lancerCalcul(String numFrida) {
        FridaEntity frida = fridaRepo.findByNumFrida(numFrida)
                .orElseThrow(() -> new RuntimeException("Frida non trouvée: " + numFrida));

        TraitementContext ctx = TraitementContext.reconstruireContexte(frida);

        // 1. Appel du moteur de calcul
        CalculEntity calcul = heirPartCalculatorService.calculerParts(ctx);
        calculRepo.save(calcul);

        // 2. Attribution des coefficients
        for (HeritierEntity heritier : frida.getHeritiers()) {
            int numerateur = determinerNumerateur(heritier, calcul);
            float coef = heirPartCalculatorService.calculerCoefficient(
                    numerateur, calcul.getDenominateur());
            heritier.setCoefPart(coef);
        }

        // 3. Sauvegarde
        frida.setCalcul(calcul);
        frida.setStatut(FridaEntity.STATUT_VALIDE);
        return fridaRepo.save(frida);
    }

    /**
     * Détermine le numérateur de la part d'un héritier selon sa catégorie.
     */
    private int determinerNumerateur(HeritierEntity heritier, CalculEntity calcul) {
        return switch (heritier.getNumParente()) {
            case "02" -> calcul.getNumerateurConjoint();
            case "03" -> SexeUtils.isMasculin(heritier.getIdentite().getSexe())
                    ? calcul.getNumerateurGarcons()
                    : calcul.getNumerateurFilles();
            case "04" -> SexeUtils.isMasculin(heritier.getIdentite().getSexe())
                    ? (calcul.getNumerateurPere() != null ? calcul.getNumerateurPere() : 0)
                    : (calcul.getNumerateurMere() != null ? calcul.getNumerateurMere() : 0);
            case "05" -> SexeUtils.isMasculin(heritier.getIdentite().getSexe())
                    ? (calcul.getNumerateurFreres() != null ? calcul.getNumerateurFreres() : 0)
                    : (calcul.getNumerateurSoeurs() != null ? calcul.getNumerateurSoeurs() : 0);
            case "06" -> (calcul.getNumerateurOnclesPaternels() != null ? calcul.getNumerateurOnclesPaternels() : 0);
            case "07" -> (calcul.getNumerateurCousinsPaternels() != null ? calcul.getNumerateurCousinsPaternels() : 0);
            case "08" -> (calcul.getNumerateurGrandPerePaternel() != null ? calcul.getNumerateurGrandPerePaternel() : 0);
            case "09" -> (calcul.getNumerateurPetitsFils() != null ? calcul.getNumerateurPetitsFils() : 0);
            case "10" -> (calcul.getNumerateurPetitesFilles() != null ? calcul.getNumerateurPetitesFilles() : 0);
            case "11" -> (calcul.getNumerateurGrandMerePaternelle() != null ? calcul.getNumerateurGrandMerePaternelle() : 0);
            default -> 0;
        };
    }
}
