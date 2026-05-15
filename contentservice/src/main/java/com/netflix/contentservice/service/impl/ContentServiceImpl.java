package com.netflix.contentservice.service.impl;


import com.netflix.contentservice.dto.MovieRequest;
import com.netflix.contentservice.dto.MovieResponse;
import com.netflix.contentservice.enums.Genre;
import com.netflix.contentservice.exception.ContentNotFoundException;
import com.netflix.contentservice.exception.DuplicateTitleException;
import com.netflix.contentservice.mapper.ContentMapper;
import com.netflix.contentservice.model.Content;
import com.netflix.contentservice.repository.ContentRepository;
import com.netflix.contentservice.service.ContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentServiceImpl implements ContentService {

    private final ContentRepository contentRepository;
    private final ContentMapper contentMapper;   // Spring injects MapStruct-generated impl        // see below

    @Override
    @Transactional
    public MovieResponse addMovie(MovieRequest request) {
        if (contentRepository.existsByTitleIgnoreCase(request.getTitle())) {
            throw new DuplicateTitleException(request.getTitle());
        }
        Content entity = contentMapper.toEntity(request);
        Content saved  = contentRepository.save(entity);
        log.info("Content saved: id={}, title={}", saved.getId(), saved.getTitle());
        return contentMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MovieResponse> getAllMovies(Pageable pageable) {
        return contentRepository.findAll(pageable)
                .map(contentMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MovieResponse> getMoviesByGenre(Genre genre, Pageable pageable) {
        return contentRepository.findByGenre(genre, pageable)
                .map(contentMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public MovieResponse getMovieById(UUID id) {
        return contentRepository.findById(id)
                .map(contentMapper::toResponse)
                .orElseThrow(() -> new ContentNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MovieResponse> searchByTitle(String keyword, Pageable pageable) {
        return contentRepository.searchByTitle(keyword, pageable)
                .map(contentMapper::toResponse);
    }
}
