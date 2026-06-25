package kz.edu.soccerhub.admin.api;

import kz.edu.soccerhub.admin.application.dto.dashboard.AdminDashboardSummaryResponse;
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
import java.util.UUID;

@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardSummaryService adminDashboardSummaryService;

    @GetMapping("/summary")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<AdminDashboardSummaryResponse> getSummary(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam UUID branchId,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) String timezone
    ) {
        boolean superAdmin = jwt.getClaimAsStringList("roles") != null
                && jwt.getClaimAsStringList("roles").contains("SUPER_ADMIN");
        return ResponseEntity.ok(adminDashboardSummaryService.getSummary(
                UUID.fromString(jwt.getSubject()),
                branchId,
                date,
                timezone,
                superAdmin
        ));
    }
}
