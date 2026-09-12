package com.muhend.backendai.config.exception;

import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Refus de sécurité levés dans un contrôleur : 403 pour un rôle insuffisant, 401 pour des
 * identifiants refusés.
 *
 * Sans ce gestionnaire prioritaire, ces exceptions tombaient dans les gestionnaires génériques
 * ({@code @ExceptionHandler(Exception.class)} de GlobalExceptionHandler et CalculsExceptionHandler)
 * et sortaient en erreur 500.
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

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> authentificationRefusee(AuthenticationException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Nom d'utilisateur ou mot de passe incorrect."));
    }
}
