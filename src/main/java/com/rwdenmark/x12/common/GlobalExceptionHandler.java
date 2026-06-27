package com.rwdenmark.x12.common;

import com.rwdenmark.x12.parser.X12ParseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * Returns errors as a plain-text message the UI can show directly, instead of
 * Spring's default /error JSON (timestamp/status/path), which carried no reason.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> handleStatus(ResponseStatusException ex) {
        String message = ex.getReason() != null ? ex.getReason() : ex.getStatusCode().toString();
        return text(ex.getStatusCode(), message);
    }

    @ExceptionHandler({X12ParseException.class, IllegalArgumentException.class})
    public ResponseEntity<String> handleBadRequest(Exception ex) {
        return text(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    private static ResponseEntity<String> text(HttpStatusCode status, String body) {
        return ResponseEntity.status(status).contentType(MediaType.TEXT_PLAIN).body(body);
    }
}
