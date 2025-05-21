package com.small.backend.accountservice.config;

import exception.GlobalExceptionHandler;
import exception.ResourceAlreadyExistsException;
import exception.ResourceNotFoundException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;

@Configuration
public class ExceptionHandlerConfig {

    @Bean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @ControllerAdvice
    public static class ExceptionHandlerAdvice {

        private final GlobalExceptionHandler globalExceptionHandler;

        public ExceptionHandlerAdvice(GlobalExceptionHandler globalExceptionHandler) {
            this.globalExceptionHandler = globalExceptionHandler;
        }

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<String> handleResourceNotFound(ResourceNotFoundException ex) {
            return globalExceptionHandler.handleResourceNotFound(ex);
        }

        @ExceptionHandler(ResourceAlreadyExistsException.class)
        public ResponseEntity<String> handleResourceAlreadyExists(ResourceAlreadyExistsException ex) {
            return globalExceptionHandler.handleResourceAlreadyExists(ex);
        }

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<String> handleAccessDenied(AccessDeniedException ex) {
            return globalExceptionHandler.handleAccessDenied(ex);
        }

        @ExceptionHandler(HttpClientErrorException.Unauthorized.class)
        public ResponseEntity<String> handleUnauthorized(HttpClientErrorException.Unauthorized ex) {
            return globalExceptionHandler.handleUnauthorized(ex);
        }

        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<String> handleBadCredentials(BadCredentialsException ex) {
            return globalExceptionHandler.handleBadCredentials(ex);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<String> handleGenericException(Exception ex) {
            return globalExceptionHandler.handleGenericException(ex);
        }
    }
}