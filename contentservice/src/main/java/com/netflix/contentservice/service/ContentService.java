package com.netflix.contentservice.service;

import com.netflix.contentservice.dto.MovieRequest;
import com.netflix.contentservice.dto.MovieResponse;
import com.netflix.contentservice.enums.Genre;
import org.hibernate.validator.constraints.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ContentService {

    MovieResponse addMovie(MovieRequest request);

    Page<MovieResponse> getAllMovies(Pageable pageable);

    Page<MovieResponse> getMoviesByGenre(Genre genre, Pageable pageable);

    MovieResponse getMovieById(UUID id);

    Page<MovieResponse> searchByTitle(String keyword, Pageable pageable);
}
