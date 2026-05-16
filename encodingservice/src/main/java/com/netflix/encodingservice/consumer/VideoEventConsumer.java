package com.netflix.encodingservice.consumer;

import com.netflix.encodingservice.event.VideoEncodedEvent;
import com.netflix.encodingservice.event.VideoUploadedEvent;
import com.netflix.encodingservice.service.EncodingService;
import com.netflix.encodingservice.service.KafkaProducerService;
import com.netflix.encodingservice.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoEventConsumer {

    private final EncodingService encodingService;
    private final S3Service s3Service;
    private final KafkaProducerService kafkaProducerService;

    /**
     * Listens to video.uploaded.events
     * Manual ack — we only commit offset after full encode + upload succeeds.
     * If encoding crashes midway, Kafka redelivers and we retry from scratch.
     */
    @KafkaListener(
            topics         = "${kafka.topics.video-uploaded}",
            groupId        = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )

    /*
     *
     * Download raw video from s3 ---its present in s3service ===how to use it -- videoeventconsumer
     * encode to mutliple qualities using ffmpeg -- encodeToHLS in which encode and generate master playlist
     *
     * gnerate HLS playlist for each quality +
     * create master playlist
     * upload  all encode chunks or file to s3
     *
     * Kafka videoEncodedEvent with all the details of the encoded video
     *
     * */
//CONSUMER of videoService
    public void onVideoUploaded(
            @Payload VideoUploadedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment ack) {

        UUID movieId = event.getMovieId();

        log.info("VideoUploadedEvent received: movieId={}, key={}, partition={}, offset={}",
                movieId, event.getVideoKey(), partition, offset);

        Path rawVideoPath = null;
        Path workDir      = null;

        try {

            // ── Step 1: download raw file from S3 ─────────────────────────────
            //TODO:
            // we need directories --videos and encodedvideo ---muttiple videos M1,M2,M3 --- each video has multiple encoding jobs --M1-1080p, M1-720p, M1-480p, M1-360p
            rawVideoPath = s3Service.downloadRawVideo(event.getVideoKey(), movieId);
            log.info("Raw video downloaded: movieId={}", movieId);

            // ── Step 2: encode with FFmpeg → HLS variants ─────────────────────
            workDir = encodingService.encodeToHls(rawVideoPath, movieId);
            log.info("Encoding complete: movieId={}", movieId);

            // ── Step 3: upload all HLS chunks + playlists to S3 ───────────────
            List<String> uploadedKeys = s3Service.uploadHlsOutput(workDir, movieId);
            String masterPlaylistKey  = "hls/" + movieId + "/master.m3u8";
            log.info("HLS output uploaded: {} files, movieId={}", uploadedKeys.size(), movieId);
            String hlsUrl = String.format(
                    "https://%s.s3.amazonaws.com/%s",
                    event.getBucket(),
                    masterPlaylistKey
            );
            // ── Step 4: publish VideoEncodedEvent ─────────────────────────────
            VideoEncodedEvent encodedEvent = VideoEncodedEvent.builder()
                    .eventId(UUID.randomUUID())
                    .movieId(movieId)
                    .hlsUri(hlsUrl) // why its using its Never be public
                    .masterPlaylistKey(masterPlaylistKey)
                    .variantKeys(uploadedKeys)
                    .encodedQualities(List.of("1080p", "720p", "480p", "360p"))
                    .bucket(event.getBucket())
                    .encodedAt(Instant.now())
                    .status(VideoEncodedEvent.EncodingStatus.SUCCESS)
                    .build();

            kafkaProducerService.publishVideoEncodedEvent(encodedEvent);

            // ── Step 5: commit offset — only after everything succeeds ─────────
            ack.acknowledge();
            log.info("Encoding pipeline complete: movieId={}", movieId);

        } catch (Exception ex) {
            log.error("Encoding pipeline FAILED: movieId={}, error={}", movieId, ex.getMessage(), ex);

            // publish FAILED event — content-service can update status to FAILED
            kafkaProducerService.publishVideoEncodedEvent(
                    VideoEncodedEvent.builder()
                            .eventId(UUID.randomUUID())
                            .movieId(movieId)
                            .status(VideoEncodedEvent.EncodingStatus.FAILED)
                            .errorMessage(ex.getMessage())
                            .encodedAt(Instant.now())
                            .build()
            );

            // DO NOT ack — Kafka will redeliver after consumer restart
            // For poison messages add a retry count header check here

        } finally {
            // ── Cleanup temp files regardless of outcome ──────────────────────
            cleanup(rawVideoPath);
            cleanup(workDir);
        }
    }

    private void cleanup(Path path) {
        if (path == null) return;
        try {
            if (Files.isDirectory(path)) {
                Files.walk(path)
                        .sorted(java.util.Comparator.reverseOrder())
                        .forEach(p -> { try { Files.delete(p); } catch (Exception ignored) {} });
            } else {
                Files.deleteIfExists(path);
            }
        } catch (Exception ex) {
            log.warn("Temp cleanup failed for path={}: {}", path, ex.getMessage());
        }
    }
}
