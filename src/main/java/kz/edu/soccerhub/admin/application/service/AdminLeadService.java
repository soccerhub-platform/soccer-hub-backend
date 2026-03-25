package kz.edu.soccerhub.admin.application.service;

import kz.edu.soccerhub.admin.application.dto.lead.AdminLeadCreateInput;
import kz.edu.soccerhub.common.dto.lead.LeadCreateCommand;
import kz.edu.soccerhub.common.dto.lead.LeadCreateOutput;
import kz.edu.soccerhub.common.dto.lead.LeadEventOutput;
import kz.edu.soccerhub.common.dto.lead.LeadKanbanOutput;
import kz.edu.soccerhub.common.dto.lead.LeadOutput;
import kz.edu.soccerhub.common.dto.lead.LeadQualificationInput;
import kz.edu.soccerhub.common.dto.lead.ScheduleTrialInput;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.LeadPort;
import kz.edu.soccerhub.crm.domain.model.enums.LeadStatus;
import kz.edu.soccerhub.crm.state.LeadEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminLeadService {

    private final LeadPort leadPort;
    private final AdminService adminService;
    private final AdminBranchService adminBranchService;

    @Transactional
    public LeadCreateOutput createLead(UUID adminId, AdminLeadCreateInput input) {
        adminService.findById(adminId)
                .orElseThrow(() -> new NotFoundException("Admin not found", adminId));

        boolean hasAccess = adminBranchService.verifyAdminBelongsToBranch(adminId, input.branchId());
        if (!hasAccess) {
            throw new BadRequestException("Admin does not have access to branch", input.branchId());
        }

        LeadCreateCommand command = new LeadCreateCommand(
                input.name(),
                input.phone(),
                input.email(),
                input.comment(),
                adminId,
                input.branchId(),
                input.children()
        );

        UUID leadId = leadPort.createLead(command);
        return new LeadCreateOutput(leadId);
    }

    @Transactional
    public void qualifyLead(UUID adminId, UUID leadId, LeadQualificationInput input) {
        adminService.findById(adminId)
                .orElseThrow(() -> new NotFoundException("Admin not found", adminId));

        leadPort.qualifyLead(leadId, input);
    }

    @Transactional
    public void scheduleTrial(UUID adminId, UUID leadId, ScheduleTrialInput input) {
        adminService.findById(adminId)
                .orElseThrow(() -> new NotFoundException("Admin not found", adminId));

        leadPort.scheduleTrial(leadId, input);
    }

    @Transactional
    public UUID convertLeadToClient(UUID adminId, UUID leadId) {
        adminService.findById(adminId)
                .orElseThrow(() -> new NotFoundException("Admin not found", adminId));

        return leadPort.convertLeadToClient(leadId);
    }

    @Transactional
    public LeadEventOutput processEvent(UUID adminId, UUID leadId, LeadEvent event) {
        adminService.findById(adminId)
                .orElseThrow(() -> new NotFoundException("Admin not found", adminId));

        LeadStatus status = leadPort.processEvent(leadId, event);
        return new LeadEventOutput(leadId, status);
    }

    @Transactional(readOnly = true)
    public LeadKanbanOutput getKanban(UUID adminId, UUID branchId) {
        adminService.findById(adminId)
                .orElseThrow(() -> new NotFoundException("Admin not found", adminId));

        boolean hasAccess = adminBranchService.verifyAdminBelongsToBranch(adminId, branchId);
        if (!hasAccess) {
            throw new BadRequestException("Admin does not have access to branch", branchId);
        }

        Map<LeadStatus, List<LeadOutput>> columns = leadPort.getKanban(branchId);
        return new LeadKanbanOutput(columns);
    }
}
