package com.small.backend.accountservice.config;

import exception.GlobalExceptionHandler;
import exception.ResourceAlreadyExistsException;
import exception.ResourceNotFoundException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Configuration
public class ExceptionHandlerConfig {

    @Bean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @ControllerAdvice
    public static class ExceptionHandlerAdvice {

        private final GlobalExceptionHandler delegate;

        public ExceptionHandlerAdvice(GlobalExceptionHandler delegate) {
            this.delegate = delegate;
        }

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<String> handleResourceNotFound(ResourceNotFoundException ex) {
            return delegate.handleResourceNotFound(ex);
        }

        @ExceptionHandler(ResourceAlreadyExistsException.class)
        public ResponseEntity<String> handleResourceAlreadyExists(ResourceAlreadyExistsException ex) {
            return delegate.handleResourceAlreadyExists(ex);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<String> handleGenericException(Exception ex) {
            return delegate.handleGenericException(ex);
        }
    }
}