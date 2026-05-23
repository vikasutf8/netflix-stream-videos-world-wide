package com.netflix.streamingservice.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {
    private final S3Client s3Client;

    private final S3Presigner s3Presigner;

    @Value("${aws.s3.encoded-bucket}")
    private String encodedBucket;

    @Getter
    @Value("${streaming.presigned-expiry-minutes:60}")
    private int expiryMinutes;
    //late say 60 minn...how to set eviction policy in redis for 60 minn..
    // we can set 55 minn for eviction policy in redis to avoid stale url

    /**
     * Generates a presigned GET URL for the HLS master playlist.
     * Client uses this directly — bucket stays fully private.
     */
    public String generatePresignedStreamingUrl(String masterPlaylistKey) {
        return generatePresignedObjectUrl(masterPlaylistKey);
    }

    public String generatePresignedObjectUrl(String objectKey) {
        var presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(expiryMinutes))
                .getObjectRequest(GetObjectRequest.builder()
                        .bucket(encodedBucket)
                        .key(objectKey)
                        .build())
                .build();

        var url = s3Presigner.presignGetObject(presignRequest).url().toString();

        log.info("Presigned URL generated | key={} expiresIn={}min",
                objectKey, expiryMinutes);

        return url;
    }

    public String readObjectAsString(String objectKey) {
        var request = GetObjectRequest.builder()
                .bucket(encodedBucket)
                .key(objectKey)
                .build();

        var content = s3Client.getObjectAsBytes(request).asUtf8String();
        log.info("S3 object read | key={} size={}chars", objectKey, content.length());
        return content;
    }

}
