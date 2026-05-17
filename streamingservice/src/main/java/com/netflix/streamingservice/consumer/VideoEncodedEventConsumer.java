package com.netflix.streamingservice.consumer;

import com.netflix.streamingservice.event.VideoEncodedEvent;
import com.netflix.streamingservice.service.RedisService;
import com.netflix.streamingservice.service.StreamingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoEncodedEventConsumer {

    private final StreamingService streamingService;
    private final RedisService redisService;

    @RetryableTopic(
            attempts               = "4",
            backoff                = @Backoff(delay = 5_000, multiplier = 2),
            autoCreateTopics       = "true",
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltTopicSuffix         = ".DLT"
    )
    @KafkaListener(
            topics           = "${kafka.topics.video-encoded}",
            groupId          = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onVideoEncoded(
            @Payload VideoEncodedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET)             long offset,
            Acknowledgment ack) {

        log.info("VideoEncodedEvent received | movieId={} status={} partition={} offset={}",
                event.movieId(), event.status(), partition, offset);

        switch (event.status()) {

            case SUCCESS -> {
                // ── persist to PostgreSQL + warm Redis ────────────────────────
                streamingService.saveStreamingMetadata(
                        event.movieId(),
                        event.masterPlaylistKey(),
                        event.bucket(),
                        event.encodedQualities()
                );

                ack.acknowledge();
                log.info("StreamingMetadata saved + Redis warmed ✓ | movieId={}", event.movieId());
            }

            case FAILED -> {
                // evict any stale cache — video is in failed state
                redisService.evictMovieCache(event.movieId());

                log.error("Encoding FAILED upstream | movieId={} reason={}",
                        event.movieId(), event.failureReason());

                ack.acknowledge();  // don't retry — failure is final from encoding service
            }
        }
    }

    @KafkaListener(
            topics  = "${kafka.topics.video-encoded}.DLT",
            groupId = "${spring.kafka.consumer.group-id}-dlt"
    )
    public void onDeadLetter(
            @Payload VideoEncodedEvent event,
            Acknowledgment ack) {

        log.error("DLT — retries exhausted | movieId={}", event.movieId());
        redisService.evictMovieCache(event.movieId());
        // TODO: trigger ops alert — SNS / PagerDuty
        ack.acknowledge();
    }
}
