package kz.edu.soccerhub.coach.api;

import jakarta.validation.Valid;
import kz.edu.soccerhub.coach.application.dto.profile.*;
import kz.edu.soccerhub.coach.application.service.CoachProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/coach/profile")
@PreAuthorize("hasAuthority('COACH')")
@RequiredArgsConstructor
public class CoachProfileController {

    private final CoachProfileService coachProfileService;

    @GetMapping
    public CoachProfileResponse getProfile(@AuthenticationPrincipal Jwt jwt) {
        return coachProfileService.getProfile(getCurrentUserId(jwt));
    }

    @PatchMapping
    public CoachProfileResponse updateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CoachProfileUpdateRequest request
    ) {
        return coachProfileService.updateProfile(getCurrentUserId(jwt), request);
    }

    @GetMapping("/availability")
    public CoachAvailabilityResponse getAvailability(@AuthenticationPrincipal Jwt jwt) {
        return coachProfileService.getAvailability(getCurrentUserId(jwt));
    }

    @PutMapping("/availability")
    public CoachAvailabilityResponse updateAvailability(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CoachAvailabilityUpdateRequest request
    ) {
        return coachProfileService.updateAvailability(getCurrentUserId(jwt), request);
    }

    @GetMapping("/notification-settings")
    public CoachNotificationSettingsResponse getNotificationSettings(@AuthenticationPrincipal Jwt jwt) {
        return coachProfileService.getNotificationSettings(getCurrentUserId(jwt));
    }

    @PutMapping("/notification-settings")
    public CoachNotificationSettingsResponse updateNotificationSettings(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody CoachNotificationSettingsUpdateRequest request
    ) {
        return coachProfileService.updateNotificationSettings(getCurrentUserId(jwt), request);
    }

    private UUID getCurrentUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
