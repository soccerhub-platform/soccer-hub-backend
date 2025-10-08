package kz.edu.soccerhub.auth.domain.repository;

import kz.edu.soccerhub.auth.domain.model.AppUserEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppUserRepo extends JpaRepository<AppUserEntity, UUID> {
    @EntityGraph(attributePaths = "roles")
    Optional<AppUserEntity> findByEmail(String email);
}