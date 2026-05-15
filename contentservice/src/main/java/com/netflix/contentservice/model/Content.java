package com.netflix.contentservice.model;




import com.netflix.contentservice.enums.Genre;
import com.netflix.contentservice.enums.VideoStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;



@Entity
@Table(
        name = "contents",
        indexes = {
                @Index(name = "idx_content_video_status", columnList = "video_status"),
                @Index(name = "idx_content_releasing_year", columnList = "releasing_year"),
                @Index(name = "idx_content_created_at",    columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Content {

    // ── Identity ──────────────────────────────────────────────────────────────

    @Id
    @UuidGenerator                          // Hibernate 6 — no trigger needed
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    // ── Core Metadata ─────────────────────────────────────────────────────────

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "discription", nullable = false, length = 255)
    private String discription;

    @Enumerated(EnumType.STRING)            // store "ACTION" not "0" — survives enum reorders
    @Column(name = "genre", nullable = false, length = 50)
    private Genre genre;

    @Column(name = "director", length = 150)
    private String director;

    /**
     * Stored as a comma-separated string for simplicity at this scale.
     * For full cast search / normalisation → extract to a separate `cast_members` table.
     */
    @Column(name = "cast", columnDefinition = "TEXT")
    private String cast;

    @Column(name = "releasing_year", nullable = false)
    private Integer releasingYear;

    /**
     * 0.0 – 10.0  (e.g. 8.4)
     * BigDecimal avoids float precision bugs in a rating-sorted feed.
     */
    @Column(name = "rating", precision = 3, scale = 1)
    private BigDecimal rating;

    // ── Media URIs ────────────────────────────────────────────────────────────

    /**
     * CDN-relative path or full URI of the thumbnail image.
     * Keep <= 2083 chars (IE URL limit, safe for all CDNs).
     */
    @Column(name = "thumbnail_uri", length = 2083)
    private String thumbnailUri;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    /**
     * S3 / GCS object key of the raw uploaded video.
     * Never expose this externally — it's internal to the transcoding pipeline.
     */
    @Column(name = "video_key", length = 1024)
    private String videoKey;

    /**
     * Base URI of the HLS manifest (.m3u8).
     * Populated by the transcoding worker once VideoStatus = READY.
     */
    @Column(name = "hls_uri", length = 2083)
    private String hlsUri;

    // ── Status ────────────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "video_status", nullable = false, length = 30)
    @Builder.Default
    private VideoStatus videoStatus = VideoStatus.PENDING;

    // ── Audit ─────────────────────────────────────────────────────────────────

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}