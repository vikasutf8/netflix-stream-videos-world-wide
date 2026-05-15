package com.netflix.videoservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;

public class KafkaConfig {

    @Value("${kafka.topics.video-uploaded}")
    private String videoUploadedTopic;

    @Bean
    public NewTopic videoUploadedTopic() {
        return TopicBuilder.name(videoUploadedTopic)
                .partitions(6)      // parallelism — 6 encoding workers can consume simultaneously
                .replicas(3)        // fault tolerance
                .build();
    }
}
