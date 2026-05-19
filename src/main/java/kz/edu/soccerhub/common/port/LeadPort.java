package kz.edu.soccerhub.common.port;

import jakarta.validation.Valid;
import kz.edu.soccerhub.common.dto.lead.LeadActivityOutput;
import kz.edu.soccerhub.common.dto.lead.LeadCreateCommand;
import kz.edu.soccerhub.common.dto.lead.LeadOutput;
import kz.edu.soccerhub.common.dto.lead.LeadQualificationInput;
import kz.edu.soccerhub.common.dto.lead.ScheduleTrialInput;
import kz.edu.soccerhub.crm.domain.model.enums.LeadStatus;
import kz.edu.soccerhub.crm.application.state.LeadEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface LeadPort {

    UUID createLead(@Valid LeadCreateCommand command);

    void qualifyLead(UUID leadId, @Valid LeadQualificationInput input, UUID currentAdminId);

    void scheduleTrial(UUID leadId, @Valid ScheduleTrialInput input, UUID currentAdminId);

    void assignLead(UUID leadId, UUID assignedAdminId, UUID currentAdminId);

    LeadStatus processEvent(UUID leadId, LeadEvent event, UUID currentAdminId);

    UUID convertLeadToClient(UUID leadId, UUID currentAdminId);

    Map<LeadStatus, List<LeadOutput>> getKanban(UUID branchId, UUID currentAdminId);

    List<LeadActivityOutput> getLeadActivities(UUID leadId);

    UUID getLeadBranchId(UUID leadId);
}
