package com.netflix.encodingservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;

    /**
     * Downloads raw video from S3 to a local temp file.
     * Returns path to the downloaded file.
     */
    public Path downloadRawVideo(String videoKey, UUID movieId) throws IOException {
        Path tempFile = Files.createTempFile("raw_" + movieId + "_", ".mp4");

        log.info("Downloading raw video: key={}", videoKey);

        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(videoKey)
                .build();

        s3Client.getObject(getRequest, tempFile);
        log.info("Download complete: localPath={}, size={}MB",
                tempFile, Files.size(tempFile) / (1024 * 1024));

        return tempFile;
    }

    /**
     * Recursively uploads all HLS output files from workDir to S3.
     * S3 prefix: hls/{movieId}/
     *
     * Returns list of all uploaded S3 keys.
     */
    public List<String> uploadHlsOutput(Path workDir, UUID movieId) throws IOException {
        List<String> uploadedKeys = new ArrayList<>();
        String s3Prefix = "hls/" + movieId + "/";

        // walk all files in workDir — .m3u8 playlists + .ts segments
        try (var stream = Files.walk(workDir)) {
            stream.filter(Files::isRegularFile)
                    .forEach(file -> {
                        String relativePath = workDir.relativize(file).toString();
                        String s3Key        = s3Prefix + relativePath;

                        try {
                            String contentType = relativePath.endsWith(".m3u8")
                                    ? "application/vnd.apple.mpegurl"
                                    : "video/mp2t";   // .ts segments

                            s3Client.putObject(
                                    PutObjectRequest.builder()
                                            .bucket(bucket)
                                            .key(s3Key)
                                            .contentType(contentType)
                                            .build(),
                                    RequestBody.fromFile(file)
                            );

                            uploadedKeys.add(s3Key);
                            log.debug("Uploaded HLS file: {}", s3Key);

                        } catch (Exception ex) {
                            throw new RuntimeException("Failed to upload: " + s3Key, ex);
                        }
                    });
        }

        log.info("HLS upload complete: {} files uploaded to s3://{}/{}",
                uploadedKeys.size(), bucket, s3Prefix);

        return uploadedKeys;
    }
}