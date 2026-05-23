package com.netflix.streamingservice.dto.response;

import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
public record StreamingResponse(
        UUID movieId,
        String       streamingHlsMasterPlaylistUri,  // presigned S3 URL for master.m3u8
        List<String> qualities,                      // ["1080p", "720p", "480p", "360p"] ---only quality
        int          expiryInMinutes,
        Instant generatedAt
) {}
