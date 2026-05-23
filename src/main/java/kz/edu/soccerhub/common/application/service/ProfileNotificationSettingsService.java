package kz.edu.soccerhub.common.application.service;

import kz.edu.soccerhub.common.domain.model.ProfileNotificationSettings;
import kz.edu.soccerhub.common.domain.repository.ProfileNotificationSettingsRepository;
import kz.edu.soccerhub.common.dto.profile.NotificationSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileNotificationSettingsService {

    private final ProfileNotificationSettingsRepository repository;

    @Transactional(readOnly = true)
    public NotificationSettings get(UUID userId) {
        return repository.findById(userId)
                .map(this::toDto)
                .orElseGet(this::defaults);
    }

    @Transactional
    public NotificationSettings update(UUID userId, NotificationSettings input) {
        ProfileNotificationSettings settings = repository.findById(userId)
                .orElseGet(() -> ProfileNotificationSettings.builder().userId(userId).build());

        settings.setTodaySessions(input.todaySessions());
        settings.setOverdueReports(input.overdueReports());
        settings.setScheduleChanges(input.scheduleChanges());
        settings.setLeadReminders(input.leadReminders());
        settings.setPaymentAlerts(input.paymentAlerts());

        return toDto(repository.save(settings));
    }

    private NotificationSettings toDto(ProfileNotificationSettings settings) {
        return new NotificationSettings(
                settings.isTodaySessions(),
                settings.isOverdueReports(),
                settings.isScheduleChanges(),
                settings.isLeadReminders(),
                settings.isPaymentAlerts()
        );
    }

    private NotificationSettings defaults() {
        return new NotificationSettings(true, true, true, true, true);
    }
}
