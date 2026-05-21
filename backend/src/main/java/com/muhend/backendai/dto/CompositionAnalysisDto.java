package com.muhend.backendai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompositionAnalysisDto {
    private String numFrida;
    private boolean hasBoy;
    private boolean hasFather;
    private boolean hasRemainingPart;
    private String remainingPartDetails; // e.g., "1/6" ou description textuelle
}
