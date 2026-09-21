package lk.booknplay.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "business_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Column(nullable = false)
    private String url;

    @Builder.Default
    @Column(name = "is_logo", nullable = false)
    private boolean logo = false;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;
}
