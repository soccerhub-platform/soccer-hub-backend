package kz.edu.soccerhub.admin.api;

import jakarta.validation.Valid;
import kz.edu.soccerhub.admin.application.dto.coach.*;
import kz.edu.soccerhub.admin.application.service.AdminCoachService;
import kz.edu.soccerhub.coach.application.dto.profile.CoachAvailabilityResponse;
import kz.edu.soccerhub.coach.application.dto.profile.CoachAvailabilityUpdateRequest;
import kz.edu.soccerhub.coach.domain.model.enums.AccountStatus;
import kz.edu.soccerhub.coach.domain.model.enums.WorkStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('ADMIN')")
@RequiredArgsConstructor
public class AdminTrainerController {

    private final AdminCoachService adminCoachService;

    @GetMapping("/branches/{branchId}/trainers")
    public AdminTrainerListOutput getTrainers(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID branchId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) List<AccountStatus> accountStatuses,
            @RequestParam(required = false) List<WorkStatus> workStatuses,
            @RequestParam(required = false) AdminTrainerFilterEnums.GroupFilter groupFilter,
            @RequestParam(required = false) AdminTrainerFilterEnums.WorkloadStatus workloadStatus,
            @RequestParam(required = false) AdminTrainerFilterEnums.ReportStatus reportStatus,
            @RequestParam(required = false) Boolean hasSessionToday,
            @RequestParam(required = false) AdminTrainerFilterEnums.SortField sort,
            @RequestParam(defaultValue = "ASC") Sort.Direction direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return adminCoachService.getTrainers(
                currentUserId(jwt),
                branchId,
                search,
                accountStatuses,
                workStatuses,
                groupFilter,
                workloadStatus,
                reportStatus,
                hasSessionToday,
                sort,
                direction,
                page,
                size
        );
    }

    @GetMapping("/branches/{branchId}/trainers/summary")
    public AdminTrainerSummaryOutput getTrainersSummary(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID branchId
    ) {
        return adminCoachService.getTrainersSummary(currentUserId(jwt), branchId);
    }

    @GetMapping("/trainers/{trainerId}/overview")
    public AdminTrainerOverviewOutput getTrainerOverview(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID trainerId
    ) {
        return adminCoachService.getTrainerOverview(currentUserId(jwt), trainerId);
    }

    @GetMapping("/trainers/{trainerId}/availability")
    public CoachAvailabilityResponse getTrainerAvailability(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID trainerId
    ) {
        return adminCoachService.getTrainerAvailability(currentUserId(jwt), trainerId);
    }

    @PutMapping("/trainers/{trainerId}/availability")
    public CoachAvailabilityResponse updateTrainerAvailability(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID trainerId,
            @Valid @RequestBody CoachAvailabilityUpdateRequest request
    ) {
        return adminCoachService.updateTrainerAvailability(currentUserId(jwt), trainerId, request);
    }

    @PostMapping("/trainers/{trainerId}/group-assignments")
    public AdminTrainerGroupAssignmentOutput assignTrainerToGroup(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID trainerId,
            @Valid @RequestBody AdminTrainerGroupAssignmentInput input
    ) {
        return adminCoachService.assignTrainerToGroup(currentUserId(jwt), trainerId, input);
    }

    @GetMapping("/trainers/{trainerId}/activity")
    public ResponseEntity<AdminTrainerActivityOutput> getTrainerActivity(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID trainerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        UUID adminId = UUID.fromString(jwt.getSubject());

        return ResponseEntity.ok(
                adminCoachService.getTrainerActivity(adminId, trainerId, page, size)
        );
    }

    private UUID currentUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
