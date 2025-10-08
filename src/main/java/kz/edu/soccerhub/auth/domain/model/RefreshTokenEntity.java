package kz.edu.soccerhub.auth.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

// RefreshToken.java
@Entity
@Table(name="refresh_token")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RefreshTokenEntity {

  @Id
  @Column(columnDefinition="uuid")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private AppUserEntity user;

  @Column(columnDefinition = "uuid", nullable=false)
  private UUID jti;

  @Column(name = "token_hash", nullable=false)
  private String tokenHash; // store hash, never raw token

  @Column(nullable = false)
  private LocalDateTime issuedAt = LocalDateTime.now();

  @Column(nullable = false)
  private LocalDateTime expiresAt;

  @Column(nullable = false)
  private boolean revoked = false;

  // optional chaining
  @Column(name = "replaced_by_jty")
  private UUID replacedByJty;

  @Column(name = "user_agent")
  private String userAgent;
}