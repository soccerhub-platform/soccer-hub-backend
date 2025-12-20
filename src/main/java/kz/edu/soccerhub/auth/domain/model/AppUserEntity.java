package kz.edu.soccerhub.auth.domain.model;

import jakarta.persistence.*;
import kz.edu.soccerhub.common.domain.model.AbstractAuditableEntity;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

// AppUser.java
@Entity
@Table(name="app_user")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AppUserEntity extends AbstractAuditableEntity {
    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, unique = true, length=100)
    private String email;

    @Column(name="password_hash", nullable=false)
    private String passwordHash;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "force_password_change", nullable = false)
    private boolean forcePasswordChange = false;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "app_user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_code"))
    private Set<AppRoleEntity> roles = new HashSet<>();
}