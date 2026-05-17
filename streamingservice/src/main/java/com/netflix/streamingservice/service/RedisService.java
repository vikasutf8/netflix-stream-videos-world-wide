package com.netflix.streamingservice.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, String> redisTemplate;

    // ── Key prefixes ──────────────────────────────────────────────────────────
    private static final String STREAMING_URL_PREFIX    = "streaming_url:";
    private static final String MASTER_PLAYLIST_PREFIX  = "master_playlist:";
    private static final String QUALITIES_PREFIX        = "streaming_qualities:";

    @Value("${redis.ttl.streaming-url-minutes:55}")
    private long streamingUrlTtlMinutes;

    @Value("${redis.ttl.master-playlist-minutes:1440}")
    private long masterPlaylistTtlMinutes;

    // ── Master Playlist Key ───────────────────────────────────────────────────

    /**
     * Stored by Kafka consumer when VideoEncodedEvent (SUCCESS) is received.
     * TTL: 24h — stable, changes only if movie is re-encoded.
     */
    public void saveMasterPlaylistKey(UUID movieId, String masterPlaylistKey) {
        var redisKey = MASTER_PLAYLIST_PREFIX + movieId;
        redisTemplate.opsForValue().set(
                redisKey, masterPlaylistKey,
                Duration.ofMinutes(masterPlaylistTtlMinutes));

        log.info("MasterPlaylistKey cached | movieId={} ttl={}min",
                movieId, masterPlaylistTtlMinutes);
    }

    public Optional<String> getMasterPlaylistKey(UUID movieId) {
        return Optional.ofNullable(
                redisTemplate.opsForValue().get(MASTER_PLAYLIST_PREFIX + movieId));
    }

    // ── Presigned Streaming URL ───────────────────────────────────────────────

    /**
     * Cached presigned URL — avoids hitting S3Presigner on every play request.
     * TTL: 55min — expires before the presigned URL itself (60min) to avoid serving stale URLs.
     */
    public void saveStreamingUrl(UUID movieId, String presignedUrl) {
        redisTemplate.opsForValue().set(
                STREAMING_URL_PREFIX + movieId,
                presignedUrl,
                Duration.ofMinutes(streamingUrlTtlMinutes));

        log.info("StreamingURL cached | movieId={} ttl={}min",
                movieId, streamingUrlTtlMinutes);
    }

    public Optional<String> getStreamingUrl(UUID movieId) {
        return Optional.ofNullable(
                redisTemplate.opsForValue().get(STREAMING_URL_PREFIX + movieId));
    }

    // ── Qualities ─────────────────────────────────────────────────────────────

    /**
     * Comma-separated qualities e.g. "1080p,720p,480p,360p"
     * Stored alongside master playlist key — same TTL.
     */
    public void saveQualities(UUID movieId, java.util.List<String> qualities) {
        redisTemplate.opsForValue().set(
                QUALITIES_PREFIX + movieId,
                String.join(",", qualities),
                Duration.ofMinutes(masterPlaylistTtlMinutes));
    }

    public java.util.List<String> getQualities(UUID movieId) {
        var raw = redisTemplate.opsForValue().get(QUALITIES_PREFIX + movieId);
        if (raw == null || raw.isBlank()) return java.util.List.of();
        return java.util.List.of(raw.split(","));
    }

    // ── Eviction ──────────────────────────────────────────────────────────────

    /** Call when re-encoding triggers — evict stale cache for this movie. */
    public void evictMovieCache(UUID movieId) {
        redisTemplate.delete(java.util.List.of(
                STREAMING_URL_PREFIX   + movieId,
                MASTER_PLAYLIST_PREFIX + movieId,
                QUALITIES_PREFIX       + movieId
        ));
        log.info("Cache evicted | movieId={}", movieId);
    }
}
