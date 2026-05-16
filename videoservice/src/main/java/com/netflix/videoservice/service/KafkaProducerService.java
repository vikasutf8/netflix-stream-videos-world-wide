package com.netflix.videoservice.service;

import com.netflix.videoservice.event.VideoUploadedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, VideoUploadedEvent> kafkaTemplate;

    @Value("${kafka.topics.video-uploaded}")
    private String videoUploadedTopic;

    /**
     * movieId as key → same movie's events always go to same partition
     * → encoding service processes them in order per movie
     */
    public void publishVideoUploadedEvent(VideoUploadedEvent event) {
        CompletableFuture<SendResult<String, VideoUploadedEvent>> future =
                kafkaTemplate.send(
                        videoUploadedTopic,
                        event.getMovieId().toString(),   // partition key
                        event
                );
        // listen to encoding service asynchronously

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish VideoUploadedEvent: movieId={}, error={}",
                        event.getMovieId(), ex.getMessage(), ex);
                // TODO: push to dead-letter topic or outbox table for retry
            } else {
                log.info("VideoUploadedEvent published: movieId={}, partition={}, offset={}",
                        event.getMovieId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
