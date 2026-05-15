package com.netflix.contentservice.exception;

public class DuplicateTitleException extends ContentServiceException {
    public DuplicateTitleException(String title) {
        super("Content already exists with title: " + title, "DUPLICATE_TITLE");
    }
}
