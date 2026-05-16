# Encoding serivice

0. listen as consumer video service events from kafka topic
1. download raws file
2. endcoide ffmeg with different quality and veritent
3. gnerate HLS playlist for each verient
4. upload all encoded chuncks s3
5. pulbish video encoded events chuncks to kafka 

```shell

video.uploaded.events (Kafka)
        │
        ▼
VideoEventConsumer.onVideoUploaded()
        │
        ├─ 1. S3Service.downloadRawVideo()
        │       s3://netflix-raw-videos/{videoKey} → /tmp/{jobId}/raw.mp4
        │
        ├─ 2. EncodingService.encodeToHls()
        │       FFmpeg × 4 variants (1080p / 720p / 480p / 360p)
        │       /tmp/{jobId}/
        │           1080p/playlist.m3u8 + segment_000.ts ...
        │           720p/ ...
        │           master.m3u8
        │
        ├─ 3. S3Service.uploadHlsOutput()
        │       s3://netflix-encoded-videos/hls/{movieId}/master.m3u8
        │       s3://netflix-encoded-videos/hls/{movieId}/720p/playlist.m3u8
        │       s3://netflix-encoded-videos/hls/{movieId}/720p/segment_000.ts ...
        │
        ├─ 4. KafkaProducerService.publishVideoEncodedEvent()
        │       topic : video.encoded.events
        │       status: SUCCESS | FAILED
        │
        ├─ 5. ack.acknowledge()  ← offset committed only here
        │
        └─ finally: cleanup /tmp/{jobId}


video.encoded.events (Kafka)
        │
        ▼
Content Service consumer (next)
        └─ videoStatus = READY
           hlsUri = "hls/{movieId}/master.m3u8"
```

### Video Format
```shell

/**
 * Immutable value object — defines one encoding target.
 * resolution : FFmpeg scale filter value  e.g. "1280x720"
 * bitrate    : video bitrate              e.g. "2M"
 * label      : human name                e.g. "720p"
 */
```
```shell
 /**
     * Encodes raw video into HLS variants.
     * Returns path to the temp working directory containing all output files.
     *
     * workDir layout after encoding:
     *   /tmp/{jobId}/
     *     720p/
     *       playlist.m3u8
     *       segment_000.ts
     *       segment_001.ts  ...
     *     480p/ ...
     *     360p/ ...
     *     master.m3u8
     */
```