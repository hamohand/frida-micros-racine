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
    private int nbFreres;
    private int nbSoeurs;

    public void incrementConjoints() { nbConjoints++; }
    public void incrementFilles() { nbFilles++; }
    public void incrementGarcons() { nbGarcons++; }
    public void incrementParents() { nbParents++; }
    public void incrementFreres() { nbFreres++; }
    public void incrementSoeurs() { nbSoeurs++; }
}
