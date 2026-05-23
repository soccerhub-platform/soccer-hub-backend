package kz.edu.soccerhub.admin.api;

import jakarta.validation.Valid;
import kz.edu.soccerhub.admin.application.service.AdminProfileService;
import kz.edu.soccerhub.common.application.service.ProfileNotificationSettingsService;
import kz.edu.soccerhub.common.dto.profile.AdminProfileOutput;
import kz.edu.soccerhub.common.dto.profile.NotificationSettings;
import kz.edu.soccerhub.common.dto.profile.ProfileUpdateInput;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/admin/profile")
@PreAuthorize("hasAuthority('ADMIN')")
@RequiredArgsConstructor
public class AdminProfileController {

    private final AdminProfileService adminProfileService;
    private final ProfileNotificationSettingsService notificationSettingsService;

    @GetMapping
    public AdminProfileOutput getProfile(@AuthenticationPrincipal Jwt jwt) {
        return adminProfileService.getProfile(getUserId(jwt));
    }

    @PatchMapping
    public AdminProfileOutput updateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ProfileUpdateInput input
    ) {
        return adminProfileService.updateProfile(getUserId(jwt), input);
    }

    @GetMapping("/notification-settings")
    public NotificationSettings getNotificationSettings(@AuthenticationPrincipal Jwt jwt) {
        return notificationSettingsService.get(getUserId(jwt));
    }

    @PutMapping("/notification-settings")
    public NotificationSettings updateNotificationSettings(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody NotificationSettings input
    ) {
        return notificationSettingsService.update(getUserId(jwt), input);
    }

    private UUID getUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
