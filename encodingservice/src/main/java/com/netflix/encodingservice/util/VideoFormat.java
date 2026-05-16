package com.netflix.encodingservice.util;

/**
 * Immutable value object — defines one encoding target.
 * resolution : FFmpeg scale filter value  e.g. "1280x720"
 * bitrate    : video bitrate              e.g. "2M"
 * label      : human name                e.g. "720p"
 */
public record VideoFormat(String label, String resolution, String bitrate) {}
