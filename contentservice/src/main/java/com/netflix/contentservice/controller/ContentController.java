package com.netflix.contentservice.controller;


import com.netflix.contentservice.dto.MovieRequest;
import com.netflix.contentservice.dto.MovieResponse;
import com.netflix.contentservice.enums.Genre;
import com.netflix.contentservice.globalResponse.ApiResponse;
import com.netflix.contentservice.service.ContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/contents")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;

    @PostMapping
    public ResponseEntity<ApiResponse<MovieResponse>> addMovie(
            @Valid @RequestBody MovieRequest request) {

        MovieResponse response = contentService.addMovie(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Movie added successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<MovieResponse>>> getAllMovies(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortBy));
        return ResponseEntity.ok(ApiResponse.success(
                contentService.getAllMovies(pageable), "Fetched all movies"));
    }

    @GetMapping("/genre/{genre}")
    public ResponseEntity<ApiResponse<Page<MovieResponse>>> getByGenre(
            @PathVariable Genre genre,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(
                contentService.getMoviesByGenre(genre, pageable), "Fetched by genre"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MovieResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                contentService.getMovieById(id), "Movie fetched"));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<MovieResponse>>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(
                contentService.searchByTitle(keyword, pageable), "Search results"));
    }
}
