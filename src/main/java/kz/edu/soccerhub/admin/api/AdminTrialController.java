package kz.edu.soccerhub.admin.api;

import jakarta.validation.Valid;
import kz.edu.soccerhub.admin.application.dto.trial.*;
import kz.edu.soccerhub.admin.application.service.AdminTrialService;
import kz.edu.soccerhub.common.dto.trial.TrialBookingListItemDto;
import kz.edu.soccerhub.common.dto.trial.TrialBookingSearchCommand;
import kz.edu.soccerhub.trial.domain.enums.TrialBookingStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin/trials")
@PreAuthorize("hasAuthority('ADMIN')")
@RequiredArgsConstructor
public class AdminTrialController {

    private final AdminTrialService adminTrialService;

    @GetMapping
    public Page<TrialBookingListItemDto> find(
            @AuthenticationPrincipal Jwt jwt, @RequestParam UUID branchId,
            @RequestParam(required = false) TrialBookingStatus status,
            @RequestParam(required = false) UUID leadId,
            @RequestParam(required = false) UUID clientId,
            @RequestParam(required = false) UUID studentId,
            @RequestParam(required = false) UUID trainingSessionId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        TrialBookingSearchCommand command = new TrialBookingSearchCommand(
                status,
                leadId,
                clientId,
                studentId,
                trainingSessionId
        );

        return adminTrialService.scopedList(adminId(jwt),branchId,command,pageable);
    }

    @PostMapping
    public TrialBookingOutput create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateTrialBookingInput input
    ) {
        UUID adminId = UUID.fromString(jwt.getSubject());

        return TrialBookingOutput.from(
                adminTrialService.create(adminId, input)
        );
    }

    @GetMapping("/{trialId}")
    public AdminTrialDetailsOutput getDetails(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID trialId
    ) {
        adminTrialService.requireTrialAccess(adminId(jwt),trialId);
        return adminTrialService.getDetails(trialId);
    }

    @GetMapping("/{trialId}/history")
    public java.util.List<kz.edu.soccerhub.common.dto.company.TimelineEvent> history(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID trialId) {return adminTrialService.history(adminId(jwt),trialId);}

    @PostMapping("/{trialId}/cancel")
    public AdminTrialDetailsOutput cancel(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID trialId,
            @Valid @RequestBody AdminCancelTrialInput input
    ) {
        return adminTrialService.cancel(
                trialId,
                adminId(jwt),
                input.reason(), input.version()
        );
    }

    @PostMapping("/{trialId}/reschedule")
    public AdminTrialDetailsOutput reschedule(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID trialId,
            @Valid @RequestBody AdminRescheduleTrialInput input
    ) {
        return adminTrialService.reschedule(
                trialId,
                adminId(jwt),
                input
        );
    }

    @PostMapping("/{trialId}/attendance")
    public AdminTrialDetailsOutput markAttendance(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID trialId,
            @Valid @RequestBody AdminMarkTrialAttendanceInput input
    ) {
        return adminTrialService.markAttendance(
                trialId,
                adminId(jwt),
                input
        );
    }

    @PostMapping("/{trialId}/result")
    public AdminTrialDetailsOutput recordResult(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID trialId,
            @Valid @RequestBody AdminRecordTrialResultInput input
    ) {
        return adminTrialService.recordResult(
                trialId,
                adminId(jwt),
                input
        );
    }

    private UUID adminId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

}
