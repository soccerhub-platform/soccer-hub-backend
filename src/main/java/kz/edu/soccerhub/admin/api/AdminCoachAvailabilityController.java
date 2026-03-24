package kz.edu.soccerhub.admin.api;

import kz.edu.soccerhub.admin.application.service.AdminCoachService;
import kz.edu.soccerhub.common.dto.lead.AvailableSlotOutput;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/coaches")
@PreAuthorize("hasAuthority('ADMIN')")
@RequiredArgsConstructor
public class AdminCoachAvailabilityController {

    private final AdminCoachService adminCoachService;

    @GetMapping("/{coachId}/available-slots")
    public List<AvailableSlotOutput> getCoachAvailableSlots(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID coachId,
            @RequestParam LocalDate date
    ) {
        UUID adminId = UUID.fromString(jwt.getSubject());
        return adminCoachService.getCoachAvailableSlots(adminId, coachId, date);
    }
}

