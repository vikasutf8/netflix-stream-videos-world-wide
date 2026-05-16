package com.netflix.videoservice.controller;


import com.netflix.videoservice.globalResponse.ApiResponse;
import com.netflix.videoservice.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/video")
@RequiredArgsConstructor
@Slf4j
public class VideoController {

    private final VideoService videoService;

    /**
     * FLOW 1 — Direct upload (server buffers file → S3)
     * Use for small/medium files or internal admin tools.
     *
     * POST /api/v1/video/{movieId}/upload
     * Content-Type: multipart/form-data
     * Body: file = <video file>
     */
    @PostMapping(
            value    = "/{movieId}/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<String>> uploadVideo(
            @PathVariable String  movieId,
            @RequestParam("file") MultipartFile file) {
        log.info("Received video upload request: movieId={}, filename={}, size={} bytes",
                movieId, file.getOriginalFilename(), file.getSize());
//        VideoUploadResponse response = videoService.uploadVideo(movieId, file);
        String videoKey= videoService.uploadVideo(movieId, file);
        return ResponseEntity.status(HttpStatus.ACCEPTED)          // 202 — async transcoding starts
                .body(ApiResponse.success(videoKey, "Video uploaded. Encoding starting via streaming of kafka."));
    }

}
