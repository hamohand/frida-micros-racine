package com.muhend.backendai.calculs.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Réponse d'erreur standardisée (RFC 7807 - Problem Details)")
public class ErrorResponse {

    @Schema(description = "Timestamp de l'erreur", example = "2025-12-21T10:30:00")
    private LocalDateTime timestamp;

    @Schema(description = "Code de statut HTTP", example = "400")
    private int status;

    @Schema(description = "Nom du statut HTTP", example = "BAD_REQUEST")
    private String error;

    @Schema(description = "Message d'erreur principal", example = "Données de famille invalides")
    private String message;

    @Schema(description = "Chemin de la requête", example = "/api/v1/heritage/calculate")
    private String path;

    @Schema(description = "Liste des erreurs de validation")
    private List<ValidationError> validationErrors;

    @Schema(description = "Détails supplémentaires")
    private Map<String, Object> details;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationError {
        @Schema(description = "Nom du champ en erreur", example = "nbFilles")
        private String field;

        @Schema(description = "Valeur rejetée", example = "-1")
        private Object rejectedValue;

        @Schema(description = "Message d'erreur", example = "Le nombre de filles ne peut pas être négatif")
        private String message;
    }
}
