package com.netflix.videoservice.exception;

public class DuplicateTitleException extends ContentServiceException {
    public DuplicateTitleException(String title) {
        super("Content already exists with title: " + title, "DUPLICATE_TITLE");
    }
}
