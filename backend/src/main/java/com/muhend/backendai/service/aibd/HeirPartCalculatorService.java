package com.muhend.backendai.service.aibd;

import com.muhend.backendai.client.calculs.CalculsApiClient;
import com.muhend.backendai.client.calculs.dto.CalculRequestDto;
import com.muhend.backendai.client.calculs.dto.CalculResponseDto;
import com.muhend.backendai.entities.CalculEntity;
import com.muhend.backendai.entities.FridaEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service responsable du calcul des parts d'héritage
 * via le microservice calculs-api.
 */
@Slf4j
@Service
public class HeirPartCalculatorService {

    private final CalculsApiClient calculsApiClient;

    public HeirPartCalculatorService(CalculsApiClient calculsApiClient) {
        this.calculsApiClient = calculsApiClient;
    }

    /**
     * Calcule les parts des héritiers en appelant le microservice calculs-api à partir d'un contexte.
     *
     * @param ctx Contexte du traitement contenant les compteurs d'héritiers.
     * @return L'entité CalculEntity peuplée avec les résultats.
     * @throws RuntimeException si l'appel au microservice échoue.
     */
    public CalculEntity calculerParts(TraitementContext ctx) {
        return executerCalcul(ctx);
    }

    /**
     * Re-calcule les parts (ex: après une correction manuelle du Sexe) depuis l'entité.
     *
     * @param frida L'entité avec ses listes d'héritiers complètes.
     * @return CalculEntity mise à jour.
     */
    public CalculEntity recalculerParts(FridaEntity frida) {
        TraitementContext ctx = TraitementContext.reconstruireContexte(frida);
        return executerCalcul(ctx);
    }

    private CalculEntity executerCalcul(TraitementContext ctx) {
        FridaEntity ficheFrida = ctx.getFicheFrida();

        // Mapping du sexe du défunt
        String sexeArabe = ficheFrida.getDefunt().getIdentite().getSexe();
        String sexe = "ذكر".equals(sexeArabe) ? "M" : "F";

        CalculRequestDto request = CalculRequestDto.builder()
                .sexeDefunt(sexe)
                .nbConjoints(ctx.getNbConjoints())
                .nbFilles(ctx.getNbFilles())
                .nbGarcons(ctx.getNbGarcons())
                .pereVivant(ctx.isPereVivant())
                .mereVivante(ctx.isMereVivante())
                .nbSoeurs(ctx.getNbSoeurs())
                .nbFreres(ctx.getNbFreres())
                .nbOncles(ctx.getNbOnclesPaternels())
                .nbCousins(ctx.getNbCousinsPaternels())
                .build();

        CalculEntity calcul = new CalculEntity();
        calcul.setNumFrida(ctx.getNumFrida());
        calcul.setNbConjoints(ctx.getNbConjoints());
        calcul.setNbFilles(ctx.getNbFilles());
        calcul.setNbGarcons(ctx.getNbGarcons());
        calcul.setNbOnclesPaternels(ctx.getNbOnclesPaternels());
        calcul.setNbCousinsPaternels(ctx.getNbCousinsPaternels());

        try {
            CalculResponseDto response = calculsApiClient.calculerParts(request);
            calcul.setDenominateur(response.getDenominateurCommun());

            // Numérateur conjoint
            response.getHeritiers().stream()
                    .filter(h -> {
                        String label = h.getHeritier().toLowerCase();
                        return label.contains("conjoint")
                                || label.contains("épouse")
                                || label.contains("époux");
                    })
                    .findFirst()
                    .ifPresent(h -> calcul.setNumerateurConjoint(h.getPart().getNumerateur()));

            // Numérateur filles
            response.getHeritiers().stream()
                    .filter(h -> h.getHeritier().toLowerCase().contains("fille"))
                    .findFirst()
                    .ifPresent(h -> calcul.setNumerateurFilles(h.getPart().getNumerateur()));

            // Numérateur garçons
            response.getHeritiers().stream()
                    .filter(h -> {
                        String label = h.getHeritier().toLowerCase();
                        return label.contains("garçon") || label.contains("fils");
                    })
                    .findFirst()
                    .ifPresent(h -> calcul.setNumerateurGarcons(h.getPart().getNumerateur()));

            // Numérateur père
            response.getHeritiers().stream()
                    .filter(h -> {
                        String label = h.getHeritier().toLowerCase();
                        return label.contains("père") || label.contains("pere");
                    })
                    .findFirst()
                    .ifPresent(h -> calcul.setNumerateurPere(h.getPart().getNumerateur()));

            // Numérateur mère
            response.getHeritiers().stream()
                    .filter(h -> {
                        String label = h.getHeritier().toLowerCase();
                        return label.contains("mère") || label.contains("mere");
                    })
                    .findFirst()
                    .ifPresent(h -> calcul.setNumerateurMere(h.getPart().getNumerateur()));

            // Numérateur frères
            response.getHeritiers().stream()
                    .filter(h -> {
                        String label = h.getHeritier().toLowerCase();
                        return label.contains("frère") || label.contains("frere");
                    })
                    .findFirst()
                    .ifPresent(h -> calcul.setNumerateurFreres(h.getPart().getNumerateur()));

            // Numérateur soeurs
            response.getHeritiers().stream()
                    .filter(h -> {
                        String label = h.getHeritier().toLowerCase();
                        return label.contains("sœur") || label.contains("soeur");
                    })
                    .findFirst()
                    .ifPresent(h -> calcul.setNumerateurSoeurs(h.getPart().getNumerateur()));

            // Numérateur oncles paternels
            response.getHeritiers().stream()
                    .filter(h -> {
                        String label = h.getHeritier().toLowerCase();
                        return label.contains("oncle");
                    })
                    .findFirst()
                    .ifPresent(h -> calcul.setNumerateurOnclesPaternels(h.getPart().getNumerateur()));

            // Numérateur cousins paternels
            response.getHeritiers().stream()
                    .filter(h -> {
                        String label = h.getHeritier().toLowerCase();
                        return label.contains("cousin");
                    })
                    .findFirst()
                    .ifPresent(h -> calcul.setNumerateurCousinsPaternels(h.getPart().getNumerateur()));

        } catch (Exception e) {
            log.error("Erreur appel microservice calculs : {}", e.getMessage(), e);
            throw new RuntimeException("Erreur microservice calculs", e);
        }

        return calcul;
    }

    /**
     * Calcule le coefficient de part d'un héritier.
     *
     * @param numerateur   Numérateur de la part.
     * @param denominateur Dénominateur commun.
     * @return Le coefficient arrondi à 2 décimales.
     */
    public float calculerCoefficient(int numerateur, int denominateur) {
        if (denominateur == 0) return 0f;
        return Math.round((float) numerateur / denominateur * 100) / 100.0f;
    }
}
