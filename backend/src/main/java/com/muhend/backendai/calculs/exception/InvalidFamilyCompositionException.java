package com.muhend.backendai.calculs.exception;

public class InvalidFamilyCompositionException extends RuntimeException {

    public InvalidFamilyCompositionException(String message) {
        super(message);
    }

    public InvalidFamilyCompositionException(String message, Throwable cause) {
        super(message, cause);
    }
}
