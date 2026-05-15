package com.netflix.videoservice.event;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Published to Kafka after successful S3 upload.
 * Encoding Service consumes this and starts FFmpeg transcoding.
 */
@Getter
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoUploadedEvent {

    private UUID eventId;           // idempotency — encoding service deduplicates on this
    private UUID movieId;           // links back to Content entity
    private String videoKey;        // S3 object key of raw uploaded video
    private String bucket;          // S3 bucket name
    private String originalFilename;
    private Long fileSizeBytes;
    private String contentType;     // video/mp4 etc
    private Instant uploadedAt;
}
