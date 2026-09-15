package com.muhend.nfcagent.dto;

import lombok.Data;

@Data
public class MrzRequestDto {
    private String documentNumber;
    private String dateOfBirth; // format yyMMdd
    private String dateOfExpiry; // format yyMMdd
}
