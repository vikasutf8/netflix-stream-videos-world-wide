package com.netflix.encodingservice.service;

import com.netflix.encodingservice.event.VideoEncodedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class EncodingService {


    private final S3Client s3Client;
    private final KafkaTemplate<String, VideoEncodedEvent> kafkaTemplate;
    @Value("${aws.s3.bucket}")
    private String bucket;
    @Value("${ffmpeg.path}")
    private String ffmpegPath = "/usr/bin/ffmpeg"; // Path to FFmpeg binary

    private static final String VIDEO_ENCODED_TOPIC = "video-encoded";

    //video quatilty to encode
    // format : resolution, bitrate, height

    private static final List<VideoFormat> targetFormats = List.of(
            new VideoFormat("720p", "1280x720", "2M"),
            new VideoFormat("480p", "854x480", "1M"),
            new VideoFormat("360p", "640x360", "500k")
    );


}
