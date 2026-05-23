
# Streaming Service


video service--> videouploaded- producer
encoding service -> videouploaded- consumer
                --> videoencoded - producer
kafka:
topics:
video-uploaded: video.uploaded.events
video-encoded: video.encoded.events
streaming service --> videoencoded - consumer

---
    /**
     * GET /api/v1/streaming/{movieId}/play
     *
     * Flow:
     * 1. check Redis for master_playlist:{movieId}       → 404 if missing (not encoded yet) or first time its not present ...is how to store or why not before do it
     * 2. check Redis for streaming_url:{movieId}         → return cached if hit
     * 3. cache MISS → generate fresh presigned URL via S3Service ---this process take time to do
     * 4. cache fresh URL → return
     */
---

/**
* GET : signedPlaylist
  *
* 1. server signed m3u8 playlist context
* 2 . callled by HLS players for each quality playlist
  *  moveId and path
  *  inside it :
  *      1 find basePath  ..from path
  *      2. read m3u8 content from s3
  *      3. rewirte each line that is a segemnt or playlist reference to have presigned url
*/

---

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


## Redis Keys:
─────────────────────────────────────────────────────────────────────
master_playlist:{movieId}   → "hls/{movieId}/master.m3u8"     TTL: 24h
set by: Kafka consumer (VideoEncodedEvent)
read by: StreamingController (Step 1)

streaming_url:{movieId}     → "https://s3.../master.m3u8?X-Amz-..."  TTL: 55min
set by: StreamingServiceImpl (on cache miss)
read by: StreamingServiceImpl (cache hit check)

streaming_qualities:{movieId} → "1080p,720p,480p,360p"         TTL: 24h
set by: Kafka consumer
read by: StreamingServiceImpl (response building)