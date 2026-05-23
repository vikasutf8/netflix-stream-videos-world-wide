package com.netflix.streamingservice.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;

import java.time.Duration;
import java.util.Base64;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
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
    private static final String SIGNED_PLAYLIST_PREFIX  = "signed_playlist:";

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
        if (qualities == null || qualities.isEmpty()) {
            redisTemplate.delete(QUALITIES_PREFIX + movieId);
            return;
        }
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

    // ── Signed Playlist Content ──────────────────────────────────────────────

    /**
     * Signed playlist content cache (m3u8 with per-line presigned URLs).
     * TTL aligns with presigned URL cache to avoid stale entries.
     */
    public void saveSignedPlaylist(UUID movieId, String playlistPath, String signedPlaylistContent) {
        redisTemplate.opsForValue().set(
                signedPlaylistRedisKey(movieId, playlistPath),
                signedPlaylistContent,
                Duration.ofMinutes(streamingUrlTtlMinutes));

        log.info("SignedPlaylist cached | movieId={} path={} ttl={}min",
                movieId, playlistPath, streamingUrlTtlMinutes);
    }

    public Optional<String> getSignedPlaylist(UUID movieId, String playlistPath) {
        return Optional.ofNullable(
                redisTemplate.opsForValue().get(signedPlaylistRedisKey(movieId, playlistPath)));
    }

    public void warmStreamingMetadataCache(
            UUID movieId,
            String masterPlaylistKey,
            java.util.List<String> qualities) {

        if (masterPlaylistKey == null || masterPlaylistKey.isBlank()) {
            throw new IllegalArgumentException("masterPlaylistKey must not be blank for SUCCESS events");
        }

        evictMovieCache(movieId);
        saveMasterPlaylistKey(movieId, masterPlaylistKey);
        saveQualities(movieId, qualities);

        log.info("Streaming metadata cache warmed | movieId={} hasQualities={}",
                movieId, qualities != null && !qualities.isEmpty());
    }

    // ── Eviction ──────────────────────────────────────────────────────────────

    /** Call when re-encoding triggers — evict stale cache for this movie. */
    public void evictMovieCache(UUID movieId) {
        Set<String> keysToDelete = new HashSet<>(java.util.List.of(
                STREAMING_URL_PREFIX + movieId,
                MASTER_PLAYLIST_PREFIX + movieId,
                QUALITIES_PREFIX + movieId
        ));

        Set<String> signedPlaylistKeys = redisTemplate.keys(SIGNED_PLAYLIST_PREFIX + movieId + ":*");
        if (signedPlaylistKeys != null && !signedPlaylistKeys.isEmpty()) {
            keysToDelete.addAll(signedPlaylistKeys);
        }

        redisTemplate.delete(keysToDelete);
        log.info("Cache evicted | movieId={} deletedKeys={}", movieId, keysToDelete.size());
    }

    private String signedPlaylistRedisKey(UUID movieId, String playlistPath) {
        String encodedPath = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(playlistPath.getBytes(StandardCharsets.UTF_8));
        return SIGNED_PLAYLIST_PREFIX + movieId + ":" + encodedPath;
    }
}
