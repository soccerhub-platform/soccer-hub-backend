package kz.edu.soccerhub.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

// AppUser.java
@Entity
@Table(name="app_user")
@Getter
@Setter
public class AppUser  {
    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable=false, unique=true, length=100)
    private String email;

    @Column(name="password_hash", nullable=false)
    private String passwordHash;

    @Column(nullable=false)
    private boolean enabled = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "app_user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_code"))
    private Set<AppRole> roles = new HashSet<>();
}