package kz.edu.soccerhub.auth.domain.repository;

import kz.edu.soccerhub.auth.domain.model.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepo extends JpaRepository<RefreshTokenEntity, UUID> {
    Optional<RefreshTokenEntity> findByJti(UUID jti);

    @Modifying
    @Query("""
        update RefreshTokenEntity t
            set t.revoked = true
        where t.user.id = :userId
            and t.revoked = false
    """)
    int revokeAllByUserId(@Param("userId") UUID userId);
}