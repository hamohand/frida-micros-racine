package com.muhend.backendai.config.exception;

import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Refus d'un {@code @PreAuthorize} : 403 avec message.
 *
 * Sans ce gestionnaire prioritaire, l'exception tombait dans les gestionnaires génériques
 * ({@code @ExceptionHandler(Exception.class)} de GlobalExceptionHandler et CalculsExceptionHandler)
 * et sortait en erreur 500.
 */
@Profile("!calc-only")
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AccesRefuseExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> accesRefuse(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Action réservée au compte Maître."));
    }
}
