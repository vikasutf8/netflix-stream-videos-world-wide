package com.netflix.videoservice.service.impl;

import com.netflix.videoservice.event.VideoUploadEvent;
import com.netflix.videoservice.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ServiceClientConfiguration;


@Service
@Slf4j
@RequiredArgsConstructor
public class VideoServiceImpl implements VideoService {

    private final S3Client s3Client;
    private final KafkaTemplate<String, VideoUploadEvent> kafkaTemplate;

    @Override
    public String uploadVideo(String videoId, MultipartFile videoData) {
        return "";
    }
}
