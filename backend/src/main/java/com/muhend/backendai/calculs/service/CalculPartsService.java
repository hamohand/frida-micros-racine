package com.muhend.backendai.calculs.service;

import com.muhend.backendai.calculs.enums.HeirType;
import com.muhend.backendai.calculs.model.Fraction;
import com.muhend.backendai.calculs.model.Heritier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class CalculPartsService {

    public List<Heritier> calculParts(com.muhend.backendai.calculs.model.FamilyRequest request) {
        int nbPetitsFils = request.getNbPetitsFils() != null ? request.getNbPetitsFils() : 0;
        int nbPetitesFilles = request.getNbPetitesFilles() != null ? request.getNbPetitesFilles() : 0;
        String sexeParentPredecede = request.getSexeParentPredecede() != null ? request.getSexeParentPredecede() : "M";

        int nb_filles = request.getNbFilles() != null ? request.getNbFilles() : 0;
        int nb_garcons = request.getNbGarcons() != null ? request.getNbGarcons() : 0;

        // Cas Classique : Si aucun enfant direct, et que le parent décédé était un fils, 
        // les petits-enfants prennent la place des enfants directs (Mawarith classique)
        if (nb_filles == 0 && nb_garcons == 0 && "M".equalsIgnoreCase(sexeParentPredecede) && (nbPetitsFils > 0 || nbPetitesFilles > 0)) {
            request.setNbGarcons(nbPetitsFils);
            request.setNbFilles(nbPetitesFilles);
            nbPetitsFils = 0;
            nbPetitesFilles = 0;
        }

        boolean hasWasiyya = (nbPetitsFils > 0 || nbPetitesFilles > 0);

        if (!hasWasiyya) {
            return calculPartsInterne(request, new Fraction(1));
        }

        log.info("Calcul avec Testament Obligatoire (Wasiyya Wajiba)");

        // --- PHASE 1 : SIMULATION ---
        com.muhend.backendai.calculs.model.FamilyRequest simRequest = com.muhend.backendai.calculs.model.FamilyRequest.builder()
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

        if ("M".equalsIgnoreCase(sexeParentPredecede)) {
            simRequest.setNbGarcons(simRequest.getNbGarcons() + 1);
        } else {
            simRequest.setNbFilles(simRequest.getNbFilles() + 1);
        }

        List<Heritier> simResult = calculPartsInterne(simRequest, new Fraction(1));

        // Extraction de la part du parent simulé
        Fraction partParentSimule = new Fraction(0);
        if ("M".equalsIgnoreCase(sexeParentPredecede)) {
            for (Heritier h : simResult) {
                if (HeirType.SON.getLabel().equals(h.getHeritier())) {
                    partParentSimule = h.getPart(); // Part d'un seul garçon simulé
                    break;
                }
            }
        } else {
            for (Heritier h : simResult) {
                if (HeirType.DAUGHTER.getLabel().equals(h.getHeritier())) {
                    partParentSimule = h.getPart(); // Part d'une seule fille simulée
                    break;
                }
            }
        }

        // Plafonnement au tiers (1/3)
        Fraction unTiers = new Fraction(1, 3);
        Fraction wasiyya = partParentSimule;
        long val1 = (long) partParentSimule.getNumerateur() * unTiers.getDenominateur();
        long val2 = (long) unTiers.getNumerateur() * partParentSimule.getDenominateur();
        if (val1 > val2) {
            wasiyya = unTiers;
        }

        if (wasiyya.getNumerateur() == 0) {
            return calculPartsInterne(request, new Fraction(1));
        }

        // --- PHASE 2 : CALCUL REEL SUR LE RESTE ---
        Fraction restePourHeritiers = new Fraction(1).soustraire(wasiyya);
        List<Heritier> finalResult = calculPartsInterne(request, restePourHeritiers);

        // --- PHASE 3 : DISTRIBUTION AUX PETITS-ENFANTS ---
        int partsPetitsEnfants = (nbPetitsFils * 2) + nbPetitesFilles;
        Fraction partPetitFils = null;
        Fraction partPetiteFille = null;

        if (partsPetitsEnfants > 0) {
            if (nbPetitsFils > 0) {
                partPetitFils = wasiyya.multiplier(2).diviser(partsPetitsEnfants);
            }
            if (nbPetitesFilles > 0) {
                partPetiteFille = wasiyya.multiplier(1).diviser(partsPetitsEnfants);
            }
        }

        // Insertion dans la liste avant la "part restante"
        if (nbPetitsFils > 0) {
            Heritier h = new Heritier(HeirType.GRANDSON, partPetitFils);
            h.setCadreLegal("الوصية الواجبة (Testament Obligatoire)");
            h.setPartLegale(partPetitFils);
            finalResult.add(finalResult.size() - 1, h);
        }
        if (nbPetitesFilles > 0) {
            Heritier h = new Heritier(HeirType.GRANDDAUGHTER, partPetiteFille);
            h.setCadreLegal("الوصية الواجبة (Testament Obligatoire)");
            h.setPartLegale(partPetiteFille);
            finalResult.add(finalResult.size() - 1, h);
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

        return finalResult;
    }

    /**
     * Méthode interne de calcul des parts d'héritage.
     * Visibilité package pour permettre la réutilisation par CalculPartsEtenduService.
     */
    List<Heritier> calculPartsInterne(com.muhend.backendai.calculs.model.FamilyRequest request, Fraction multiplicateur) {
        String sexe_defunt = request.getSexeDefunt();
        boolean conjoint_vivant = request.getNbConjoints() != null && request.getNbConjoints() > 0;
        int nb_conjoints = request.getNbConjoints() != null ? request.getNbConjoints() : 0;
        boolean pere_vivant = request.isPereVivant();
        boolean grand_pere_vivant = request.isGrandPerePaternelVivant();
        boolean mere_vivante = request.isMereVivante();
        boolean grand_mere_vivante = request.isGrandMerePaternelleVivante();
        int nb_filles = request.getNbFilles() != null ? request.getNbFilles() : 0;
        int nb_garcons = request.getNbGarcons() != null ? request.getNbGarcons() : 0;
        int nb_soeurs = request.getNbSoeurs() != null ? request.getNbSoeurs() : 0;
        int nb_freres = request.getNbFreres() != null ? request.getNbFreres() : 0;
        int nb_oncles = request.getNbOncles() != null ? request.getNbOncles() : 0;
        int nb_cousins = request.getNbCousins() != null ? request.getNbCousins() : 0;

        // Le père exclut le grand-père et la grand-mère paternelle
        if (pere_vivant) {
            grand_pere_vivant = false;
            grand_mere_vivante = false;
        }

        // La mère exclut la grand-mère paternelle
        if (mere_vivante) {
            grand_mere_vivante = false;
        }

        int active_freres = nb_freres;
        int active_soeurs = nb_soeurs;
        if (pere_vivant || grand_pere_vivant || nb_garcons > 0) {
            active_freres = 0;
            active_soeurs = 0;
        }

        int active_oncles = nb_oncles;
        if (pere_vivant || grand_pere_vivant || nb_garcons > 0 || nb_freres > 0) {
            active_oncles = 0;
        }

        int active_cousins = nb_cousins;
        if (pere_vivant || grand_pere_vivant || nb_garcons > 0 || nb_freres > 0 || nb_oncles > 0) {
            active_cousins = 0;
        }

        Fraction f_conjoint = new Fraction(0);
        Fraction f_mere = new Fraction(0);
        Fraction f_pere = new Fraction(0);
        Fraction f_grand_pere = new Fraction(0);
        Fraction f_grand_mere = new Fraction(0);
        Fraction f_filles = new Fraction(0);
        Fraction f_soeurs = new Fraction(0);

        if (conjoint_vivant) {
            boolean has_descendants = (nb_filles > 0 || nb_garcons > 0);
            if (has_descendants) {
                f_conjoint = (sexe_defunt.equalsIgnoreCase("M") || sexe_defunt.equalsIgnoreCase("Masculin"))
                        ? new Fraction(1, 8) : new Fraction(1, 4);
            } else {
                f_conjoint = (sexe_defunt.equalsIgnoreCase("M") || sexe_defunt.equalsIgnoreCase("Masculin"))
                        ? new Fraction(1, 4) : new Fraction(1, 2);
            }
        }

        if (mere_vivante) {
            boolean has_descendants = (nb_filles > 0 || nb_garcons > 0);
            boolean has_multiple_siblings = (active_freres + active_soeurs) >= 2;
            if (has_descendants || has_multiple_siblings) {
                f_mere = new Fraction(1, 6);
            } else {
                if (conjoint_vivant && pere_vivant && (active_freres + active_soeurs) == 0) {
                    if (f_conjoint.getNumerateur() == 1 && f_conjoint.getDenominateur() == 2) {
                        f_mere = new Fraction(1, 6);
                    } else if (f_conjoint.getNumerateur() == 1 && f_conjoint.getDenominateur() == 4) {
                        f_mere = new Fraction(1, 4);
                    } else {
                        f_mere = new Fraction(1, 3);
                    }
                } else {
                    f_mere = new Fraction(1, 3);
                }
            }
        }

        if (pere_vivant) {
            if (nb_filles > 0 || nb_garcons > 0) f_pere = new Fraction(1, 6);
            else f_pere = new Fraction(0);
        } else if (grand_pere_vivant) {
            if (nb_filles > 0 || nb_garcons > 0) f_grand_pere = new Fraction(1, 6);
            else f_grand_pere = new Fraction(0);
        }

        if (grand_mere_vivante) {
            f_grand_mere = new Fraction(1, 6);
        }

        if (nb_filles > 0 && nb_garcons == 0) {
            f_filles = (nb_filles == 1) ? new Fraction(1, 2) : new Fraction(2, 3);
        }

        if (active_soeurs > 0 && active_freres == 0 && nb_filles == 0 && nb_garcons == 0) {
            f_soeurs = (active_soeurs == 1) ? new Fraction(1, 2) : new Fraction(2, 3);
        }

        Fraction final_conjoint = f_conjoint;
        Fraction final_mere = f_mere;
        Fraction final_pere = f_pere;
        Fraction final_grand_pere = f_grand_pere;
        Fraction final_grand_mere = f_grand_mere;
        Fraction final_fille = (nb_filles > 0 && nb_garcons == 0) ? f_filles.diviser(nb_filles) : null;
        Fraction final_garcon = null;
        Fraction final_soeur = (active_soeurs > 0 && active_freres == 0 && nb_filles == 0 && nb_garcons == 0) ? f_soeurs.diviser(active_soeurs) : null;
        Fraction final_frere = null;
        Fraction final_oncle = null;
        Fraction final_cousin = null;
        Fraction final_part_restant = new Fraction(0);

        List<Fraction> listFixed = new ArrayList<>();
        listFixed.add(f_conjoint);
        listFixed.add(f_mere);
        listFixed.add(f_pere);
        listFixed.add(f_grand_pere);
        listFixed.add(f_grand_mere);
        listFixed.add(f_filles);
        listFixed.add(f_soeurs);

        List<Fraction> listCommon = Fraction.reduireAuMemDenominateur(listFixed);
        int commonDen = listCommon.get(0).getDenominateur();
        int sumNum = 0;
        for (Fraction f : listCommon) sumNum += f.getNumerateur();

        String cadre_conjoint = "";
        String cadre_mere = "";
        String cadre_pere = "";
        String cadre_grand_pere = "";
        String cadre_grand_mere = "";
        String cadre_fille = "";
        String cadre_garcon = "";
        String cadre_soeur = "";
        String cadre_frere = "";
        String cadre_oncle = "";
        String cadre_cousin = "";

        if (sumNum > commonDen) {
            final_conjoint = new Fraction(listCommon.get(0).getNumerateur(), sumNum);
            final_mere = new Fraction(listCommon.get(1).getNumerateur(), sumNum);
            final_pere = new Fraction(listCommon.get(2).getNumerateur(), sumNum);
            final_grand_pere = new Fraction(listCommon.get(3).getNumerateur(), sumNum);
            final_grand_mere = new Fraction(listCommon.get(4).getNumerateur(), sumNum);
            if (nb_filles > 0) final_fille = new Fraction(listCommon.get(5).getNumerateur(), sumNum).diviser(nb_filles);
            if (active_soeurs > 0) final_soeur = new Fraction(listCommon.get(6).getNumerateur(), sumNum).diviser(active_soeurs);
            
            if (nb_garcons > 0) { final_garcon = new Fraction(0); if (nb_filles > 0) final_fille = new Fraction(0); }
            if (active_freres > 0) { final_frere = new Fraction(0); if (active_soeurs > 0) final_soeur = new Fraction(0); }
            if (active_oncles > 0) final_oncle = new Fraction(0);
            if (active_cousins > 0) final_cousin = new Fraction(0);
            
            cadre_conjoint = cadre_mere = cadre_pere = cadre_grand_pere = cadre_grand_mere = cadre_fille = cadre_soeur = "العول (Aoul - Réduction)";
            cadre_garcon = cadre_frere = cadre_oncle = cadre_cousin = "العصبة (Asaba - Résiduaire)";
        } else {
            Fraction residue = new Fraction(commonDen - sumNum, commonDen);
            boolean has_asaba = false;

            if (nb_garcons > 0) {
                has_asaba = true;
                int parts = (nb_garcons * 2) + nb_filles;
                final_garcon = residue.multiplier(2).diviser(parts);
                final_fille = residue.multiplier(1).diviser(parts);
                cadre_garcon = cadre_fille = "العصبة (Asaba - Résiduaire)";
            } else if (pere_vivant) {
                has_asaba = true;
                final_pere = final_pere.ajouter(residue);
                cadre_pere = (nb_filles > 0) ? "الفرض والعصبة (Fard et Asaba)" : "العصبة (Asaba - Résiduaire)";
            } else if (grand_pere_vivant) {
                has_asaba = true;
                final_grand_pere = final_grand_pere.ajouter(residue);
                cadre_grand_pere = (nb_filles > 0) ? "الفرض والعصبة (Fard et Asaba)" : "العصبة (Asaba - Résiduaire)";
            } else if (active_freres > 0) {
                has_asaba = true;
                int parts = (active_freres * 2) + active_soeurs;
                final_frere = residue.multiplier(2).diviser(parts);
                final_soeur = residue.multiplier(1).diviser(parts);
                cadre_frere = cadre_soeur = "العصبة (Asaba - Résiduaire)";
            } else if (nb_filles > 0 && active_soeurs > 0) {
                has_asaba = true;
                final_soeur = residue.diviser(active_soeurs);
                cadre_soeur = "العصبة (Asaba - Résiduaire)";
            } else if (active_oncles > 0) {
                has_asaba = true;
                final_oncle = residue.diviser(active_oncles);
                cadre_oncle = "العصبة (Asaba - Résiduaire)";
            } else if (active_cousins > 0) {
                has_asaba = true;
                final_cousin = residue.diviser(active_cousins);
                cadre_cousin = "العصبة (Asaba - Résiduaire)";
            }

            boolean is_radd = !has_asaba && residue.getNumerateur() > 0;

            if (is_radd) {
                Fraction sommeEligible = f_mere.ajouter(f_grand_mere).ajouter(f_filles).ajouter(f_soeurs);
                if (sommeEligible.getNumerateur() > 0) {
                    Fraction multiplierFactor = new Fraction(1).soustraire(f_conjoint).diviser(sommeEligible);
                    if (mere_vivante) final_mere = f_mere.multiplier(multiplierFactor);
                    if (grand_mere_vivante) final_grand_mere = f_grand_mere.multiplier(multiplierFactor);
                    if (nb_filles > 0) final_fille = f_filles.multiplier(multiplierFactor).diviser(nb_filles);
                    if (active_soeurs > 0) final_soeur = f_soeurs.multiplier(multiplierFactor).diviser(active_soeurs);
                } else {
                    final_part_restant = residue;
                }
            }
            
            if (conjoint_vivant) cadre_conjoint = "الفرض (Fard - Part fixe)";
            if (mere_vivante) cadre_mere = is_radd ? "الفرض والرد (Fard et Radd)" : "الفرض (Fard - Part fixe)";
            if (pere_vivant && cadre_pere.isEmpty()) cadre_pere = "الفرض (Fard - Part fixe)";
            if (grand_pere_vivant && cadre_grand_pere.isEmpty()) cadre_grand_pere = "الفرض (Fard - Part fixe)";
            if (grand_mere_vivante && cadre_grand_mere.isEmpty()) cadre_grand_mere = is_radd ? "الفرض والرد (Fard et Radd)" : "الفرض (Fard - Part fixe)";
            if (nb_filles > 0 && cadre_fille.isEmpty()) cadre_fille = is_radd ? "الفرض والرد (Fard et Radd)" : "الفرض (Fard - Part fixe)";
            if (nb_soeurs > 0 && cadre_soeur.isEmpty()) cadre_soeur = (active_soeurs == 0) ? "محجوب (Exclu)" : (is_radd ? "الفرض والرد (Fard et Radd)" : "الفرض (Fard - Part fixe)");
            if (nb_freres > 0 && cadre_frere.isEmpty()) cadre_frere = (active_freres == 0) ? "محجوب (Exclu)" : "العصبة (Asaba - Résiduaire)";
            if (nb_oncles > 0 && cadre_oncle.isEmpty()) cadre_oncle = (active_oncles == 0) ? "محجوب (Exclu)" : "العصبة (Asaba - Résiduaire)";
            if (nb_cousins > 0 && cadre_cousin.isEmpty()) cadre_cousin = (active_cousins == 0) ? "محجوب (Exclu)" : "العصبة (Asaba - Résiduaire)";
        }

        List<Heritier> heritiersList = new ArrayList<>();
        List<Fraction> fractionsList = new ArrayList<>();

        if (conjoint_vivant) {
            Heritier h = new Heritier(HeirType.SPOUSE, final_conjoint.diviser(nb_conjoints).multiplier(multiplicateur));
            h.setCadreLegal(cadre_conjoint);
            h.setPartLegale(new Fraction(final_conjoint.getNumerateur(), final_conjoint.getDenominateur()).multiplier(multiplicateur));
            heritiersList.add(h);
            fractionsList.add(h.getPart());
        }
        if (pere_vivant) {
            Heritier h = new Heritier(HeirType.FATHER, (final_pere != null ? final_pere : new Fraction(0)).multiplier(multiplicateur));
            h.setCadreLegal(cadre_pere);
            h.setPartLegale(h.getPart());
            heritiersList.add(h);
            fractionsList.add(h.getPart());
        }
        if (grand_pere_vivant) {
            Heritier h = new Heritier(HeirType.PATERNAL_GRANDFATHER, (final_grand_pere != null ? final_grand_pere : new Fraction(0)).multiplier(multiplicateur));
            h.setCadreLegal(cadre_grand_pere);
            h.setPartLegale(h.getPart());
            heritiersList.add(h);
            fractionsList.add(h.getPart());
        }
        if (grand_mere_vivante) {
            Heritier h = new Heritier(HeirType.PATERNAL_GRANDMOTHER, (final_grand_mere != null ? final_grand_mere : new Fraction(0)).multiplier(multiplicateur));
            h.setCadreLegal(cadre_grand_mere);
            h.setPartLegale(h.getPart());
            heritiersList.add(h);
            fractionsList.add(h.getPart());
        }
        if (mere_vivante) {
            Heritier h = new Heritier(HeirType.MOTHER, (final_mere != null ? final_mere : new Fraction(0)).multiplier(multiplicateur));
            h.setCadreLegal(cadre_mere);
            h.setPartLegale(h.getPart());
            heritiersList.add(h);
            fractionsList.add(h.getPart());
        }
        if (nb_filles > 0) {
            Heritier h = new Heritier(HeirType.DAUGHTER, (final_fille != null ? final_fille : new Fraction(0)).multiplier(multiplicateur));
            h.setCadreLegal(cadre_fille);
            h.setPartLegale(h.getPart());
            heritiersList.add(h);
            fractionsList.add(h.getPart());
        }
        if (nb_garcons > 0) {
            Heritier h = new Heritier(HeirType.SON, (final_garcon != null ? final_garcon : new Fraction(0)).multiplier(multiplicateur));
            h.setCadreLegal(cadre_garcon);
            h.setPartLegale(h.getPart());
            heritiersList.add(h);
            fractionsList.add(h.getPart());
        }
        if (nb_soeurs > 0) {
            Heritier h = new Heritier(HeirType.SISTER, (final_soeur != null ? final_soeur : new Fraction(0)).multiplier(multiplicateur));
            h.setCadreLegal(cadre_soeur);
            h.setPartLegale(h.getPart());
            heritiersList.add(h);
            fractionsList.add(h.getPart());
        }
        if (nb_freres > 0) {
            Heritier h = new Heritier(HeirType.BROTHER, (final_frere != null ? final_frere : new Fraction(0)).multiplier(multiplicateur));
            h.setCadreLegal(cadre_frere);
            h.setPartLegale(h.getPart());
            heritiersList.add(h);
            fractionsList.add(h.getPart());
        }
        if (nb_oncles > 0) {
            Heritier h = new Heritier(HeirType.PATERNAL_UNCLE, (final_oncle != null ? final_oncle : new Fraction(0)).multiplier(multiplicateur));
            h.setCadreLegal(cadre_oncle);
            h.setPartLegale(h.getPart());
            heritiersList.add(h);
            fractionsList.add(h.getPart());
        }
        if (nb_cousins > 0) {
            Heritier h = new Heritier(HeirType.PATERNAL_COUSIN, (final_cousin != null ? final_cousin : new Fraction(0)).multiplier(multiplicateur));
            h.setCadreLegal(cadre_cousin);
            h.setPartLegale(h.getPart());
            heritiersList.add(h);
            fractionsList.add(h.getPart());
        }

        Heritier h_rest = new Heritier(HeirType.REMAINDER, final_part_restant.multiplier(multiplicateur));
        heritiersList.add(h_rest);
        fractionsList.add(h_rest.getPart());

        List<Fraction> fractionListMemeDenominateur = Fraction.reduireAuMemDenominateur(fractionsList);
        for (int i = 0; i < heritiersList.size(); i++) {
            heritiersList.get(i).setPart(fractionListMemeDenominateur.get(i));
            if (HeirType.SPOUSE.getLabel().equals(heritiersList.get(i).getHeritier()) == false && HeirType.GRANDSON.getLabel().equals(heritiersList.get(i).getHeritier()) == false && HeirType.GRANDDAUGHTER.getLabel().equals(heritiersList.get(i).getHeritier()) == false) {
                 heritiersList.get(i).setPartLegale(fractionListMemeDenominateur.get(i));
            }
        }

        return heritiersList;
    }
}
