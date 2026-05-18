package kz.edu.soccerhub.admin.api;

import kz.edu.soccerhub.admin.application.service.AdminAnalyticsService;
import kz.edu.soccerhub.common.dto.analytics.AnalyticsCohortBy;
import kz.edu.soccerhub.common.dto.analytics.AnalyticsGroupBy;
import kz.edu.soccerhub.common.dto.analytics.CoachLoadAnalyticsOutput;
import kz.edu.soccerhub.common.dto.analytics.FunnelAnalyticsOutput;
import kz.edu.soccerhub.common.dto.analytics.RetentionAnalyticsOutput;
import kz.edu.soccerhub.common.dto.analytics.SlaAnalyticsOutput;
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
@RequestMapping("/admin/analytics")
@PreAuthorize("hasAuthority('ADMIN')")
@RequiredArgsConstructor
public class AdminAnalyticsController {

    private final AdminAnalyticsService adminAnalyticsService;

    @GetMapping("/funnel")
    public ResponseEntity<FunnelAnalyticsOutput> getFunnelAnalytics(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam UUID branchId,
            @RequestParam LocalDate dateFrom,
            @RequestParam LocalDate dateTo,
            @RequestParam AnalyticsGroupBy groupBy,
            @RequestParam(required = false) String timezone
    ) {
        UUID adminId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(
                adminAnalyticsService.getFunnelAnalytics(adminId, branchId, dateFrom, dateTo, groupBy, timezone)
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
        UUID adminId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(
                adminAnalyticsService.getCoachLoadAnalytics(adminId, branchId, dateFrom, dateTo, groupBy, timezone)
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
        UUID adminId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(
                adminAnalyticsService.getRetentionAnalytics(adminId, branchId, cohortBy, periods, timezone)
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
        UUID adminId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(
                adminAnalyticsService.getSlaAnalytics(adminId, branchId, dateFrom, dateTo, groupBy, timezone)
        );
    }
}

