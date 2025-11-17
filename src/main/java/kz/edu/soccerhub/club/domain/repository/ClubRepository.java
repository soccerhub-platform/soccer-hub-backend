package kz.edu.soccerhub.club.domain.repository;

import kz.edu.soccerhub.club.domain.model.ClubEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ClubRepository extends JpaRepository<ClubEntity, UUID> {
}
