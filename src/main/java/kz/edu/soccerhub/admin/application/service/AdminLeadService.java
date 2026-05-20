package kz.edu.soccerhub.admin.application.service;

import kz.edu.soccerhub.admin.application.dto.lead.AdminLeadCreateInput;
import kz.edu.soccerhub.common.dto.lead.LeadAssignInput;
import kz.edu.soccerhub.common.dto.lead.LeadActivityOutput;
import kz.edu.soccerhub.common.dto.lead.LeadCreateCommand;
import kz.edu.soccerhub.common.dto.lead.LeadCreateOutput;
import kz.edu.soccerhub.common.dto.lead.LeadEventOutput;
import kz.edu.soccerhub.common.dto.lead.LeadKanbanOutput;
import kz.edu.soccerhub.common.dto.lead.LeadLossReasonResponse;
import kz.edu.soccerhub.common.dto.lead.LeadOutput;
import kz.edu.soccerhub.common.dto.lead.LeadQualificationInput;
import kz.edu.soccerhub.common.dto.lead.ScheduleTrialInput;
import kz.edu.soccerhub.common.dto.lead.ConvertLeadRequest;
import kz.edu.soccerhub.common.dto.lead.ConvertLeadResponse;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.LeadPort;
import kz.edu.soccerhub.crm.domain.model.enums.LeadStatus;
import kz.edu.soccerhub.crm.application.state.LeadEvent;
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
        verifyAdminAccessToBranch(adminId, input.branchId());

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
        verifyAdminAccessToLead(adminId, leadId);
        leadPort.qualifyLead(leadId, input, adminId);
    }

    @Transactional
    public void assignLead(UUID adminId, UUID leadId, LeadAssignInput input) {
        verifyAdminAccessToLead(adminId, leadId);
        leadPort.assignLead(leadId, input.assignedAdminId(), adminId);
    }

    @Transactional
    public void scheduleTrial(UUID adminId, UUID leadId, ScheduleTrialInput input) {
        verifyAdminAccessToLead(adminId, leadId);
        leadPort.scheduleTrial(leadId, input, adminId);
    }

    @Transactional
    public ConvertLeadResponse convertLeadToClient(UUID actorUserId, UUID leadId, ConvertLeadRequest request) {
        return leadPort.convertLeadToClient(leadId, request, actorUserId);
    }

    @Transactional
    public LeadEventOutput processEvent(
            UUID adminId,
            UUID leadId,
            LeadEvent event,
            String lostReasonCode,
            String lostComment
    ) {
        verifyAdminAccessToLead(adminId, leadId);
        LeadStatus status = leadPort.processEvent(leadId, event, lostReasonCode, lostComment, adminId);
        return new LeadEventOutput(leadId, status);
    }

    @Transactional(readOnly = true)
    public LeadKanbanOutput getKanban(UUID adminId, UUID branchId) {
        verifyAdminAccessToBranch(adminId, branchId);

        Map<LeadStatus, List<LeadOutput>> columns = leadPort.getKanban(branchId, adminId);
        return new LeadKanbanOutput(columns);
    }

    @Transactional(readOnly = true)
    public List<LeadActivityOutput> getLeadActivities(UUID adminId, UUID leadId) {
        verifyAdminAccessToLead(adminId, leadId);
        return leadPort.getLeadActivities(leadId);
    }

    @Transactional(readOnly = true)
    public List<LeadLossReasonResponse> getActiveLossReasons(UUID adminId) {
        verifyAdminExists(adminId);
        return leadPort.getActiveLossReasons();
    }

    private void verifyAdminAccessToLead(UUID adminId, UUID leadId) {
        verifyAdminExists(adminId);
        UUID branchId = leadPort.getLeadBranchId(leadId);
        boolean hasAccess = adminBranchService.verifyAdminBelongsToBranch(adminId, branchId);
        if (!hasAccess) {
            throw new BadRequestException("Admin does not have access to branch", branchId);
        }
    }

    private void verifyAdminAccessToBranch(UUID adminId, UUID branchId) {
        verifyAdminExists(adminId);

        boolean hasAccess = adminBranchService.verifyAdminBelongsToBranch(adminId, branchId);
        if (!hasAccess) {
            throw new BadRequestException("Admin does not have access to branch", branchId);
        }
    }

    private void verifyAdminExists(UUID adminId) {
        adminService.findById(adminId)
                .orElseThrow(() -> new NotFoundException("Admin not found", adminId));
    }
}
