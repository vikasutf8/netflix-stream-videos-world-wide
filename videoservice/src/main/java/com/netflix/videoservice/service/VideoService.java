package com.netflix.videoservice.service;

import org.springframework.web.multipart.MultipartFile;

public interface VideoService {

    String uploadVideo(String videoId, MultipartFile videoData);
}
