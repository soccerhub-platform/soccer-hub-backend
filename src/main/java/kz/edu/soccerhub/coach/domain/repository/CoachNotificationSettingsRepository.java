package kz.edu.soccerhub.coach.domain.repository;

import kz.edu.soccerhub.coach.domain.model.CoachNotificationSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CoachNotificationSettingsRepository extends JpaRepository<CoachNotificationSettings, UUID> {
}
