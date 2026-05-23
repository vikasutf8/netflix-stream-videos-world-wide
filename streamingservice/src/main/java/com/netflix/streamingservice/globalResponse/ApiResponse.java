package com.netflix.streamingservice.globalResponse;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)  // hides null errorDetails in success responses
public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;
    private final ApiError error;

    @Builder.Default
    private final Instant timestamp = Instant.now();

    // ── Static factories ──────────────────────────────────────────────────────

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> failure(ApiError error) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(error)
                .build();
    }
}
