package com.netflix.videoservice.exception;


import com.netflix.videoservice.globalResponse.ApiError;
import com.netflix.videoservice.globalResponse.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.validation.FieldError;
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

    @ExceptionHandler(S3UploadException.class)
    public ResponseEntity<ApiResponse<Void>> handleS3Upload(
            S3UploadException ex, HttpServletRequest req) {

        log.error("S3 upload failed at {}: {}", req.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.failure(ApiError.builder()
                        .status(502)
                        .code(ex.getErrorCode())
                        .message(ex.getMessage())
                        .path(req.getRequestURI())
                        .build()));
    }

    @ExceptionHandler(ContentServiceException.class)
    public ResponseEntity<ApiResponse<Void>> handleContentService(
            ContentServiceException ex, HttpServletRequest req) {

        log.warn("Domain error at {}: {}", req.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(ApiError.builder()
                        .status(400)
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
