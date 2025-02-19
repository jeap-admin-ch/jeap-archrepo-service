package ch.admin.bit.jeap.archrepo.web.rest;

import ch.admin.bit.jeap.archrepo.importer.openapi.OpenApiFileParsingException;
import ch.admin.bit.jeap.archrepo.web.rest.openapi.OpenApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidationException(MethodArgumentNotValidException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(OpenApiFileParsingException.class)
    public ResponseEntity<String> handleOpenApiFileParsingException(OpenApiFileParsingException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(OpenApiException.class)
    public ResponseEntity<String> handleOpenApiException(OpenApiException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}