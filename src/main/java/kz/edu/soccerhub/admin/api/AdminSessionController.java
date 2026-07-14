package kz.edu.soccerhub.admin.api;

import jakarta.validation.Valid;
import kz.edu.soccerhub.admin.application.dto.session.AdminCancelSessionInput;
import kz.edu.soccerhub.admin.application.dto.session.AdminGroupAttendanceOutput;
import kz.edu.soccerhub.admin.application.dto.session.AdminGroupSessionsOutput;
import kz.edu.soccerhub.admin.application.dto.session.AdminRescheduleSessionInput;
import kz.edu.soccerhub.admin.application.dto.session.AdminSessionAttendanceOutput;
import kz.edu.soccerhub.admin.application.dto.session.AdminSessionAttendanceUpdateInput;
import kz.edu.soccerhub.admin.application.dto.session.AdminSessionDetailsOutput;
import kz.edu.soccerhub.admin.application.dto.session.AdminSubstituteCoachInput;
import kz.edu.soccerhub.admin.application.service.AdminSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasAuthority('ADMIN')")
@RequiredArgsConstructor
public class AdminSessionController {

    private final AdminSessionService adminSessionService;

    @GetMapping("/groups/{groupId}/sessions")
    public ResponseEntity<AdminGroupSessionsOutput> getGroupSessions(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID groupId,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID coachId
    ) {
        UUID adminId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(adminSessionService.getGroupSessions(adminId, groupId, from, to, status, coachId));
    }

    @GetMapping("/groups/{groupId}/attendance")
    public ResponseEntity<AdminGroupAttendanceOutput> getGroupAttendance(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID groupId,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to
    ) {
        UUID adminId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(adminSessionService.getGroupAttendance(adminId, groupId, from, to));
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<AdminSessionDetailsOutput> getSessionDetails(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID sessionId
    ) {
        UUID adminId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(adminSessionService.getSessionDetails(adminId, sessionId));
    }

    @GetMapping("/sessions/{sessionId}/attendance")
    public ResponseEntity<AdminSessionAttendanceOutput> getSessionAttendance(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID sessionId
    ) {
        UUID adminId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(adminSessionService.getSessionAttendance(adminId, sessionId));
    }

    @PostMapping("/sessions/{sessionId}/cancel")
    public ResponseEntity<AdminSessionDetailsOutput> cancelSession(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID sessionId,
            @Valid @RequestBody AdminCancelSessionInput input
    ) {
        UUID adminId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(adminSessionService.cancelSession(adminId, sessionId, input));
    }

    @PostMapping("/sessions/{sessionId}/reschedule")
    public ResponseEntity<AdminSessionDetailsOutput> rescheduleSession(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID sessionId,
            @Valid @RequestBody AdminRescheduleSessionInput input
    ) {
        UUID adminId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(adminSessionService.rescheduleSession(adminId, sessionId, input));
    }

    @PostMapping("/sessions/{sessionId}/substitute-coach")
    public ResponseEntity<AdminSessionDetailsOutput> substituteCoach(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID sessionId,
            @Valid @RequestBody AdminSubstituteCoachInput input
    ) {
        UUID adminId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(adminSessionService.substituteCoach(adminId, sessionId, input));
    }

    @PutMapping("/sessions/{sessionId}/attendance")
    public ResponseEntity<AdminSessionAttendanceOutput> updateSessionAttendance(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID sessionId,
            @Valid @RequestBody AdminSessionAttendanceUpdateInput input
    ) {
        UUID adminId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(adminSessionService.updateSessionAttendance(adminId, sessionId, input));
    }
}
