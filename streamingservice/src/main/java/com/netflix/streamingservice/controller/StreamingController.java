package com.netflix.streamingservice.controller;

import com.netflix.streamingservice.dto.response.StreamingResponse;
import com.netflix.streamingservice.globalResponse.ApiError;
import com.netflix.streamingservice.globalResponse.ApiResponse;
import com.netflix.streamingservice.service.RedisService;
import com.netflix.streamingservice.service.StreamingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/streaming")
@RequiredArgsConstructor
public class StreamingController {

    private final StreamingService              streamingService;
    private final RedisService redisService;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String STREAMING_URL_CACHE_PREFIX  = "streaming_url:";
    private static final String MASTER_PLAYLIST_KEY_PREFIX  = "master_playlist:";

    /**
     * GET /api/v1/streaming/{movieId}/play
     * Flow:
     * 1. check Redis for master_playlist:{movieId}       → 404 if missing (not encoded yet)
     * 2. check Redis for streaming_url:{movieId}         → return cached if hit
     * 3. cache MISS → generate fresh presigned URL via S3Service
     * 4. cache fresh URL → return
     */
    @GetMapping("/{movieId}/play")
    public ResponseEntity<ApiResponse<StreamingResponse>> getStreamingUrl(
            @PathVariable UUID movieId) {

        log.info("Streaming request | movieId={}", movieId);

        // ── Step 1: get master playlist key from Redis ────────────────────────
        String masterPlaylistKey = redisTemplate.opsForValue()
                .get(MASTER_PLAYLIST_KEY_PREFIX + movieId);

        if (masterPlaylistKey == null) {
            log.warn("Master playlist key not found in Redis | movieId={}", movieId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.failure(
                            ApiError.builder()
                                    .status(404)
                                    .code("STREAMING_NOT_READY")
                                    .message("Video is still processing or not found: " + movieId)
                                    .path("/api/v1/streaming/" + movieId + "/play")
                                    .build()));
        }

        // ── Step 2 + 3 + 4: handled inside service (cache hit/miss + presign) ─
        StreamingResponse response = streamingService.getStreamingUrl(movieId, masterPlaylistKey);
// step nnext - -> 5 --removeing  presigned url from redis after 55 minn to avoid stale url --- prevent HOT key
        return ResponseEntity.ok(ApiResponse.success(response, "Streaming URL ready"));
    }


    /**
     * DELETE /api/v1/streaming/{movieId}/cache
     *
     * Admin endpoint — manually evict cache for a movie.
     * Useful when re-encoding is triggered manually.
     */
    @DeleteMapping("/{movieId}/cache")
    public ResponseEntity<ApiResponse<Void>> evictCache(@PathVariable UUID movieId) {
        redisService.evictMovieCache(movieId);
        log.info("Cache manually evicted | movieId={}", movieId);
        return ResponseEntity.ok(ApiResponse.success(null, "Cache evicted for movieId: " + movieId));
    }

    /**
     * GET /api/v1/streaming/{movieId}/status
     *
     * Quick check — is this movie ready to stream?
     * Reads only from Redis — zero DB hit.
     */
    @GetMapping("/{movieId}/status")
    public ResponseEntity<ApiResponse<String>> getStreamingStatus(
            @PathVariable UUID movieId) {

        boolean isReady = redisService.getMasterPlaylistKey(movieId).isPresent();
        String  status  = isReady ? "READY" : "NOT_READY";

        return ResponseEntity.ok(ApiResponse.success(status,
                "Streaming status for movieId: " + movieId));
    }
}
