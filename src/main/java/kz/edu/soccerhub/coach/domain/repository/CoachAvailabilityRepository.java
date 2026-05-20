package kz.edu.soccerhub.coach.domain.repository;

import kz.edu.soccerhub.coach.domain.model.CoachAvailability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CoachAvailabilityRepository extends JpaRepository<CoachAvailability, UUID> {
}
