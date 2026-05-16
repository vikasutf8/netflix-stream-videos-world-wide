package com.netflix.encodingservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoEncodedEvent {
    private UUID         eventId;
    private UUID         movieId;
    private String       hlsUri;                // master playlist URI (e.g. https://s3.amazonaws.com/bucket/encoded/movie123/master.m3u8)
    private String       masterPlaylistKey;  // S3 key of master.m3u8
    private List<String> variantKeys;        // S3 keys of all .ts chunks + variant playlists
    private List<String> encodedQualities;   // ["720p", "480p", "360p"]
    private String       bucket;
    private Instant      encodedAt;
    private EncodingStatus status;// SUCCESS / FAILED
    private String errorMessage;

    public enum EncodingStatus { SUCCESS, FAILED }
}