package com.muhend.backendai.calculs.service;

import com.muhend.backendai.calculs.enums.HeirType;
import com.muhend.backendai.calculs.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service de calcul étendu multi-tombes.
 * Gère plusieurs héritiers pré-décédés (tombes), chacun avec ses propres descendants.
 * 
 * Algorithme en 4 phases :
 * 1. SIMULATION   — Pour chaque tombe, simuler l'héritier pré-décédé comme vivant
 * 2. PLAFONNEMENT — Sommer les wasiyya, plafonner à 1/3 globalement si nécessaire
 * 3. CALCUL RÉEL  — Calculer les parts des héritiers vivants sur le reste
 * 4. DISTRIBUTION — Distribuer la wasiyya de chaque tombe à ses descendants
 */
@Service
@Slf4j
public class CalculPartsEtenduService {

    private final CalculPartsService calculPartsService;

    @Autowired
    public CalculPartsEtenduService(CalculPartsService calculPartsService) {
        this.calculPartsService = calculPartsService;
    }

    /**
     * Point d'entrée principal du calcul multi-tombes.
     */
    public CalculEtenduResult calculPartsEtendu(ExtendedFamilyRequest request) {
        List<Tombe> tombes = request.getTombes();
        
        if (tombes == null) {
            tombes = new ArrayList<>();
        }

        // Filtrer les tombes sans descendants
        List<Tombe> tombesActives = new ArrayList<>();
        for (int i = 0; i < tombes.size(); i++) {
            Tombe t = tombes.get(i);
            if (t.hasDescendants()) {
                if (t.getIdentifiant() == null || t.getIdentifiant().isBlank()) {
                    t.setIdentifiant("tombe_" + (i + 1));
                }
                tombesActives.add(t);
            }
        }

        // Si aucune tombe active, calcul simple
        if (tombesActives.isEmpty()) {
            log.info("Aucune tombe active, calcul simple");
            List<Heritier> heritiers = calculPartsService.calculParts(request);
            return new CalculEtenduResult(heritiers, null, 0);
        }

        log.info("Calcul étendu avec {} tombe(s)", tombesActives.size());

        // --- PHASE 1 : SIMULATION ---
        List<Fraction> partsSimulees = new ArrayList<>();
        for (Tombe tombe : tombesActives) {
            Fraction partSimulee = simulerTombe(request, tombe);
            partsSimulees.add(partSimulee);
            log.info("Tombe {} ({}): part simulée = {}/{}", 
                tombe.getIdentifiant(), tombe.getLienParente(),
                partSimulee.getNumerateur(), partSimulee.getDenominateur());
        }

        // --- PHASE 2 : PLAFONNEMENT GLOBAL À 1/3 ---
        Fraction unTiers = new Fraction(1, 3);
        Fraction sommeWasiyya = new Fraction(0);
        for (Fraction part : partsSimulees) {
            sommeWasiyya = sommeWasiyya.ajouter(part);
        }

        boolean hasLivingChildren = (request.getNbFilles() != null && request.getNbFilles() > 0) ||
                                    (request.getNbGarcons() != null && request.getNbGarcons() > 0);

        if (!hasLivingChildren) {
            log.info("Aucun enfant vivant : Représentation pure (les petits-enfants remplacent leurs parents)");
            return calculerRepresentationPure(request, tombesActives);
        }

        boolean plafonnee = estSuperieur(sommeWasiyya, unTiers);
        List<Fraction> wasiyyaEffectives = new ArrayList<>();

        if (plafonnee) {
            log.info("Plafonnement appliqué : somme wasiyya ({}/{}) > 1/3", 
                sommeWasiyya.getNumerateur(), sommeWasiyya.getDenominateur());
            // Réduction proportionnelle : wasiyya[i] = partSimulée[i] × (1/3) / somme
            for (Fraction partSim : partsSimulees) {
                Fraction wasiyya = partSim.multiplier(unTiers.getNumerateur() * sommeWasiyya.getDenominateur())
                    .diviser(unTiers.getDenominateur() * sommeWasiyya.getNumerateur());
                wasiyyaEffectives.add(wasiyya);
            }
        } else {
            wasiyyaEffectives.addAll(partsSimulees);
        }

        // Somme wasiyya effective finale
        Fraction wasiyyaTotale = new Fraction(0);
        for (Fraction w : wasiyyaEffectives) {
            wasiyyaTotale = wasiyyaTotale.ajouter(w);
        }

        // --- PHASE 3 : CALCUL RÉEL SUR LE RESTE ---
        Fraction restePourHeritiers = new Fraction(1).soustraire(wasiyyaTotale);
        log.info("Reste pour héritiers : {}/{}", restePourHeritiers.getNumerateur(), restePourHeritiers.getDenominateur());
        
        List<Heritier> finalResult = calculPartsService.calculPartsInterne(request, restePourHeritiers);

        // --- PHASE 4 : DISTRIBUTION AUX DESCENDANTS DE CHAQUE TOMBE ---
        List<TombeDetail> detailTombes = new ArrayList<>();

        for (int i = 0; i < tombesActives.size(); i++) {
            Tombe tombe = tombesActives.get(i);
            Fraction wasiyya = wasiyyaEffectives.get(i);
            Fraction partSim = partsSimulees.get(i);

            List<Heritier> beneficiaires = distribuerWasiyya(tombe, wasiyya);

            // Insérer les bénéficiaires avant la "part restante" dans le résultat final
            for (Heritier benef : beneficiaires) {
                finalResult.add(finalResult.size() - 1, benef);
            }

            detailTombes.add(TombeDetail.builder()
                .identifiant(tombe.getIdentifiant())
                .sexeParentPredecede(tombe.getSexeParentPredecede())
                .lienParente(tombe.getLienParente())
                .partSimulee(partSim)
                .wasiyyaEffective(wasiyya)
                .plafonnee(plafonnee)
                .beneficiaires(beneficiaires)
                .build());
        }

        // Réduction au même dénominateur final
        List<Fraction> allFractions = new ArrayList<>();
        for (Heritier h : finalResult) {
            allFractions.add(h.getPart());
        }
        List<Fraction> allFractionsMemeDen = Fraction.reduireAuMemDenominateur(allFractions);
        for (int i = 0; i < finalResult.size(); i++) {
            finalResult.get(i).setPart(allFractionsMemeDen.get(i));
        }

        return new CalculEtenduResult(finalResult, detailTombes, tombesActives.size());
    }

