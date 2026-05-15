package com.netflix.videoservice.globalResponse;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    private final int status;
    private final String code;           // machine-readable  e.g. "CONTENT_NOT_FOUND"
    private final String message;        // human-readable
    private final String path;           // which endpoint was hit
    private final Map<String, String> fieldErrors;  // validation errors per field
}
