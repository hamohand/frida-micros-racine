package com.muhend.backendai.service.aibd;

import com.muhend.backendai.entities.FridaEntity;
import com.muhend.backendai.entities.HeritierEntity;
import com.muhend.backendai.entities.TemoinEntity;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Objet de contexte contenant l'état mutable d'un traitement de dossier.
 * Créé pour chaque appel à {@code traiterExtraitsNaissance},
 * ce qui garantit le thread-safety du service orchestrateur.
 */
@Getter
@Setter
public class TraitementContext {

    private String numFrida = "0";
    private FridaEntity ficheFrida = new FridaEntity();
    private List<String> tableauNumParente = new ArrayList<>();
    private List<HeritierEntity> listeHeritiers = new ArrayList<>();
    private List<TemoinEntity> listeTemoins = new ArrayList<>();

    private int nbConjoints;
    private int nbFilles;
    private int nbGarcons;
    private int nbParents;
    private boolean pereVivant;
    private boolean mereVivante;
    private int nbFreres;
    private int nbSoeurs;
    private int nbOnclesPaternels;
    private int nbCousinsPaternels;

    public void incrementConjoints() { nbConjoints++; }
    public void incrementFilles() { nbFilles++; }
    public void incrementGarcons() { nbGarcons++; }
    public void incrementParents(String sexe) { 
        nbParents++; 
        if ("ذكر".equals(sexe)) {
            this.pereVivant = true;
        } else {
            this.mereVivante = true;
        }
    }
    public void incrementFreres() { nbFreres++; }
    public void incrementSoeurs() { nbSoeurs++; }
    public void incrementOnclesPaternels() { nbOnclesPaternels++; }
    public void incrementCousinsPaternels() { nbCousinsPaternels++; }

    public static TraitementContext reconstruireContexte(FridaEntity frida) {
        TraitementContext ctx = new TraitementContext();
        ctx.setFicheFrida(frida);
        ctx.setNumFrida(frida.getNumFrida());
        
        if (frida.getHeritiers() != null) {
            for (HeritierEntity heritier : frida.getHeritiers()) {
                String numParente = heritier.getNumParente();
                String sexe = heritier.getIdentite().getSexe();
                if (numParente == null) continue;
                switch (numParente) {
                    case "2" -> ctx.incrementConjoints();
                    case "3" -> {
                        if ("ذكر".equals(sexe)) ctx.incrementGarcons();
                        else ctx.incrementFilles();
                    }
                    case "4" -> ctx.incrementParents(sexe);
                    case "5" -> {
                        if ("ذكر".equals(sexe)) ctx.incrementFreres();
                        else ctx.incrementSoeurs();
                    }
                    case "6" -> ctx.incrementOnclesPaternels();
                    case "7" -> ctx.incrementCousinsPaternels();
                }
            }
        }
        return ctx;
    }
}
