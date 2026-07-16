package kz.edu.soccerhub.admin.api;

import jakarta.validation.Valid;
import kz.edu.soccerhub.admin.application.dto.group.*;
import kz.edu.soccerhub.common.dto.media.MediaAssetResponse;
import kz.edu.soccerhub.common.dto.media.MediaDownloadUrlResponse;
import kz.edu.soccerhub.common.dto.group.GroupScheduleValidationCommand;
import kz.edu.soccerhub.common.dto.group.ScheduleValidationResult;
import kz.edu.soccerhub.admin.application.service.AdminGroupActivityService;
import kz.edu.soccerhub.admin.application.service.AdminGroupService;
import kz.edu.soccerhub.admin.application.service.AdminGroupMembershipService;
import kz.edu.soccerhub.common.dto.group.GroupScheduleBatchCommand;
import kz.edu.soccerhub.common.dto.group.UpdateScheduleBatchCommand;
import kz.edu.soccerhub.common.dto.lead.AvailableSlotOutput;
import kz.edu.soccerhub.organization.application.dto.CoachBusySlotView;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    private final AdminGroupMembershipService adminGroupMembershipService;
    private final AdminGroupActivityService adminGroupActivityService;

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

    @GetMapping("/overview")
    public ResponseEntity<AdminGroupOverviewOutput> getOverview(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam UUID branchId
    ) {
        UUID adminId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(adminGroupService.getGroupsOverview(adminId, branchId));
    }

    @GetMapping("/{groupId}/health")
    public ResponseEntity<AdminGroupHealthOutput> getGroupHealth(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID groupId
    ) {
        UUID adminId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(adminGroupService.getGroupHealth(adminId, groupId));
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<AdminGroupDetailsOutput> getGroupDetails(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID groupId
    ) {
        UUID adminId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(adminGroupService.getGroupDetails(adminId, groupId));
    }

    @PostMapping("/{groupId}/avatar")
    public ResponseEntity<MediaAssetResponse> uploadGroupAvatar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID groupId,
            @RequestPart("file") MultipartFile file
    ) {
        UUID adminId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(adminGroupService.uploadGroupAvatar(adminId, groupId, file));
    }

    @DeleteMapping("/{groupId}/avatar")
    public ResponseEntity<Void> deleteGroupAvatar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID groupId
    ) {
        UUID adminId = UUID.fromString(jwt.getSubject());
        adminGroupService.deleteGroupAvatar(adminId, groupId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{groupId}/avatar/download-url")
    public ResponseEntity<MediaDownloadUrlResponse> getGroupAvatarDownloadUrl(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID groupId
    ) {
        UUID adminId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(adminGroupService.getGroupAvatarDownloadUrl(adminId, groupId));
    }

    @GetMapping("/{groupId}/members")
    public ResponseEntity<Page<AdminGroupMemberOutput>> getGroupMembers(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID groupId,
            @PageableDefault Pageable pageable
    ) {
        UUID adminId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(adminGroupService.getGroupMembers(adminId, groupId, pageable));
    }

    @GetMapping("/{groupId}/member-candidates")
    public ResponseEntity<AdminGroupMemberCandidatesOutput> getGroupMemberCandidates(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID groupId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        UUID adminId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(adminGroupMembershipService.getMemberCandidates(adminId, groupId, search, page, size));
    }

    @GetMapping("/{groupId}/activity")
    public ResponseEntity<Page<AdminGroupActivityOutput>> getGroupActivity(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID groupId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        UUID adminId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(adminGroupActivityService.getGroupActivity(adminId, groupId, pageable));
    }

    @PostMapping("/{groupId}/members")
    public ResponseEntity<AdminGroupMembershipOutput> addGroupMember(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID groupId,
            @RequestBody @Valid AdminAddGroupMemberInput input
    ) {
        UUID adminId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(adminGroupMembershipService.addMember(adminId, groupId, input));
    }

    @PostMapping("/group-memberships/{membershipId}/transfer")
    public ResponseEntity<AdminGroupMembershipTransferOutput> transferGroupMember(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID membershipId,
            @RequestBody @Valid AdminTransferGroupMembershipInput input
    ) {
        UUID adminId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(adminGroupMembershipService.transferMember(adminId, membershipId, input));
    }

    @PostMapping("/group-memberships/{membershipId}/remove")
    public ResponseEntity<AdminGroupMembershipOutput> removeGroupMember(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID membershipId,
            @RequestBody @Valid AdminRemoveGroupMembershipInput input
    ) {
        UUID adminId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(adminGroupMembershipService.removeMember(adminId, membershipId, input));
    }

    @GetMapping("/{groupId}/schedule/risks")
    public ResponseEntity<AdminGroupScheduleRiskOutput> getScheduleRisks(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID groupId
    ) {
        UUID adminId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(adminGroupService.getScheduleRisks(adminId, groupId));
    }


    @PatchMapping("/{groupId}/status")
    public ResponseEntity<Void> updateStatusOfGroup(@AuthenticationPrincipal Jwt jwt,
                                                    @PathVariable("groupId") UUID groupId,
                                                    @RequestBody AdminGroupStatusChangeInput request) {
        UUID adminId = UUID.fromString(jwt.getSubject());

        adminGroupService.updateGroupStatus(adminId, groupId, request.status());

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{groupId}")
    public ResponseEntity<Void> updateGroup(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID groupId,
            @RequestBody @Valid AdminGroupUpdateInput input
    ) {
        UUID adminId = UUID.fromString(jwt.getSubject());
        adminGroupService.updateGroup(adminId, groupId, input);
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
                input.role(),
                input.assignedFrom(),
                input.assignedTo()
        );

        return Map.of("groupCoachId", id);
    }

    @GetMapping("/coaches/{groupCoachId}/removal-preview")
    public ResponseEntity<AdminGroupCoachRemovalPreviewOutput> previewCoachRemoval(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID groupCoachId
    ) {
        return ResponseEntity.ok(adminGroupService.previewCoachRemoval(UUID.fromString(jwt.getSubject()), groupCoachId));
    }

    @PostMapping("/coaches/{groupCoachId}/remove")
    public ResponseEntity<Void> removeCoach(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID groupCoachId,
            @RequestBody @Valid AdminRemoveGroupCoachInput input
    ) {
        adminGroupService.removeCoachFromGroup(UUID.fromString(jwt.getSubject()), groupCoachId, input);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/coaches/{groupCoachId}")
    public ResponseEntity<Void> unassignCoachWithoutReplacement(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID groupCoachId
    ) {
        adminGroupService.removeCoachFromGroup(
                UUID.fromString(jwt.getSubject()),
                groupCoachId,
                new AdminRemoveGroupCoachInput(null, LocalDate.now(), "Снятие тренера")
        );
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/coaches/{groupCoachId}")
    public ResponseEntity<Void> updateCoachRole(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID groupCoachId,
            @RequestBody @Valid AdminUpdateGroupCoachInput input
    ) {
        adminGroupService.updateGroupCoachRole(UUID.fromString(jwt.getSubject()), groupCoachId, input.role());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{groupId}/coaches")
    public ResponseEntity<Map<String, Object>> getGroupCoaches(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID groupId
    ) {
        UUID adminId = UUID.fromString(jwt.getSubject());
        Collection<AdminGroupCoachOutput> coaches = adminGroupService.getGroupCoaches(adminId, groupId);
        return ResponseEntity.ok(Map.of(
                "groupId", groupId,
                "coaches", coaches
        ));
    }

    @GetMapping("/{groupId}/coaches/history")
    public ResponseEntity<Collection<AdminGroupCoachHistoryOutput>> getGroupCoachHistory(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID groupId
    ) {
        return ResponseEntity.ok(adminGroupService.getGroupCoachHistory(
                UUID.fromString(jwt.getSubject()), groupId
        ));
    }

    /* ================= SCHEDULE ================= */

    @PostMapping("/{groupId}/schedule")
    public void createSchedule(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID groupId,
            @RequestBody @Valid GroupScheduleBatchCommand command
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

    @PostMapping("/{groupId}/schedule/validate")
    public ResponseEntity<ScheduleValidationResult> validateSchedule(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID groupId,
            @Valid @RequestBody GroupScheduleValidationCommand command
    ) {
        UUID adminId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(adminGroupService.validateGroupSchedule(adminId, groupId, command));
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

    @GetMapping("/{groupId}/available-slots")
    public List<AvailableSlotOutput> getGroupAvailableSlots(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID groupId,
            @RequestParam LocalDate date
    ) {
        UUID adminId = UUID.fromString(jwt.getSubject());
        return adminGroupService.getGroupAvailableSlots(adminId, groupId, date);
    }
}
