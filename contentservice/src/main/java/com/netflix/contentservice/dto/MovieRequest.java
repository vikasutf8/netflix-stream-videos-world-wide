package com.netflix.contentservice.dto;

import com.netflix.contentservice.enums.Genre;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovieRequest {


    @NotBlank()
    private String title;
    private String discription;
    @NotBlank()
    private Genre genre;
    private String director;
    private String thumbnailUri;
    private BigDecimal rating;
    private Integer releasingYear;
    private String cast;
    private Integer durationMinutes;
}
