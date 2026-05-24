package com.muhend.backendai.dto;

import lombok.Data;
import java.util.List;

@Data
public class FicheUpdateDto {
    private PersonneUpdateDto defunt;
    private List<PersonneUpdateDto> heritiers;
}
