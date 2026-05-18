package kz.edu.soccerhub.dispatcher.api;

import kz.edu.soccerhub.common.dto.analytics.AnalyticsCohortBy;
import kz.edu.soccerhub.common.dto.analytics.AnalyticsGroupBy;
import kz.edu.soccerhub.common.dto.analytics.CoachLoadAnalyticsOutput;
import kz.edu.soccerhub.common.dto.analytics.FunnelAnalyticsOutput;
import kz.edu.soccerhub.common.dto.analytics.RetentionAnalyticsOutput;
import kz.edu.soccerhub.common.dto.analytics.SlaAnalyticsOutput;
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
    public ResponseEntity<FunnelAnalyticsOutput> getFunnelAnalytics(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam UUID branchId,
            @RequestParam LocalDate dateFrom,
            @RequestParam LocalDate dateTo,
            @RequestParam AnalyticsGroupBy groupBy,
            @RequestParam(required = false) String timezone
    ) {
        UUID dispatcherId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(
                dispatcherAnalyticsService.getFunnelAnalytics(
                        dispatcherId,
                        branchId,
                        dateFrom,
                        dateTo,
                        groupBy,
                        timezone
                )
        );
    }

    @GetMapping("/coach-load")
    public ResponseEntity<CoachLoadAnalyticsOutput> getCoachLoad(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam UUID branchId,
            @RequestParam LocalDate dateFrom,
            @RequestParam LocalDate dateTo,
            @RequestParam(required = false) AnalyticsGroupBy groupBy,
            @RequestParam(required = false) String timezone
    ) {
        UUID dispatcherId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(
                dispatcherAnalyticsService.getCoachLoadAnalytics(
                        dispatcherId,
                        branchId,
                        dateFrom,
                        dateTo,
                        groupBy,
                        timezone
                )
        );
    }

    @GetMapping("/retention")
    public ResponseEntity<RetentionAnalyticsOutput> getRetention(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam UUID branchId,
            @RequestParam AnalyticsCohortBy cohortBy,
            @RequestParam int periods,
            @RequestParam(required = false) String timezone
    ) {
        UUID dispatcherId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(
                dispatcherAnalyticsService.getRetentionAnalytics(dispatcherId, branchId, cohortBy, periods, timezone)
        );
    }

    @GetMapping("/sla")
    public ResponseEntity<SlaAnalyticsOutput> getSla(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam UUID branchId,
            @RequestParam LocalDate dateFrom,
            @RequestParam LocalDate dateTo,
            @RequestParam(required = false) AnalyticsGroupBy groupBy,
            @RequestParam(required = false) String timezone
    ) {
        UUID dispatcherId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(
                dispatcherAnalyticsService.getSlaAnalytics(dispatcherId, branchId, dateFrom, dateTo, groupBy, timezone)
        );
    }
}

