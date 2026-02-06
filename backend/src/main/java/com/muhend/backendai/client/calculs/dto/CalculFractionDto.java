package com.muhend.backendai.client.calculs.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalculFractionDto {
    private int numerateur;
    private int denominateur;

    @Override
    public String toString() {
        return numerateur + "/" + denominateur;
    }
}
