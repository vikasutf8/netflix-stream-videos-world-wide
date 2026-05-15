package com.netflix.contentservice.exception;

import com.netflix.contentservice.globalResponse.ApiError;
import com.netflix.contentservice.globalResponse.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── Domain exceptions ─────────────────────────────────────────────────────

    @ExceptionHandler(ContentNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(
            ContentNotFoundException ex, HttpServletRequest req) {

        log.warn("Content not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure(ApiError.builder()
                        .status(404)
                        .code(ex.getErrorCode())
                        .message(ex.getMessage())
                        .path(req.getRequestURI())
                        .build()));
    }

    @ExceptionHandler(DuplicateTitleException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicate(
            DuplicateTitleException ex, HttpServletRequest req) {

        log.warn("Duplicate title: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(ApiError.builder()
                        .status(409)
                        .code(ex.getErrorCode())
                        .message(ex.getMessage())
                        .path(req.getRequestURI())
                        .build()));
    }

    // ── Validation (@Valid failures) ──────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest req) {

        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value"
                ));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(ApiError.builder()
                        .status(400)
                        .code("VALIDATION_FAILED")
                        .message("Request validation failed")
                        .path(req.getRequestURI())
                        .fieldErrors(fieldErrors)
                        .build()));
    }

    // ── Catch-all ─────────────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(
            Exception ex, HttpServletRequest req) {

        log.error("Unexpected error at {}: {}", req.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure(ApiError.builder()
                        .status(500)
                        .code("INTERNAL_SERVER_ERROR")
                        .message("Something went wrong. Please try again later.")
                        .path(req.getRequestURI())
                        .build()));
    }
}
