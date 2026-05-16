package com.netflix.encodingservice.service;

import com.netflix.encodingservice.event.VideoEncodedEvent;
import com.netflix.encodingservice.util.VideoFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.codec.EncodingException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class EncodingService {

    @Value("${ffmpeg.path}")
    private String ffmpegPath;
// bitrate -- how much data is processed/ second --- high bitrate --better visual quality- high internet
    private static final List<VideoFormat> TARGET_FORMATS = List.of(
            new VideoFormat("1080p", "1920x1080", "5M"),
            new VideoFormat("720p",  "1280x720",  "2M"),
            new VideoFormat("480p",  "854x480",   "1M"),
            new VideoFormat("360p",  "640x360",   "500k")
    );

    /**
     * Encodes raw video into HLS variants.
     * Returns path to the temp working directory containing all output files.
     *
     * workDir layout after encoding:
     *   /tmp/{jobId}/
     *     720p/
     *       playlist.m3u8
     *       segment_000.ts
     *       segment_001.ts  ...
     *     480p/ ...
     *     360p/ ...
     *     master.m3u8
     */
    public Path encodeToHls(Path rawVideoPath, UUID movieId) {
        String jobId   = movieId + "_" + UUID.randomUUID();
        Path   workDir = Path.of(System.getProperty("java.io.tmpdir"), jobId);

        try {
            Files.createDirectories(workDir);
            log.info("Encoding job started: jobId={}, input={}", jobId, rawVideoPath);
//IMPORTANT : LOOPING
            // ── encode each variant ───────────────────────────────────────────
            for (VideoFormat fmt : TARGET_FORMATS) {
                encodeVariant(rawVideoPath, workDir, fmt);
            }

            // ── generate master playlist ──────────────────────────────────────
            generateMasterPlaylist(workDir);

            log.info("Encoding complete: jobId={}", jobId);
            return workDir;

        } catch (IOException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new EncodingException("Encoding failed for movieId: " + movieId, ex);
        }
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private void encodeVariant(Path input, Path workDir, VideoFormat fmt)
            throws IOException, InterruptedException {

        Path variantDir = workDir.resolve(fmt.label());
        Files.createDirectories(variantDir);

        // FFmpeg command — encode to HLS segments
        List<String> cmd = List.of(
                ffmpegPath,
                "-i",         input.toString(),
                "-vf",        "scale=" + fmt.resolution(),
                "-b:v",       fmt.bitrate(),
                "-c:v",       "libx264",
                "-c:a",       "aac",
                "-hls_time",  "6",                                    // 6-second segments
                "-hls_playlist_type", "vod",
                "-hls_segment_filename", variantDir.resolve("segment_%03d.ts").toString(),
                variantDir.resolve("playlist.m3u8").toString()
        );

        log.info("FFmpeg encoding [{}]: {}", fmt.label(), String.join(" ", cmd));

        Process process = new ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .start();

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            String output = new String(process.getInputStream().readAllBytes());
            throw new EncodingException(
                    "FFmpeg failed for variant " + fmt.label() + ": " + output);
        }

        log.info("Variant encoded successfully: {}", fmt.label());
    }

    private void generateMasterPlaylist(Path workDir) throws IOException {
        StringBuilder master = new StringBuilder("#EXTM3U\n#EXT-X-VERSION:3\n\n");

        for (VideoFormat fmt : TARGET_FORMATS) {
            Path variantPlaylist = workDir.resolve(fmt.label()).resolve("playlist.m3u8");
            if (Files.exists(variantPlaylist)) {
                master.append("#EXT-X-STREAM-INF:BANDWIDTH=")
                        .append(parseBandwidth(fmt.bitrate()))
                        .append(",RESOLUTION=").append(fmt.resolution())
                        .append("\n")
                        .append(fmt.label()).append("/playlist.m3u8\n\n");
            }
        }

        Files.writeString(workDir.resolve("master.m3u8"), master.toString());
        log.info("Master playlist generated at: {}", workDir.resolve("master.m3u8"));
    }

    /** "2M" → 2000000,  "500k" → 500000 */
    private int parseBandwidth(String bitrate) {
        if (bitrate.endsWith("M"))
            return Integer.parseInt(bitrate.replace("M", "")) * 1_000_000;
        if (bitrate.endsWith("k"))
            return Integer.parseInt(bitrate.replace("k", "")) * 1_000;
        return Integer.parseInt(bitrate);
    }




    public List<VideoFormat> getTargetFormats() {
        return TARGET_FORMATS;
    }
}
