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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class StreamingService {
    private final StreamingMetadataRepository metadataRepository;
    private final RedisService                redisService; // redis checking again present nopt presinge url
    private final S3Service                   s3Service; //using for create  new  presigned url
    private static final Pattern URI_ATTRIBUTE_PATTERN = Pattern.compile("URI=\"([^\"]+)\"");

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
        var presignedUrl = s3Service.generatePresignedStreamingUrl(masterPlaylistKey); // TODO::: wrong

        // ── 3. cache the fresh URL for next requests ──────────────────────────
        redisService.saveStreamingUrl(movieId, presignedUrl);

        return buildResponse(movieId, presignedUrl, qualities);
    }

    @Transactional(readOnly = true)
    public String getSignedPlaylist(UUID movieId, String playlistPath) {
        String masterPlaylistKey = redisService.getMasterPlaylistKey(movieId)
                .orElseThrow(() -> new IllegalStateException(
                        "Video is still processing or not found: " + movieId));

        String movieBasePath = extractBasePath(masterPlaylistKey);
        String scopedPlaylistPath = resolvePlaylistPathWithinMovie(movieBasePath, playlistPath);

        var cachedSignedPlaylist = redisService.getSignedPlaylist(movieId, scopedPlaylistPath);
        if (cachedSignedPlaylist.isPresent()) {
            log.info("Cache HIT — signed playlist | movieId={} path={}", movieId, scopedPlaylistPath);
            return cachedSignedPlaylist.get();
        }

        log.info("Cache MISS — generating signed playlist | movieId={} path={}",
                movieId, scopedPlaylistPath);

        String playlistContent = s3Service.readObjectAsString(scopedPlaylistPath);
        String playlistBasePath = extractBasePath(scopedPlaylistPath);

        String signedPlaylistContent = rewritePlaylistWithSignedUrls(
                playlistContent, playlistBasePath, movieBasePath);

        redisService.saveSignedPlaylist(movieId, scopedPlaylistPath, signedPlaylistContent);
        return signedPlaylistContent;
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private String resolvePlaylistPathWithinMovie(String movieBasePath, String playlistPath) {
        if (playlistPath == null || playlistPath.isBlank()) {
            throw new IllegalArgumentException("path query param is required");
        }

        String normalizedPath = playlistPath.trim();
        if (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }
        if (normalizedPath.contains("..")) {
            throw new IllegalArgumentException("Invalid playlist path");
        }
        if (isAbsoluteUri(normalizedPath)) {
            throw new IllegalArgumentException("Absolute URLs are not allowed for path");
        }

        String scopedPath = normalizedPath.startsWith(movieBasePath)
                ? normalizedPath
                : movieBasePath + normalizedPath;

        return ensureWithinMovieScope(scopedPath, movieBasePath);
    }

    private String rewritePlaylistWithSignedUrls(
            String playlistContent,
            String basePath,
            String movieBasePath) {

        String[] lines = playlistContent.split("\\r?\\n", -1);
        StringBuilder rewritten = new StringBuilder(playlistContent.length() + 128);

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmedLine = line.trim();

            if (trimmedLine.isBlank()) {
                rewritten.append(line);
            } else if (trimmedLine.startsWith("#")) {
                rewritten.append(rewriteTagLineWithSignedUri(line, basePath, movieBasePath));
            } else {
                rewritten.append(signPlaylistReference(trimmedLine, basePath, movieBasePath));
            }

            if (i < lines.length - 1) {
                rewritten.append('\n');
            }
        }
        return rewritten.toString();
    }

    private String rewriteTagLineWithSignedUri(String line, String basePath, String movieBasePath) {
        Matcher matcher = URI_ATTRIBUTE_PATTERN.matcher(line);
        if (!matcher.find()) {
            return line;
        }

        String uriReference = matcher.group(1);
        if (uriReference == null
                || uriReference.isBlank()
                || isAbsoluteUri(uriReference)
                || uriReference.startsWith("data:")) {
            return line;
        }

        String signedUri = signPlaylistReference(uriReference, basePath, movieBasePath);
        return matcher.replaceFirst("URI=\"" + Matcher.quoteReplacement(signedUri) + "\"");
    }

    private String signPlaylistReference(String reference, String basePath, String movieBasePath) {
        String normalizedReference = reference.trim();
        if (isAbsoluteUri(normalizedReference)) {
            return normalizedReference;
        }

        if (normalizedReference.startsWith("/")) {
            normalizedReference = normalizedReference.substring(1);
        }
        if (normalizedReference.contains("..")) {
            throw new IllegalArgumentException("Invalid playlist reference");
        }

        String objectKey = normalizedReference.startsWith(movieBasePath)
                ? normalizedReference
                : basePath + normalizedReference;

        objectKey = ensureWithinMovieScope(objectKey, movieBasePath);
        return s3Service.generatePresignedObjectUrl(objectKey);
    }

    private String ensureWithinMovieScope(String objectKey, String movieBasePath) {
        if (!movieBasePath.isBlank() && !objectKey.startsWith(movieBasePath)) {
            throw new IllegalArgumentException("Playlist path is outside allowed movie scope");
        }
        return objectKey;
    }

    private String extractBasePath(String path) {
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(0, lastSlash + 1) : "";
    }

    private boolean isAbsoluteUri(String value) {
        return value.startsWith("http://") || value.startsWith("https://");
    }

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
