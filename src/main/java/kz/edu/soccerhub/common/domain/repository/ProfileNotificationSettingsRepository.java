package kz.edu.soccerhub.common.domain.repository;

import kz.edu.soccerhub.common.domain.model.ProfileNotificationSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProfileNotificationSettingsRepository extends JpaRepository<ProfileNotificationSettings, UUID> {
}
