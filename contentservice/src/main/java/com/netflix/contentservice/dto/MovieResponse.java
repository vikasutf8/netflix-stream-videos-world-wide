package com.netflix.contentservice.dto;

import com.netflix.contentservice.enums.Genre;
import com.netflix.contentservice.enums.VideoStatus;
import lombok.*;
import org.hibernate.validator.constraints.UUID;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovieResponse {

    private String id;
    private String title;
    private Genre genre;
    private String director;
    private String cast;
    private Integer releasingYear;
    private BigDecimal rating;
    private String thumbnailUri;
    private Integer durationMinutes;
    private String hlsUri;          // videoKey intentionally excluded — internal only
    private VideoStatus videoStatus;
    private Instant createdAt;
    private Instant updatedAt;
}
