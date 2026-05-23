package com.netflix.streamingservice.consumer;

import com.netflix.streamingservice.event.VideoEncodedEvent;
import com.netflix.streamingservice.service.RedisService;
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

    private final RedisService redisService;

    /**
     * Listen encoded video events from Kafka.
     * On SUCCESS, cache master playlist key (and qualities) in Redis.
     * On FAILED, evict stale cache for that movie.
     */
    @RetryableTopic(
            attempts               = "4",
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
                event.getMovieId(), event.getStatus(), partition, offset);

        if (event.getStatus() == null) {
            log.error("VideoEncodedEvent status missing | movieId={}", event.getMovieId());
            ack.acknowledge();
            return;
        }

        switch (event.getStatus()) {
            case SUCCESS -> {
                redisService.warmStreamingMetadataCache(
                        event.getMovieId(),
                        event.getMasterPlaylistKey(),
                        event.getEncodedQualities());

                ack.acknowledge();
                log.info("Redis cache warmed from VideoEncodedEvent ✓ | movieId={}", event.getMovieId());
            }

            case FAILED -> {
                redisService.evictMovieCache(event.getMovieId());
                log.error("Encoding FAILED upstream | movieId={} reason={}",
                        event.getMovieId(), event.getErrorMessage());
                ack.acknowledge();
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

        log.error("DLT — retries exhausted | movieId={}", event.getMovieId());
        redisService.evictMovieCache(event.getMovieId());
        // TODO: trigger ops alert — SNS / PagerDuty
        ack.acknowledge();
    }
}
