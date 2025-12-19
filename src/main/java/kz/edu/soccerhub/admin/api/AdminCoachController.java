package kz.edu.soccerhub.admin.api;

import jakarta.validation.Valid;
import kz.edu.soccerhub.admin.application.service.AdminCoachService;
import kz.edu.soccerhub.admin.application.dto.coach.AdminCoachAssignBranchInput;
import kz.edu.soccerhub.admin.application.dto.coach.AdminCoachUnassignBranchInput;
import kz.edu.soccerhub.admin.application.dto.coach.AdminCreateCoachInput;
import kz.edu.soccerhub.admin.application.dto.coach.AdminCreateCoachOutput;
import kz.edu.soccerhub.common.dto.coach.CoachDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin/coach")
@PreAuthorize("hasAuthority('ADMIN')")
@RequiredArgsConstructor
public class AdminCoachController {

    private final AdminCoachService adminCoachService;

    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createCoach(@AuthenticationPrincipal Jwt jwt,
                                         @Valid @RequestBody AdminCreateCoachInput input) {
        final UUID adminId = UUID.fromString(jwt.getSubject());
        AdminCreateCoachOutput output = adminCoachService.createCoach(adminId, input);
        return ResponseEntity.created(URI.create("/coach/" + output.coachId()))
                .body(Map.of("coachId", output.coachId()));
    }

    @PostMapping(value = "/{coachId}/assign-branch",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> assignBranch(@AuthenticationPrincipal Jwt jwt,
                                             @PathVariable UUID coachId,
                                             @Valid @RequestBody AdminCoachAssignBranchInput input) {
        final UUID adminId = UUID.fromString(jwt.getSubject());
        adminCoachService.assignCoachToBranch(adminId, coachId, input.branchId());
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/{coachId}/unassign-branch",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> unassignBranch(@AuthenticationPrincipal Jwt jwt,
                                               @PathVariable UUID coachId,
                                               @Valid @RequestBody AdminCoachUnassignBranchInput input) {
        final UUID adminId = UUID.fromString(jwt.getSubject());
        adminCoachService.unassignCoachFromBranch(adminId, coachId, input.branchId());
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/all/branch/{branchId}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<CoachDto>> getCoachByBranchId(@AuthenticationPrincipal Jwt jwt,
                                               @PathVariable("branchId") UUID branchId,
                                               @PageableDefault Pageable pageable) {
        final UUID adminId = UUID.fromString(jwt.getSubject());
        Page<CoachDto> output = adminCoachService.getCoachByBranchId(adminId, branchId, pageable);
        return ResponseEntity.ok(output);
    }

}
