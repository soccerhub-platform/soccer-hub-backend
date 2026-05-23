package kz.edu.soccerhub.dispatcher.api;

import jakarta.validation.Valid;
import kz.edu.soccerhub.common.application.service.ProfileNotificationSettingsService;
import kz.edu.soccerhub.common.dto.profile.DispatcherProfileOutput;
import kz.edu.soccerhub.common.dto.profile.NotificationSettings;
import kz.edu.soccerhub.common.dto.profile.ProfileUpdateInput;
import kz.edu.soccerhub.dispatcher.application.service.DispatcherProfileService;
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
@RequestMapping("/dispatcher/profile")
@PreAuthorize("hasAuthority('DISPATCHER')")
@RequiredArgsConstructor
public class DispatcherProfileController {

    private final DispatcherProfileService dispatcherProfileService;
    private final ProfileNotificationSettingsService notificationSettingsService;

    @GetMapping
    public DispatcherProfileOutput getProfile(@AuthenticationPrincipal Jwt jwt) {
        return dispatcherProfileService.getProfile(getUserId(jwt));
    }

    @PatchMapping
    public DispatcherProfileOutput updateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ProfileUpdateInput input
    ) {
        return dispatcherProfileService.updateProfile(getUserId(jwt), input);
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
