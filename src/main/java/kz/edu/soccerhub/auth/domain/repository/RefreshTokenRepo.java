package kz.edu.soccerhub.auth.domain.repository;

import kz.edu.soccerhub.auth.domain.model.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepo extends JpaRepository<RefreshTokenEntity, UUID> {
    Optional<RefreshTokenEntity> findByJti(UUID jti);
}