package com.muhend.backendai.service.pipeline;

import com.muhend.backendai.calculs.exception.InvalidFamilyCompositionException;
import com.muhend.backendai.entities.FridaEntity;
import com.muhend.backendai.entities.HeritierEntity;
import com.muhend.backendai.entities.IdentitesEntity;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Refuse le calcul des parts quand une même personne figure plusieurs fois dans un dossier.
 * <p>
 * Un héritier saisi deux fois fausse la répartition sans aucun signal. La comparaison porte
 * sur le NIN, normalisé par {@link NinValidationService} ; les NIN absents ou invalides sont
 * ignorés (échec de lecture, saisie incomplète). Le contrôle est local au dossier : une même
 * personne peut hériter dans plusieurs successions.
 */
@Profile("!calc-only")
@Component
public class DoublonsHeritiersValidator {

    private final NinValidationService ninValidationService;

    public DoublonsHeritiersValidator(NinValidationService ninValidationService) {
        this.ninValidationService = ninValidationService;
    }

    /**
     * @throws InvalidFamilyCompositionException si un même NIN apparaît plusieurs fois,
     *                                           entre héritiers ou entre le défunt et un héritier.
     */
    public void verifier(FridaEntity frida) {
        Map<String, List<String>> personnesParNin = new LinkedHashMap<>();

        if (frida.getDefunt() != null) {
            enregistrer(personnesParNin, frida.getDefunt().getIdentite(), "le défunt");
        }
        if (frida.getHeritiers() != null) {
            for (HeritierEntity heritier : frida.getHeritiers()) {
                enregistrer(personnesParNin, heritier.getIdentite(), libelleRole(heritier.getNumParente()));
            }
        }

        // Seuls les 4 derniers chiffres du NIN figurent dans le message
        List<String> doublons = personnesParNin.entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
                .map(e -> "NIN se terminant par " + e.getKey().substring(e.getKey().length() - 4)
                        + " : " + String.join(", ", e.getValue()))
                .toList();

        if (!doublons.isEmpty()) {
            throw new InvalidFamilyCompositionException(
                    "Une même personne figure plusieurs fois dans ce dossier. "
                            + String.join(" ; ", doublons)
                            + ". Corrigez le NIN ou retirez la personne en double avant de lancer le calcul.");
        }
    }

    private void enregistrer(Map<String, List<String>> personnesParNin, IdentitesEntity identite, String role) {
        if (identite == null) {
            return;
        }
        String nin = ninValidationService.cleanAndValidate(identite.getNin());
        if (nin == null) {
            return;
        }
        String nom = (valeur(identite.getPrenom()) + " " + valeur(identite.getNom())).trim();
        personnesParNin.computeIfAbsent(nin, k -> new ArrayList<>())
                .add(nom.isEmpty() ? role : role + " " + nom);
    }

    private static String valeur(String s) {
        return s == null ? "" : s.trim();
    }

    private static String libelleRole(String numParente) {
        if (numParente == null) {
            return "un héritier";
        }
        return switch (numParente) {
            case "02" -> "le conjoint";
            case "03" -> "l'enfant";
            case "04" -> "le parent";
            case "05" -> "le frère ou la sœur";
            case "06" -> "l'oncle paternel";
            case "07" -> "le cousin paternel";
            case "08" -> "le grand-père paternel";
            case "09" -> "le petit-fils";
            case "10" -> "la petite-fille";
            case "11" -> "la grand-mère paternelle";
            default -> "un héritier";
        };
    }
}
