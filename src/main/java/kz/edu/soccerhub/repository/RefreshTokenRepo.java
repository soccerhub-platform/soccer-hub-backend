package kz.edu.soccerhub.repository;

import kz.edu.soccerhub.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepo extends JpaRepository<RefreshToken, UUID> {
  Optional<RefreshToken> findByJti(UUID jti);
}