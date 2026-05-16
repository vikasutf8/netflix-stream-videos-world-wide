package com.netflix.videoservice.service.impl;

import com.netflix.videoservice.event.VideoUploadedEvent;
import com.netflix.videoservice.service.KafkaProducerService;
import com.netflix.videoservice.service.S3Service;
import com.netflix.videoservice.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.InvalidFileNameException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


import java.time.Instant;
import java.util.List;
import java.util.UUID;



@Service
@Slf4j
@RequiredArgsConstructor
public class VideoServiceImpl implements VideoService {

    private final S3Service s3Service;
    private final KafkaProducerService kafkaProducerService;


    @Value("${aws.s3.bucket}")
    private String bucket;

    private static final List<String> ALLOWED_TYPES =
            List.of("video/mp4", "video/x-matroska", "video/quicktime");

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {

            throw new InvalidFileNameException(file.getName(), "File is empty or missing");
        }

        if (!ALLOWED_TYPES.contains(file.getContentType()))
            throw new InvalidFileNameException(file.getContentType(),
                    "Unsupported type: " + file.getContentType()
                            + ". Allowed: " + ALLOWED_TYPES);
    }

    @Override
    public String uploadVideo(String movieId, MultipartFile file) {
        log.info("Start ------Uploading video: id={}, filename={}, size={} bytes, type={}",
                movieId, file.getOriginalFilename(), file.getSize(), file.getContentType());

        // ---step 1.. validate file
        validateFile(file);

        // ── Step 2: generate unique S3 key ────────────────────────────────────
        // ── Step 3: request upload to S3 ──Format: .raw/movieID/uuid_filename────────────────────────────────────────────

        String videoKey = s3Service.uploadVideo(UUID.fromString(movieId), file);



        // ----step4 : create an event and push that on kafka as producer ....
        // producer must be ordered and idempotent to avoid duplicate processing and out of order processing in consumer side
        // ── Step 5: publish VideoUploadedEvent to Kafka ───────────────────────
        VideoUploadedEvent event = VideoUploadedEvent.builder()
                .eventId(UUID.randomUUID())
                .movieId(UUID.fromString(movieId))
                .videoKey(videoKey)
                .bucket(bucket)
                .originalFilename(file.getOriginalFilename())
                .fileSizeBytes(file.getSize())
                .contentType(file.getContentType())
                .uploadedAt(Instant.now())
                .build();

        kafkaProducerService.publishVideoUploadedEvent(event);
        log.info("VideoUploadedEvent published: movieId={}", movieId);
        return "";
    }
}
