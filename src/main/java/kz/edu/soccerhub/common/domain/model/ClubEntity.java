package kz.edu.soccerhub.common.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "clubs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClubEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    private String email;
    private String phone;
    private String website;

    @Column(name = "logo_url")
    private String logoUrl;

    private String address;

    private String timezone;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}