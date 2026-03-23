package kz.edu.soccerhub.common.port;

import jakarta.validation.Valid;
import kz.edu.soccerhub.common.dto.lead.LeadCreateCommand;
import kz.edu.soccerhub.common.dto.lead.LeadOutput;
import kz.edu.soccerhub.common.dto.lead.LeadQualificationInput;
import kz.edu.soccerhub.common.dto.lead.ScheduleTrialInput;
import kz.edu.soccerhub.crm.domain.model.enums.LeadStatus;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface LeadPort {

    UUID createLead(@Valid LeadCreateCommand command);

    void qualifyLead(UUID leadId, @Valid LeadQualificationInput input);

    void scheduleTrial(UUID leadId, @Valid ScheduleTrialInput input);

    Map<LeadStatus, List<LeadOutput>> getKanban(UUID branchId, UUID assignedAdminId, Boolean includeUnassigned);
}

