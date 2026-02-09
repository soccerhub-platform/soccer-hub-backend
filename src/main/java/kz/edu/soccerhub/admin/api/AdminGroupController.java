package kz.edu.soccerhub.admin.api;

import jakarta.validation.Valid;
import kz.edu.soccerhub.admin.application.dto.group.*;
import kz.edu.soccerhub.admin.application.service.AdminGroupService;
import kz.edu.soccerhub.common.dto.group.GroupScheduleBatchCommand;
import kz.edu.soccerhub.common.dto.group.UpdateScheduleBatchCommand;
import kz.edu.soccerhub.organization.application.dto.CoachBusySlotView;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.Collection;
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


    @PatchMapping("/{groupId}/status")
    public ResponseEntity<Void> updateStatusOfGroup(@AuthenticationPrincipal Jwt jwt,
                                                    @PathVariable("groupId") UUID groupId,
                                                    @RequestBody AdminGroupStatusChangeInput request) {
        UUID adminId = UUID.fromString(jwt.getSubject());

        adminGroupService.updateGroupStatus(adminId, groupId, request.status());

        return ResponseEntity.noContent().build();
    }

    /* ================= COACH ================= */

    @PostMapping("/{groupId}/coaches")
    public Map<String, UUID> assignCoach(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID groupId,
            @RequestBody AdminAssignCoachToGroupInput input
    ) {
        UUID adminId = UUID.fromString(jwt.getSubject());

        UUID id = adminGroupService.assignCoachToGroup(
                adminId,
                groupId,
                input.coachId(),
                input.role()
        );

        return Map.of("groupCoachId", id);
    }

    @DeleteMapping("/coaches/{groupCoachId}")
    public void unassignCoach(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID groupCoachId
    ) {
        UUID adminId = UUID.fromString(jwt.getSubject());

        adminGroupService.unassignCoachFromGroup(
                adminId,
                groupCoachId
        );
    }

    @GetMapping("/{groupId}/coaches")
    public ResponseEntity<Map<String, Object>> getGroupCoaches(@PathVariable UUID groupId) {
        Collection<AdminGroupCoachOutput> coaches = adminGroupService.getGroupCoaches(groupId);
        return ResponseEntity.ok(Map.of(
                "groupId", groupId,
                "coaches", coaches
        ));
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

    @PatchMapping("/schedule/{scheduleId}/cancel")
    public void cancelSchedule(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID scheduleId
    ) {
        UUID adminId = UUID.fromString(jwt.getSubject());

        adminGroupService.cancelSchedule(adminId, scheduleId);
    }

    @PatchMapping("/schedule/{scheduleId}/activate")
    public void activateSchedule(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID scheduleId
    ) {
        UUID adminId = UUID.fromString(jwt.getSubject());

        adminGroupService.activateSchedule(adminId, scheduleId);
    }

    @DeleteMapping("/{groupId}/schedule")
    public void cancelGroupScheduleBatch(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID groupId,
            @Valid @RequestBody AdminCancelScheduleBatchInput input
    )  {

        UUID adminId = UUID.fromString(jwt.getSubject());

        adminGroupService.cancelScheduleBatch(adminId, groupId, input);
    }

    @PutMapping("/{groupId}/schedule")
    public ResponseEntity<Void> updateSchedule(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID groupId,
            @Valid @RequestBody UpdateScheduleBatchCommand command
    ) {
        UUID adminId = UUID.fromString(jwt.getSubject());

        adminGroupService.updateGroupSchedule(adminId, groupId, command);
        return ResponseEntity.noContent().build();
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