    /**
     * Calcule la répartition lorsque tous les enfants sont décédés.
     * Les petits-enfants prennent exactement la place de leurs parents (Représentation pure).
     */
    private CalculEtenduResult calculerRepresentationPure(FamilyRequest request, List<Tombe> tombesActives) {
        // 1. Créer une requête simulant que tous les parents décédés (Tombes) sont vivants
        FamilyRequest simRequest = FamilyRequest.builder()
                .sexeDefunt(request.getSexeDefunt())
                .nbConjoints(request.getNbConjoints())
                .pereVivant(request.isPereVivant())
                .mereVivante(request.isMereVivante())
                .grandPerePaternelVivant(request.isGrandPerePaternelVivant())
                .grandMerePaternelleVivante(request.isGrandMerePaternelleVivante())
                .nbSoeurs(request.getNbSoeurs() != null ? request.getNbSoeurs() : 0)
                .nbFreres(request.getNbFreres() != null ? request.getNbFreres() : 0)
                .nbOncles(request.getNbOncles() != null ? request.getNbOncles() : 0)
                .nbCousins(request.getNbCousins() != null ? request.getNbCousins() : 0)
                .build();

        int simGarcons = 0;
        int simFilles = 0;
        for (Tombe t : tombesActives) {
            if ("M".equalsIgnoreCase(t.getSexeParentPredecede())) {
                simGarcons++;
            } else {
                simFilles++;
            }
        }
        simRequest.setNbGarcons(simGarcons);
        simRequest.setNbFilles(simFilles);

        // 2. Calculer les parts avec ces enfants simulés
        List<Heritier> simResult = calculPartsService.calculPartsInterne(simRequest, new Fraction(1));

        // 3. Récupérer la part d'un seul Fils simulé et d'une seule Fille simulée
        Fraction partOneSon = new Fraction(0);
        Fraction partOneDaughter = new Fraction(0);
        for (Heritier h : simResult) {
            if ("garçon".equalsIgnoreCase(h.getHeritier())) {
                partOneSon = h.getPart(); // La part retournée est déjà celle d'un seul enfant
            }
            if ("fille".equalsIgnoreCase(h.getHeritier())) {
                partOneDaughter = h.getPart();
            }
        }

        // 4. Construire le résultat final
        List<Heritier> finalResult = new ArrayList<>();
        // Conserver tous les héritiers classiques (conjoint, parents...) sauf les enfants simulés et la part restante
        for (Heritier h : simResult) {
            if (!"garçon".equalsIgnoreCase(h.getHeritier()) &&
                !"fille".equalsIgnoreCase(h.getHeritier()) &&
                !"part restant".equalsIgnoreCase(h.getHeritier())) {
                finalResult.add(h);
            }
        }

        // 5. Distribuer la part de chaque parent simulé à ses propres bénéficiaires (la Tombe)
        List<TombeDetail> detailTombes = new ArrayList<>();
        for (Tombe t : tombesActives) {
            Fraction parentShare = "M".equalsIgnoreCase(t.getSexeParentPredecede()) ? partOneSon : partOneDaughter;
            
            List<Heritier> beneficiaires = distribuerWasiyya(t, parentShare);
            finalResult.addAll(beneficiaires);

            detailTombes.add(TombeDetail.builder()
                .identifiant(t.getIdentifiant())
                .sexeParentPredecede(t.getSexeParentPredecede())
                .lienParente(t.getLienParente())
                .partSimulee(parentShare)
                .wasiyyaEffective(parentShare)
                .plafonnee(false)
                .beneficiaires(beneficiaires)
                .build());
        }

        // Réinsérer la part restante à la fin si elle existe
        for (Heritier h : simResult) {
            if ("part restant".equalsIgnoreCase(h.getHeritier())) {
                finalResult.add(h);
            }
        }

        // Réduction au même dénominateur final
        List<Fraction> allFractions = new ArrayList<>();
        for (Heritier h : finalResult) {
            allFractions.add(h.getPart());
        }
        List<Fraction> allFractionsMemeDen = Fraction.reduireAuMemDenominateur(allFractions);
        for (int i = 0; i < finalResult.size(); i++) {
            finalResult.get(i).setPart(allFractionsMemeDen.get(i));
        }

        return new CalculEtenduResult(finalResult, detailTombes, tombesActives.size());
    }

