package com.psyclone.url.shortener.exception;

import com.psyclone.url.shortener.domain.UrlErrors;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionhandler {

    @ExceptionHandler(UrlErrors.InvalidUrl.class)
    public ResponseEntity<ProblemDetail> invalid(UrlErrors.InvalidUrl ex) {
        return problem(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .findFirst()
                .orElse("Request validation failed");
        return problem(HttpStatus.BAD_REQUEST, detail);
    }

    @ExceptionHandler(UrlErrors.AliasTaken.class)
    public ResponseEntity<ProblemDetail> conflict(UrlErrors.AliasTaken ex) {
        return problem(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(UrlErrors.NotFound.class)
    public ResponseEntity<ProblemDetail> notFound(UrlErrors.NotFound ex) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(UrlErrors.Gone.class)
    public ResponseEntity<ProblemDetail> gone(UrlErrors.Gone ex) {
        return problem(HttpStatus.GONE, ex.getMessage());
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(ProblemDetail.forStatusAndDetail(status, detail));
    }
}
