package kz.edu.soccerhub.coach.api;

import jakarta.validation.Valid;
import kz.edu.soccerhub.coach.application.dto.session.*;
import kz.edu.soccerhub.coach.application.service.CoachSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/coach")
@PreAuthorize("hasAuthority('COACH')")
@RequiredArgsConstructor
public class CoachSessionController {

    private static final String DEFAULT_TIMEZONE = "Asia/Almaty";

    private final CoachSessionService coachSessionService;

    @GetMapping("/sessions/today")
    public CoachTodaySessionsResponse todaySessions(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam LocalDate date,
            @RequestParam(defaultValue = DEFAULT_TIMEZONE) String timezone
    ) {
        return coachSessionService.getTodaySessions(getCurrentUserId(jwt), date, timezone);
    }

    @GetMapping("/sessions/{sessionId}")
    public CoachSessionDetailsResponse sessionDetails(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID sessionId,
            @RequestParam(defaultValue = DEFAULT_TIMEZONE) String timezone
    ) {
        return coachSessionService.getSessionDetails(getCurrentUserId(jwt), sessionId, timezone);
    }

    @GetMapping("/schedule")
    public CoachScheduleResponse schedule(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam LocalDate dateFrom,
            @RequestParam LocalDate dateTo,
            @RequestParam(defaultValue = DEFAULT_TIMEZONE) String timezone
    ) {
        return coachSessionService.getSchedule(getCurrentUserId(jwt), dateFrom, dateTo, timezone);
    }

    @GetMapping("/history")
    public CoachHistoryResponse history(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam LocalDate dateFrom,
            @RequestParam LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return coachSessionService.getHistory(getCurrentUserId(jwt), dateFrom, dateTo, page, size);
    }

    @PatchMapping("/sessions/{sessionId}/attendance")
    public CoachAttendanceUpdateResponse updateAttendance(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID sessionId,
            @Valid @RequestBody CoachAttendancePatchInput input
    ) {
        return coachSessionService.updateAttendance(getCurrentUserId(jwt), sessionId, input);
    }

    @PostMapping("/sessions/{sessionId}/attendance/mark-all-present")
    public CoachAttendanceUpdateResponse markAllPresent(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID sessionId
    ) {
        return coachSessionService.markAllPresent(getCurrentUserId(jwt), sessionId);
    }

    @PutMapping("/sessions/{sessionId}/report")
    public CoachReportSaveResponse saveReport(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID sessionId,
            @Valid @RequestBody CoachReportSaveInput input
    ) {
        return coachSessionService.saveReport(getCurrentUserId(jwt), sessionId, input);
    }

    @PostMapping("/sessions/{sessionId}/start")
    public CoachSimpleStatusResponse start(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID sessionId
    ) {
        return coachSessionService.startSession(getCurrentUserId(jwt), sessionId);
    }

    @PostMapping("/sessions/{sessionId}/complete")
    public CoachSimpleStatusResponse complete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID sessionId
    ) {
        return coachSessionService.completeSession(getCurrentUserId(jwt), sessionId);
    }

    @PostMapping("/sessions/{sessionId}/cancel")
    public CoachSimpleStatusResponse cancel(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID sessionId,
            @Valid @RequestBody CoachCancelSessionInput input
    ) {
        return coachSessionService.cancelSession(getCurrentUserId(jwt), sessionId, input);
    }

    private UUID getCurrentUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
