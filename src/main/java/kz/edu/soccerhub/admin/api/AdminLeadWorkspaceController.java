package kz.edu.soccerhub.admin.api;
import jakarta.validation.Valid;
import kz.edu.soccerhub.admin.api.dto.AdminLeadWorkInput;
import kz.edu.soccerhub.admin.application.service.AdminLeadWorkspaceService;
import kz.edu.soccerhub.common.dto.lead.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
@RestController
@RequestMapping("/admin/leads")
@PreAuthorize("hasAuthority('ADMIN')")
@RequiredArgsConstructor
public class AdminLeadWorkspaceController {
    private final AdminLeadWorkspaceService service;
    @PatchMapping("/{id}/preferences")
    public LeadOutput preferences(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @RequestBody @Valid kz.edu.soccerhub.admin.api.dto.AdminLeadPreferencesInput input) {
        return service.updatePreferences(UUID.fromString(jwt.getSubject()), id, input);
    }
    @GetMapping("/workspace")
    public LeadKanbanOutput get(@AuthenticationPrincipal Jwt jwt, @RequestParam UUID branchId) {
        return service.get(UUID.fromString(jwt.getSubject()), branchId);
    }
    @GetMapping("/{id}/workspace")
    public LeadOutput detail(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return service.detail(UUID.fromString(jwt.getSubject()), id);
    }
    @PatchMapping("/{id}/work")
    public LeadOutput update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                             @RequestBody @Valid AdminLeadWorkInput input) {
        return service.updateInput(UUID.fromString(jwt.getSubject()), id, input);
    }
}
