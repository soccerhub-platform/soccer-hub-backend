package kz.edu.soccerhub.dispatcher.api;

import kz.edu.soccerhub.common.dto.analytics.AnalyticsGroupBy;
import kz.edu.soccerhub.common.dto.analytics.AnalyticsResponseOutput;
import kz.edu.soccerhub.dispatcher.application.service.DispatcherAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/dispatcher/analytics")
@PreAuthorize("hasAuthority('DISPATCHER')")
@RequiredArgsConstructor
public class DispatcherAnalyticsController {

    private final DispatcherAnalyticsService dispatcherAnalyticsService;

    @GetMapping("/funnel")
    public ResponseEntity<AnalyticsResponseOutput> getFunnelAnalytics(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam UUID branchId,
            @RequestParam LocalDate dateFrom,
            @RequestParam LocalDate dateTo,
            @RequestParam(required = false) AnalyticsGroupBy groupBy,
            @RequestParam(required = false) String timezone,
            @RequestParam(required = false) UUID coachId,
            @RequestParam(required = false) UUID groupId
    ) {
        UUID dispatcherId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(
                dispatcherAnalyticsService.getFunnelAnalytics(
                        dispatcherId,
                        branchId,
                        dateFrom,
                        dateTo,
                        groupBy,
                        timezone,
                        coachId,
                        groupId
                )
        );
    }

    @GetMapping("/coach-load")
    public ResponseEntity<AnalyticsResponseOutput> getCoachLoad(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam UUID branchId,
            @RequestParam LocalDate dateFrom,
            @RequestParam LocalDate dateTo,
            @RequestParam(required = false) AnalyticsGroupBy groupBy,
            @RequestParam(required = false) String timezone,
            @RequestParam(required = false) UUID coachId,
            @RequestParam(required = false) UUID groupId
    ) {
        UUID dispatcherId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(
                dispatcherAnalyticsService.getCoachLoadAnalytics(
                        dispatcherId,
                        branchId,
                        dateFrom,
                        dateTo,
                        groupBy,
                        timezone,
                        coachId,
                        groupId
                )
        );
    }

    @GetMapping("/retention")
    public ResponseEntity<AnalyticsResponseOutput> getRetention(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam UUID branchId,
            @RequestParam LocalDate dateFrom,
            @RequestParam LocalDate dateTo,
            @RequestParam(required = false) AnalyticsGroupBy groupBy,
            @RequestParam(required = false) String timezone,
            @RequestParam(required = false) UUID coachId,
            @RequestParam(required = false) UUID groupId
    ) {
        UUID dispatcherId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(
                dispatcherAnalyticsService.getRetentionAnalytics(
                        dispatcherId, branchId, dateFrom, dateTo, groupBy, timezone, coachId, groupId
                )
        );
    }

    @GetMapping("/sla")
    public ResponseEntity<AnalyticsResponseOutput> getSla(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam UUID branchId,
            @RequestParam LocalDate dateFrom,
            @RequestParam LocalDate dateTo,
            @RequestParam(required = false) AnalyticsGroupBy groupBy,
            @RequestParam(required = false) String timezone,
            @RequestParam(required = false) UUID coachId,
            @RequestParam(required = false) UUID groupId
    ) {
        UUID dispatcherId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(
                dispatcherAnalyticsService.getSlaAnalytics(
                        dispatcherId, branchId, dateFrom, dateTo, groupBy, timezone, coachId, groupId
                )
        );
    }

    @GetMapping("/loss-reasons")
    public ResponseEntity<AnalyticsResponseOutput> getLossReasons(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam UUID branchId,
            @RequestParam LocalDate dateFrom,
            @RequestParam LocalDate dateTo,
            @RequestParam(required = false) AnalyticsGroupBy groupBy,
            @RequestParam(required = false) String timezone,
            @RequestParam(required = false) UUID coachId,
            @RequestParam(required = false) UUID groupId
    ) {
        UUID dispatcherId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(
                dispatcherAnalyticsService.getLossReasonsAnalytics(
                        dispatcherId, branchId, dateFrom, dateTo, groupBy, timezone, coachId, groupId
                )
        );
    }

    @GetMapping("/kpi")
    public ResponseEntity<AnalyticsResponseOutput> getKpi(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam UUID branchId,
            @RequestParam LocalDate dateFrom,
            @RequestParam LocalDate dateTo,
            @RequestParam(required = false) AnalyticsGroupBy groupBy,
            @RequestParam(required = false) String timezone,
            @RequestParam(required = false) UUID coachId,
            @RequestParam(required = false) UUID groupId
    ) {
        UUID dispatcherId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(
                dispatcherAnalyticsService.getKpiAnalytics(
                        dispatcherId, branchId, dateFrom, dateTo, groupBy, timezone, coachId, groupId
                )
        );
    }
}
