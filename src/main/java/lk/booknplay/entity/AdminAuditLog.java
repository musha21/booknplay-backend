package lk.booknplay.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "admin_audit_logs")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AdminAuditLog {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @Column(name = "admin_email", nullable = false)
    private String adminEmail;
    @Column(nullable = false)
    private String action;
    @Column(name = "resource_type", nullable = false)
    private String resourceType;
    @Column(name = "resource_id")
    private String resourceId;
    @Column(columnDefinition = "TEXT")
    private String reason;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}