    /**
     * Simule un héritier pré-décédé comme s'il était vivant et retourne sa part théorique.
     */
    private Fraction simulerTombe(ExtendedFamilyRequest request, Tombe tombe) {
        // Créer une copie de la requête pour la simulation
        FamilyRequest simRequest = FamilyRequest.builder()
                .sexeDefunt(request.getSexeDefunt())
                .nbConjoints(request.getNbConjoints())
                .pereVivant(request.isPereVivant())
                .mereVivante(request.isMereVivante())
                .grandPerePaternelVivant(request.isGrandPerePaternelVivant())
                .grandMerePaternelleVivante(request.isGrandMerePaternelleVivante())
                .nbFilles(request.getNbFilles() != null ? request.getNbFilles() : 0)
                .nbGarcons(request.getNbGarcons() != null ? request.getNbGarcons() : 0)
                .nbSoeurs(request.getNbSoeurs() != null ? request.getNbSoeurs() : 0)
                .nbFreres(request.getNbFreres() != null ? request.getNbFreres() : 0)
                .nbOncles(request.getNbOncles() != null ? request.getNbOncles() : 0)
                .nbCousins(request.getNbCousins() != null ? request.getNbCousins() : 0)
                .build();

        // Ajouter le fantôme selon le lien de parenté
        String lien = tombe.getLienParente();
        String sexe = tombe.getSexeParentPredecede();

        if ("enfant".equalsIgnoreCase(lien)) {
            if ("M".equalsIgnoreCase(sexe)) {
                simRequest.setNbGarcons(simRequest.getNbGarcons() + 1);
            } else {
                simRequest.setNbFilles(simRequest.getNbFilles() + 1);
            }
        } else if ("frere_soeur".equalsIgnoreCase(lien)) {
            if ("M".equalsIgnoreCase(sexe)) {
                simRequest.setNbFreres(simRequest.getNbFreres() + 1);
            } else {
                simRequest.setNbSoeurs(simRequest.getNbSoeurs() + 1);
            }
        }

        // Lancer la simulation
        List<Heritier> simResult = calculPartsService.calculPartsInterne(simRequest, new Fraction(1));

        // Extraire la part du fantôme
        String labelRecherche = getLabelFantome(lien, sexe);
        for (Heritier h : simResult) {
            if (labelRecherche.equals(h.getHeritier())) {
                return h.getPart();
            }
        }

        // Si pas trouvé (ex: exclu par hajb), retourner 0
        log.warn("Part simulée non trouvée pour tombe {} (label={})", tombe.getIdentifiant(), labelRecherche);
        return new Fraction(0);
    }

