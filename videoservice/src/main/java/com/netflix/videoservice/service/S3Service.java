package com.netflix.videoservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.presigned-expiry-minutes}")
    private long presignedExpiryMinutes;

    /**
     * Directly uploads the multipart file to S3.
     * Returns the S3 object key.
     */
    public String uploadVideo(UUID movieId, MultipartFile file) {
        String videoKey = buildVideoKey(movieId, file.getOriginalFilename());

        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(videoKey)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            log.info("Video uploaded to S3: bucket={}, key={}", bucket, videoKey);
            return videoKey;

        } catch (IOException e) {
            throw new RuntimeException("Failed to upload video to S3: " + e.getMessage(), e);
        }
    }

    /**
     * Generates a presigned PUT URL — client uploads directly to S3.
     * Server never buffers the file. Preferred for large files.
     */
    public String generatePresignedUploadUrl(UUID movieId, String originalFilename) {
        String videoKey = buildVideoKey(movieId, originalFilename);

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(presignedExpiryMinutes))
                .putObjectRequest(r -> r.bucket(bucket).key(videoKey))
                .build();

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);
        log.info("Presigned URL generated for key={}", videoKey);

        return presigned.url().toString();
    }

    // videos/raw/{movieId}/{uuid}_{filename}.mp4
    private String buildVideoKey(UUID movieId, String originalFilename) {
        return String.format("videos/raw/%s/%s_%s",
                movieId,
                UUID.randomUUID(),       // prevents overwrite if same file re-uploaded
                sanitize(originalFilename));
    }

    private String sanitize(String filename) {
        return filename == null ? "video.mp4"
                : filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}