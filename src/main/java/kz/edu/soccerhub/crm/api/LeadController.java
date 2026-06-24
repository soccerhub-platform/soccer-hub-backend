package kz.edu.soccerhub.crm.api;

import kz.edu.soccerhub.common.dto.lead.LeadOutput;
import kz.edu.soccerhub.crm.application.mapper.LeadMapper;
import kz.edu.soccerhub.crm.domain.model.enums.LeadStatus;
import kz.edu.soccerhub.crm.application.service.LeadService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/leads")
@PreAuthorize("hasAuthority('ADMIN') or hasAuthority('DISPATCHER') or hasAuthority('SUPER_ADMIN')")
@RequiredArgsConstructor
public class LeadController {

    private final LeadService leadService;
    private final LeadMapper leadMapper;

    @GetMapping
    public ResponseEntity<Page<LeadOutput>> getLeads(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) List<LeadStatus> statuses,
            @RequestParam(required = false) UUID assignedAdminId,
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) Boolean unassigned,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) LocalDate createdFrom,
            @RequestParam(required = false) LocalDate createdTo,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        UUID currentAdminId = UUID.fromString(jwt.getSubject());
        Page<kz.edu.soccerhub.crm.domain.model.Lead> page = leadService.getLeads(
                statuses,
                assignedAdminId,
                branchId,
                unassigned,
                search,
                createdFrom,
                createdTo,
                pageable
        );
        Page<LeadOutput> leads = new PageImpl<>(
                leadMapper.toOutputs(page.getContent(), currentAdminId),
                pageable,
                page.getTotalElements()
        );

        return ResponseEntity.ok(leads);
    }

    @GetMapping("/{leadId}")
    public ResponseEntity<LeadOutput> getLeadById(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID leadId
    ) {
        UUID currentAdminId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(leadMapper.toOutput(leadService.getLeadById(leadId), currentAdminId));
    }

    @GetMapping("/kanban")
    public ResponseEntity<Map<LeadStatus, List<LeadOutput>>> getKanban(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam UUID branchId
    ) {
        UUID currentAdminId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(leadService.getKanban(branchId, currentAdminId));
    }

}
