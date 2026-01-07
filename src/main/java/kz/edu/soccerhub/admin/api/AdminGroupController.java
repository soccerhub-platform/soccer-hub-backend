package kz.edu.soccerhub.admin.api;

import jakarta.validation.Valid;
import kz.edu.soccerhub.admin.application.dto.AdminGroupCreateInput;
import kz.edu.soccerhub.admin.application.service.AdminGroupService;
import kz.edu.soccerhub.common.dto.group.GroupScheduleBatchCommand;
import kz.edu.soccerhub.organization.application.dto.CoachBusySlotView;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin/groups")
@PreAuthorize("hasAuthority('ADMIN')")
@RequiredArgsConstructor
public class AdminGroupController {

    private final AdminGroupService adminGroupService;

    /* ================= GROUP ================= */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createGroup(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid AdminGroupCreateInput input
    ) {
        UUID adminId = UUID.fromString(jwt.getSubject());

        UUID groupId = adminGroupService.createGroup(adminId, input);

        return ResponseEntity.created(URI.create("/organization/groups/" + groupId))
                .body(Map.of("groupId", groupId));
    }

    /* ================= COACH ================= */

    @PostMapping("/{groupId}/coaches/{coachId}")
    public Map<String, UUID> assignCoach(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID groupId,
            @PathVariable UUID coachId
    ) {
        UUID adminId = UUID.fromString(jwt.getSubject());

        UUID id = adminGroupService.assignCoachToGroup(
                adminId,
                groupId,
                coachId
        );

        return Map.of("groupCoachId", id);
    }

    @DeleteMapping("/{groupId}/coaches/{coachId}")
    public void unassignCoach(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID groupId,
            @PathVariable UUID coachId
    ) {
        UUID adminId = UUID.fromString(jwt.getSubject());

        adminGroupService.unassignCoachFromGroup(
                adminId,
                groupId,
                coachId
        );
    }

    /* ================= SCHEDULE ================= */

    @PostMapping("/{groupId}/schedule")
    public void createSchedule(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID groupId,
            @RequestBody GroupScheduleBatchCommand command
    ) {
        UUID adminId = UUID.fromString(jwt.getSubject());

        adminGroupService.createGroupSchedule(
                adminId,
                groupId,
                command
        );
    }

    @DeleteMapping("/schedule/{scheduleId}")
    public void cancelSchedule(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID scheduleId
    ) {
        UUID adminId = UUID.fromString(jwt.getSubject());

        adminGroupService.cancelSchedule(adminId, scheduleId);
    }

    /* ================= AVAILABILITY ================= */

    @GetMapping("/coaches/{coachId}/availability")
    public List<CoachBusySlotView> getCoachAvailability(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID coachId,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to
    ) {
        UUID adminId = UUID.fromString(jwt.getSubject());

        return adminGroupService.getCoachAvailability(
                adminId,
                coachId,
                from,
                to
        );
    }
}