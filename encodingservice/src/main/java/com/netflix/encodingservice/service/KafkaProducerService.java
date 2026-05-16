package com.netflix.encodingservice.service;

import com.netflix.encodingservice.event.VideoEncodedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, VideoEncodedEvent> kafkaTemplate;

    @Value("${kafka.topics.video-encoded}")
    private String videoEncodedTopic;
// PRODUCER SIDE --encoding event pulblic for string
    public void publishVideoEncodedEvent(VideoEncodedEvent event) {
        kafkaTemplate.send(
                videoEncodedTopic,
                event.getMovieId().toString(),   // partition key — same as producer side
                event
        ).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish VideoEncodedEvent: movieId={}, error={}",
                        event.getMovieId(), ex.getMessage());
            } else {
                log.info("VideoEncodedEvent published: movieId={}, status={}, partition={}",
                        event.getMovieId(),
                        event.getStatus(),
                        result.getRecordMetadata().partition());
            }
        });
    }
}
