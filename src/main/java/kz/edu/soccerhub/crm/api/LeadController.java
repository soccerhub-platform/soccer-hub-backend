package kz.edu.soccerhub.crm.api;

import kz.edu.soccerhub.common.dto.lead.LeadOutput;
import kz.edu.soccerhub.crm.application.mapper.LeadMapper;
import kz.edu.soccerhub.crm.domain.model.enums.LeadStatus;
import kz.edu.soccerhub.crm.service.LeadService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/leads")
@RequiredArgsConstructor
public class LeadController {

    private final LeadService leadService;

    @GetMapping
    public ResponseEntity<Page<LeadOutput>> getLeads(
            @RequestParam(required = false) List<LeadStatus> statuses,
            @RequestParam(required = false) UUID assignedAdminId,
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) Boolean unassigned,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) LocalDate createdFrom,
            @RequestParam(required = false) LocalDate createdTo,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<LeadOutput> leads = leadService.getLeads(
                        statuses,
                        assignedAdminId,
                        branchId,
                        unassigned,
                        search,
                        createdFrom,
                        createdTo,
                        pageable
                )
                .map(LeadMapper::toOutput);

        return ResponseEntity.ok(leads);
    }

    @GetMapping("/{leadId}")
    public ResponseEntity<LeadOutput> getLeadById(@PathVariable UUID leadId) {
        return ResponseEntity.ok(LeadMapper.toOutput(leadService.getLeadById(leadId)));
    }

}

