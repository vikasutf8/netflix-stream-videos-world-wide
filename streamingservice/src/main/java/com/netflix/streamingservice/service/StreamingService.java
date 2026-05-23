package com.netflix.streamingservice.service;

import com.netflix.streamingservice.dto.response.StreamingResponse;
import com.netflix.streamingservice.repository.StreamingMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StreamingService {
    private final StreamingMetadataRepository metadataRepository;
    private final RedisService                redisService; // redis checking again present nopt presinge url
    private final S3Service                   s3Service; //using for create  new  presigned url

//    @Transactional
//    public void saveStreamingMetadata(UUID movieId, String masterPlaylistKey,
//                                      String bucket, List<String> qualities) {
//
//        // ── 1. upsert PostgreSQL ──────────────────────────────────────────────
//        var metadata = metadataRepository.findByMovieId(movieId)
//                .orElse(StreamingMetadata.builder().movieId(movieId).build());
//
//        metadata.setMasterPlaylistKey(masterPlaylistKey);
//        metadata.setBucket(bucket);
//        metadata.setEncodedQualities(qualities);
//        metadata.setStatus(StreamingStatus.READY);
//        metadataRepository.save(metadata);
//
//        // ── 2. evict stale Redis cache (re-encoding case) ─────────────────────
//        redisService.evictMovieCache(movieId);
//
//        // ── 3. warm Redis with fresh data ─────────────────────────────────────
//        redisService.saveMasterPlaylistKey(movieId, masterPlaylistKey);
//        redisService.saveQualities(movieId, qualities);
//
//        log.info("StreamingMetadata saved + Redis warmed | movieId={}", movieId);
//    }

    @Transactional(readOnly = true)
    public StreamingResponse getStreamingUrl(UUID movieId, String masterPlaylistKey) {

        // ── 1. check Redis for cached presigned URL ───────────────────────────
        var cachedUrl = redisService.getStreamingUrl(movieId);

        if (cachedUrl.isPresent()) {
            log.info("Cache HIT — streaming URL | movieId={}", movieId);
            var qualities = redisService.getQualities(movieId);
            return buildResponse(movieId, cachedUrl.get(), qualities);
        }

        // ── 2. cache MISS — generate fresh presigned URL ──────────────────────
        log.info("Cache MISS — generating presigned URL | movieId={}", movieId);

        var qualities   = redisService.getQualities(movieId);
        var presignedUrl = s3Service.generatePresignedStreamingUrl(masterPlaylistKey);

        // ── 3. cache the fresh URL for next requests ──────────────────────────
        redisService.saveStreamingUrl(movieId, presignedUrl);

        return buildResponse(movieId, presignedUrl, qualities);
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private StreamingResponse buildResponse(UUID movieId, String url, List<String> qualities) {
        return StreamingResponse.builder()
                .movieId(movieId)
                .streamingHlsMasterPlaylistUri(url)
                .qualities(qualities)
                .expiryInMinutes(s3Service.getExpiryMinutes())
                .generatedAt(Instant.now())
                .build();
    }
}
