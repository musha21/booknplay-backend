package lk.booknplay.entity;

import jakarta.persistence.*;
import lk.booknplay.enums.VenueStatus;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "venues")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Venue {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String city;

    @Column(name = "formatted_address")
    private String formattedAddress;

    @Column(name = "venue_type")
    private String venueType;

    private Double latitude;
    private Double longitude;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "additional_rules", columnDefinition = "TEXT")
    private String additionalRules;

    @ElementCollection
    @CollectionTable(name = "venue_rules", joinColumns = @JoinColumn(name = "venue_id"))
    @Column(name = "rule_text")
    @Builder.Default
    private java.util.List<String> rules = new java.util.ArrayList<>();

    @Column(name = "cover_image_url")
    private String coverImageUrl;

    @ElementCollection
    @CollectionTable(name = "venue_amenities", joinColumns = @JoinColumn(name = "venue_id"))
    @Column(name = "amenity")
    @Builder.Default
    private java.util.List<String> amenities = new java.util.ArrayList<>();

    @OneToMany(mappedBy = "venue", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @OrderBy("sortOrder ASC")
    private java.util.List<VenueImage> images = new java.util.ArrayList<>();

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 40)
    @Builder.Default
    private VenueStatus status = VenueStatus.DRAFT;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
