package kz.edu.soccerhub.repository;

import kz.edu.soccerhub.model.AppUser;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppUserRepo extends JpaRepository<AppUser, UUID> {
  @EntityGraph(attributePaths = "roles")
  Optional<AppUser> findByEmail(String email);
}