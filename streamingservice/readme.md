
# Streaming Service


video service--> videouploaded- producer
encoding service -> videouploaded- consumer
                --> videoencoded - producer
kafka:
topics:
video-uploaded: video.uploaded.events
video-encoded: video.encoded.events
streaming service --> videoencoded - consumer


```shell
video.encoded.events (Kafka)
        │
        ▼
VideoEncodedEventConsumer.onVideoEncoded()
        │
        ├── status = SUCCESS
        │       │
        │       └── StreamingService.saveStreamingMetadata()
        │               └── StreamingMetadata { movieId, masterPlaylistKey,
        │                                       qualities, status=READY } → PostgreSQL
        │
        └── status = FAILED
                └── log + ack (upstream already published FAILED)


Client Request
        │
        ▼
GET /api/v1/streaming/{movieId}/play
        │
        ▼
StreamingController → StreamingService.getStreamingUrl()
        │
        ├── fetch StreamingMetadata by movieId (status = READY)
        │       └── 404 STREAMING_NOT_READY if not found
        │
        ├── S3Presigner.presignGetObject(masterPlaylistKey, 60min)
        │
        └── StreamingResponse {
                movieId,
                streamingHlsMasterPlaylistUri: "https://s3.../hls/{id}/master.m3u8?X-Amz-...",
                qualities: ["1080p","720p","480p","360p"],
                expiryInMinutes: 60,
                generatedAt: "2026-05-17T..."
            }
```