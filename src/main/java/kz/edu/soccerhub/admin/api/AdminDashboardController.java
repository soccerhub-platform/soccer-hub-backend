package kz.edu.soccerhub.admin.api;

import kz.edu.soccerhub.admin.application.dto.dashboard.AdminDashboardAlertsDto;
import kz.edu.soccerhub.admin.application.dto.dashboard.AdminDashboardBranchTodayDto;
import kz.edu.soccerhub.admin.application.dto.dashboard.AdminDashboardKpisDto;
import kz.edu.soccerhub.admin.application.dto.dashboard.AdminDashboardLeadFunnelDto;
import kz.edu.soccerhub.admin.application.dto.dashboard.AdminDashboardRiskItemDto;
import kz.edu.soccerhub.admin.application.dto.dashboard.AdminDashboardRisksDto;
import kz.edu.soccerhub.admin.application.dto.dashboard.AdminDashboardSummaryResponse;
import kz.edu.soccerhub.admin.application.dto.dashboard.AdminDashboardTodayScheduleDto;
import kz.edu.soccerhub.admin.application.dto.dashboard.AdminDashboardWeeklyTrendDto;
import kz.edu.soccerhub.admin.application.service.AdminDashboardSummaryService;
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
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardSummaryService adminDashboardSummaryService;

    @GetMapping("/summary")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<AdminDashboardSummaryResponse> getSummary(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam UUID branchId,
            @RequestParam LocalDate date,
            @RequestParam(required = false) String timezone
    ) {
        return ResponseEntity.ok(loadSummary(jwt, branchId, date, timezone));
    }

    @GetMapping("/alerts")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<AdminDashboardAlertsDto> getAlerts(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam UUID branchId,
            @RequestParam LocalDate date,
            @RequestParam(required = false) String timezone
    ) {
        return ResponseEntity.ok(loadSummary(jwt, branchId, date, timezone).alerts());
    }

    @GetMapping("/kpis")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<AdminDashboardKpisDto> getKpis(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam UUID branchId,
            @RequestParam LocalDate date,
            @RequestParam(required = false) String timezone
    ) {
        return ResponseEntity.ok(loadSummary(jwt, branchId, date, timezone).kpis());
    }

    @GetMapping("/branch-summary")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<AdminDashboardBranchTodayDto> getBranchSummary(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam UUID branchId,
            @RequestParam LocalDate date,
            @RequestParam(required = false) String timezone
    ) {
        return ResponseEntity.ok(adminDashboardSummaryService.getBranchSummary(
                UUID.fromString(jwt.getSubject()),
                branchId,
                date,
                timezone,
                isSuperAdmin(jwt)
        ));
    }

    @GetMapping("/funnel")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<AdminDashboardLeadFunnelDto> getFunnel(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam UUID branchId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) String timezone
    ) {
        return ResponseEntity.ok(adminDashboardSummaryService.getFunnel(
                UUID.fromString(jwt.getSubject()),
                branchId,
                from,
                to,
                date,
                timezone,
                isSuperAdmin(jwt)
        ));
    }

    @GetMapping("/risks")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<AdminDashboardRisksDto> getRisks(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam UUID branchId,
            @RequestParam LocalDate date,
            @RequestParam(required = false) String timezone
    ) {
        return ResponseEntity.ok(adminDashboardSummaryService.getRisks(
                UUID.fromString(jwt.getSubject()),
                branchId,
                date,
                timezone,
                isSuperAdmin(jwt)
        ));
    }

    @GetMapping("/today-schedule")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<AdminDashboardTodayScheduleDto> getTodaySchedule(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam UUID branchId,
            @RequestParam LocalDate date,
            @RequestParam(required = false) String timezone
    ) {
        return ResponseEntity.ok(adminDashboardSummaryService.getTodaySchedule(
                UUID.fromString(jwt.getSubject()),
                branchId,
                date,
                timezone,
                isSuperAdmin(jwt)
        ));
    }

    @GetMapping("/weekly-dynamics")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<AdminDashboardWeeklyTrendDto> getWeeklyDynamics(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam UUID branchId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) String timezone
    ) {
        return ResponseEntity.ok(adminDashboardSummaryService.getWeeklyDynamics(
                UUID.fromString(jwt.getSubject()),
                branchId,
                from,
                to,
                date,
                timezone,
                isSuperAdmin(jwt)
        ));
    }

    private AdminDashboardSummaryResponse loadSummary(
            Jwt jwt,
            UUID branchId,
            LocalDate date,
            String timezone
    ) {
        return adminDashboardSummaryService.getSummary(
                UUID.fromString(jwt.getSubject()),
                branchId,
                date,
                timezone,
                isSuperAdmin(jwt)
        );
    }

    private boolean isSuperAdmin(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        return roles != null && roles.contains("SUPER_ADMIN");
    }
}
