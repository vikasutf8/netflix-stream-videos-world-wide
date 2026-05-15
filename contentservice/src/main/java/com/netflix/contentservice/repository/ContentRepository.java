package com.netflix.contentservice.repository;

import com.netflix.contentservice.enums.Genre;
import com.netflix.contentservice.model.Content;
import org.hibernate.validator.constraints.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContentRepository extends JpaRepository<Content, UUID> {

    boolean existsByTitleIgnoreCase(String title);

    Page<Content> findByGenre(Genre genre, Pageable pageable);

    @Query("""
            SELECT c FROM Content c
            WHERE LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
            """)
    Page<Content> searchByTitle(@Param("keyword") String keyword, Pageable pageable);
}
