package com.yugibuilder.cardimporter.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// SOLUTION 1: Add @Hidden annotation to fix Swagger 500 error
import io.swagger.v3.oas.annotations.Hidden;

/**
 * FIXED: Global Exception Handler with @Hidden annotation
 * This prevents SpringDoc from trying to document exception handler methods
 * which was causing the 500 error on /v3/api-docs
 */
@RestControllerAdvice
@Hidden // This annotation fixes the Swagger 500 error
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    @Hidden // Also hide individual methods to be extra safe
    public ResponseEntity<String> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("❌ Une erreur est survenue : " + ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @Hidden
    public ResponseEntity<String> handleGeneralException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("❌ Erreur interne du serveur : " + ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @Hidden
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("❌ Argument invalide : " + ex.getMessage());
    }
}