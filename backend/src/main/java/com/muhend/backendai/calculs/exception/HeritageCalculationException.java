package com.muhend.backendai.calculs.exception;

public class HeritageCalculationException extends RuntimeException {

    public HeritageCalculationException(String message) {
        super(message);
    }

    public HeritageCalculationException(String message, Throwable cause) {
        super(message, cause);
    }
}
