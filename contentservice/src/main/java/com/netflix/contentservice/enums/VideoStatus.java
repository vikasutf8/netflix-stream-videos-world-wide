package com.netflix.contentservice.enums;

// VideoStatus.java
public enum VideoStatus {
    PENDING,  // movie added not yet uploaded
    UPLOADED,
    ENCODING,
    ENCODED,
    READY,
    FAILED,
//    PROCESSING,   // uploaded, not yet transcoded
//    READY,        // HLS transcoding done, playable
//    FAILED,       // transcoding failed
//    ARCHIVED,     // soft-removed from catalog
//    DRAFT         // not yet published
}