    /**
     * Distribue la wasiyya d'une tombe entre ses descendants avec le ratio 2:1 (garçon:fille).
     */
    private List<Heritier> distribuerWasiyya(Tombe tombe, Fraction wasiyya) {
        List<Heritier> beneficiaires = new ArrayList<>();

        int nbMales = tombe.getNbDescendantsMales() != null ? tombe.getNbDescendantsMales() : 0;
        int nbFemelles = tombe.getNbDescendantesFemelles() != null ? tombe.getNbDescendantesFemelles() : 0;
        int totalParts = (nbMales * 2) + nbFemelles;

        if (totalParts == 0) return beneficiaires;

        boolean isEnfant = "enfant".equalsIgnoreCase(tombe.getLienParente());
        String cadreLegal = "الوصية الواجبة - " + tombe.getIdentifiant();

        if (nbMales > 0) {
            Fraction partMale = wasiyya.multiplier(2).diviser(totalParts);
            HeirType type = isEnfant ? HeirType.GRANDSON : HeirType.NEPHEW;
            Heritier h = new Heritier(type, partMale);
            h.setCadreLegal(cadreLegal);
            h.setPartLegale(partMale);
            h.setHeritier(type.getLabel() + " (" + tombe.getIdentifiant() + ")");
            beneficiaires.add(h);
        }

        if (nbFemelles > 0) {
            Fraction partFemelle = wasiyya.multiplier(1).diviser(totalParts);
            HeirType type = isEnfant ? HeirType.GRANDDAUGHTER : HeirType.NIECE;
            Heritier h = new Heritier(type, partFemelle);
            h.setCadreLegal(cadreLegal);
            h.setPartLegale(partFemelle);
            h.setHeritier(type.getLabel() + " (" + tombe.getIdentifiant() + ")");
            beneficiaires.add(h);
        }

        return beneficiaires;
    }

    /**
     * Retourne le label HeirType à chercher dans le résultat de simulation.
     */
    private String getLabelFantome(String lienParente, String sexe) {
        if ("enfant".equalsIgnoreCase(lienParente)) {
            return "M".equalsIgnoreCase(sexe) ? HeirType.SON.getLabel() : HeirType.DAUGHTER.getLabel();
        } else if ("frere_soeur".equalsIgnoreCase(lienParente)) {
            return "M".equalsIgnoreCase(sexe) ? HeirType.BROTHER.getLabel() : HeirType.SISTER.getLabel();
        }
        return "";
    }

    /**
     * Compare deux fractions : retourne true si a > b.
     */
    private boolean estSuperieur(Fraction a, Fraction b) {
        long val1 = (long) a.getNumerateur() * b.getDenominateur();
        long val2 = (long) b.getNumerateur() * a.getDenominateur();
        return val1 > val2;
    }

    /**
     * Résultat interne du calcul étendu.
     */
    public static class CalculEtenduResult {
        private final List<Heritier> heritiers;
        private final List<TombeDetail> detailTombes;
        private final int nombreTombes;

        public CalculEtenduResult(List<Heritier> heritiers, List<TombeDetail> detailTombes, int nombreTombes) {
            this.heritiers = heritiers;
            this.detailTombes = detailTombes;
            this.nombreTombes = nombreTombes;
        }

        public List<Heritier> getHeritiers() { return heritiers; }
        public List<TombeDetail> getDetailTombes() { return detailTombes; }
        public int getNombreTombes() { return nombreTombes; }
    }
}
