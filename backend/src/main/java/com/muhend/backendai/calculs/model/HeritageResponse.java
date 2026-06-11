package com.muhend.backendai.calculs.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Réponse contenant les parts d'héritage calculées selon la loi islamique")
public class HeritageResponse {

        @Schema(description = "Identifiant unique du calcul", example = "550e8400-e29b-41d4-a716-446655440000")
        private String calculId;

        @Schema(description = "Timestamp de la réponse", example = "2025-12-21T10:30:00")
        private LocalDateTime timestamp;

        @Schema(description = "Liste des héritiers avec leurs parts")
        private List<Heritier> heritiers;

        @Schema(description = "Nombre total d'héritiers", example = "4")
        private Integer nombreHeritiers;

        @Schema(description = "Dénominateur commun utilisé pour toutes les parts", example = "24")
        private Integer denominateurCommun;

        @Schema(description = "Part restante non distribuée")
        private Fraction partRestante;

        @Schema(description = "Message de statut", example = "Calcul des parts d'héritage réussi")
        private String message;

        @Schema(description = "Indique si le calcul est complet (aucune part restante)", example = "true")
        private Boolean calculComplet;

        @Schema(description = "Résumé de la composition familiale")
        private CompositionFamiliale composition;

        @Schema(description = "Détail par tombe (uniquement en mode étendu multi-tombes)")
        private List<TombeDetail> detailTombes;

        @Schema(description = "Nombre de tombes (héritiers pré-décédés référencés)", example = "2")
        private Integer nombreTombes;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @Schema(description = "Résumé de la composition de la famille")
        public static class CompositionFamiliale {
                @Schema(description = "Sexe du défunt", example = "M")
                private String sexeDefunt;

                @Schema(description = "Nombre de conjoints vivants", example = "1")
                private int nbConjoints;

                @Schema(description = "Présence du père", example = "false")
                private boolean pereVivant;

                @Schema(description = "Présence de la mère", example = "false")
                private boolean mereVivante;

                @Schema(description = "Présence du grand-père paternel", example = "false")
                private boolean grandPerePaternelVivant;
                @Schema(description = "Présence de la grand-mère paternelle", example = "false")
                private boolean grandMerePaternelleVivante;

                @Schema(description = "Nombre de filles", example = "1")
                private int nbFilles;

                @Schema(description = "Nombre de garçons", example = "1")
                private int nbGarcons;

                @Schema(description = "Nombre de soeurs", example = "0")
                private int nbSoeurs;

                @Schema(description = "Nombre de frères", example = "0")
                private int nbFreres;

                @Schema(description = "Nombre d'oncles", example = "0")
                private int nbOncles;

                @Schema(description = "Nombre de cousins", example = "0")
                private int nbCousins;
        }

        /**
         * Méthode utilitaire pour créer une réponse à partir de la requête et des
         * héritiers calculés
         */
        public static HeritageResponse fromCalculation(
                        FamilyRequest request,
                        List<Heritier> heritiers,
                        String message) {

                // Déterminer le dénominateur commun
                Integer denominateur = heritiers.isEmpty() ? null
                                : heritiers.get(0).getPart() != null ? heritiers.get(0).getPart().getDenominateur()
                                                : null;

                // Trouver la part restante
                Fraction partRestante = heritiers.stream()
                                .filter(h -> "part restant".equals(h.getHeritier()))
                                .map(Heritier::getPart)
                                .findFirst()
                                .orElse(new Fraction(0));

                // Calculer le nombre d'héritiers (sans la part restante)
                int nbHeritiers = (int) heritiers.stream()
                                .filter(h -> !"part restant".equals(h.getHeritier()))
                                .filter(h -> h.getPart() != null && h.getPart().getNumerateur() > 0)
                                .count();

                // Vérifier si le calcul est complet
                boolean calculComplet = partRestante.getNumerateur() == 0;

                return HeritageResponse.builder()
                                .calculId(UUID.randomUUID().toString())
                                .timestamp(LocalDateTime.now())
                                .heritiers(heritiers)
                                .nombreHeritiers(nbHeritiers)
                                .denominateurCommun(denominateur)
                                .partRestante(partRestante)
                                .message(message)
                                .calculComplet(calculComplet)
                                .composition(CompositionFamiliale.builder()
                                                .sexeDefunt(request.getSexeDefunt())
                                                .nbConjoints(request.getNbConjoints() != null ? request.getNbConjoints()
                                                                : 0)
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
                                                .build())
                                .build();
        }
}
