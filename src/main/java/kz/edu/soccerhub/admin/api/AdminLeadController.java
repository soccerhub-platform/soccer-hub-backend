package kz.edu.soccerhub.admin.api;

import jakarta.validation.Valid;
import kz.edu.soccerhub.admin.application.dto.lead.AdminLeadCreateInput;
import kz.edu.soccerhub.admin.application.service.AdminLeadService;
import kz.edu.soccerhub.common.dto.lead.LeadCreateOutput;
import kz.edu.soccerhub.common.dto.lead.LeadConvertOutput;
import kz.edu.soccerhub.common.dto.lead.LeadEventInput;
import kz.edu.soccerhub.common.dto.lead.LeadEventOutput;
import kz.edu.soccerhub.common.dto.lead.LeadKanbanOutput;
import kz.edu.soccerhub.common.dto.lead.LeadQualificationInput;
import kz.edu.soccerhub.common.dto.lead.ScheduleTrialInput;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/admin/leads")
@PreAuthorize("hasAuthority('ADMIN')")
@RequiredArgsConstructor
@Validated
public class AdminLeadController {

    private final AdminLeadService adminLeadService;

    @PostMapping("/create")
    public ResponseEntity<LeadCreateOutput> createLead(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid AdminLeadCreateInput input
    ) {
        UUID adminId = UUID.fromString(jwt.getSubject());
        LeadCreateOutput output = adminLeadService.createLead(adminId, input);

        return ResponseEntity
                .created(URI.create("/leads/" + output.leadId()))
                .body(output);
    }

    @PatchMapping("/{leadId}/qualify")
    public ResponseEntity<Void> qualifyLead(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID leadId,
            @RequestBody @Valid LeadQualificationInput input
    ) {
        UUID adminId = UUID.fromString(jwt.getSubject());
        adminLeadService.qualifyLead(adminId, leadId, input);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{leadId}/trial")
    public ResponseEntity<Void> scheduleTrial(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID leadId,
            @RequestBody @Valid ScheduleTrialInput input
    ) {
        UUID adminId = UUID.fromString(jwt.getSubject());
        adminLeadService.scheduleTrial(adminId, leadId, input);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{leadId}/convert")
    public ResponseEntity<LeadConvertOutput> convertLead(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID leadId
    ) {
        UUID adminId = UUID.fromString(jwt.getSubject());
        UUID clientId = adminLeadService.convertLeadToClient(adminId, leadId);
        return ResponseEntity.ok(new LeadConvertOutput(clientId));
    }

        @PostMapping("/{leadId}/events")
        public ResponseEntity<LeadEventOutput> processEvent(
                @AuthenticationPrincipal Jwt jwt,
                @PathVariable UUID leadId,
                @RequestBody @Valid LeadEventInput input
        ) {
            UUID adminId = UUID.fromString(jwt.getSubject());
            return ResponseEntity.ok(adminLeadService.processEvent(adminId, leadId, input.event()));
        }

    @GetMapping("/kanban")
    public ResponseEntity<LeadKanbanOutput> getKanban(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam UUID branchId
    ) {
        UUID adminId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(adminLeadService.getKanban(adminId, branchId));
    }

}
