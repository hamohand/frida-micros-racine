package com.muhend.backendai.service.pipeline;

import com.muhend.backendai.entities.DefuntEntity;
import com.muhend.backendai.entities.HeritierEntity;
import com.muhend.backendai.entities.IdentitesEntity;
import com.muhend.backendai.entities.TemoinEntity;
import com.muhend.backendai.utils.SexeUtils;
import org.springframework.stereotype.Service;

/**
 * Factory pour créer les entités Personne (Défunt, Héritier, Témoin)
 * à partir des données OCR et du contexte de traitement.
 * <p>
 * Responsabilité unique : construire les objets, sans les persister.
 */
@Service
public class PersonneFactory {

    /**
     * Crée un défunt à partir du contexte et de l'identité OCR.
     */
    public DefuntEntity creerDefunt(TraitementContext ctx, IdentitesEntity identite) {
        DefuntEntity defunt = new DefuntEntity();
        defunt.setNumFrida(ctx.getNumFrida());
        defunt.setIdentite(identite);
        return defunt;
    }

    /**
     * Crée un héritier et met à jour les compteurs du contexte
     * selon la catégorie de parenté et le sexe.
     */
    public HeritierEntity creerHeritier(TraitementContext ctx, IdentitesEntity identite,
                                        int indiceParente) {
        HeritierEntity heritier = new HeritierEntity();
        heritier.setNumFrida(ctx.getNumFrida());
        heritier.setNumParente(ctx.getTableauNumParente().get(indiceParente));
        heritier.setIdentite(identite);

        // Comptage par catégorie
        String numParente = heritier.getNumParente();
        String sexe = identite.getSexe();

        switch (numParente) {
            case "02" -> ctx.incrementConjoints();
            case "03" -> {
                if (SexeUtils.isMasculin(sexe)) {
                    ctx.incrementGarcons();
                } else {
                    ctx.incrementFilles();
                }
            }
            case "04" -> ctx.incrementParents(sexe);
            case "05" -> {
                if (SexeUtils.isMasculin(sexe)) {
                    ctx.incrementFreres();
                } else {
                    ctx.incrementSoeurs();
                }
            }
            case "06" -> ctx.incrementOnclesPaternels();
            case "07" -> ctx.incrementCousinsPaternels();
            case "08" -> ctx.setGrandPerePaternelVivant(true);
            case "09" -> ctx.incrementPetitsFils();
            case "10" -> ctx.incrementPetitesFilles();
        }

        return heritier;
    }

    /**
     * Crée un témoin à partir du contexte et de l'identité OCR.
     */
    public TemoinEntity creerTemoin(TraitementContext ctx, IdentitesEntity identite,
                                    int indiceParente) {
        TemoinEntity temoin = new TemoinEntity();
        temoin.setNumFrida(ctx.getNumFrida());
        temoin.setIdentite(identite);
        temoin.setNumParente(ctx.getTableauNumParente().get(indiceParente));
        return temoin;
    }
}
