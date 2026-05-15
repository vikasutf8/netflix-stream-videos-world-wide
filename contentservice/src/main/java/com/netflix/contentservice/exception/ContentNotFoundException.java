package com.netflix.contentservice.exception;

import org.hibernate.validator.constraints.UUID;

public class ContentNotFoundException extends ContentServiceException {
    public ContentNotFoundException(UUID id) {
        super("Content not found with id: " + id, "CONTENT_NOT_FOUND");
    }
}