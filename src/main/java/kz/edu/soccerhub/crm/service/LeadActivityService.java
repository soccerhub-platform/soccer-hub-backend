package kz.edu.soccerhub.crm.service;

import kz.edu.soccerhub.crm.domain.model.Lead;
import kz.edu.soccerhub.crm.domain.model.LeadActivity;
import kz.edu.soccerhub.crm.domain.model.enums.LeadActivityType;
import kz.edu.soccerhub.crm.domain.model.enums.LeadStatus;
import kz.edu.soccerhub.crm.domain.repository.LeadActivityRepository;
import kz.edu.soccerhub.crm.state.LeadEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LeadActivityService {

    private final LeadActivityRepository leadActivityRepository;

    @Transactional
    public void logLeadCreated(Lead lead) {
        save(
                lead.getId(),
                LeadActivityType.LEAD_CREATED,
                null,
                null,
                lead.getStatus(),
                lead.getAssignedAdminId(),
                "Lead created"
        );
    }

    @Transactional
    public void logLeadAssigned(Lead lead, UUID previousAssignedAdminId) {
        save(
                lead.getId(),
                LeadActivityType.ASSIGNED_ADMIN_CHANGED,
                null,
                null,
                null,
                lead.getAssignedAdminId(),
                "Assigned admin changed from " + previousAssignedAdminId + " to " + lead.getAssignedAdminId()
        );
    }

    @Transactional
    public void logStatusChanged(Lead lead, LeadEvent event, LeadStatus fromStatus) {
        save(
                lead.getId(),
                LeadActivityType.STATUS_CHANGED,
                event,
                fromStatus,
                lead.getStatus(),
                lead.getAssignedAdminId(),
                "Lead status changed via event " + event
        );
    }

    private void save(
            UUID leadId,
            LeadActivityType activityType,
            LeadEvent event,
            LeadStatus fromStatus,
            LeadStatus toStatus,
            UUID assignedAdminId,
            String details
    ) {
        leadActivityRepository.save(
                LeadActivity.builder()
                        .id(UUID.randomUUID())
                        .leadId(leadId)
                        .activityType(activityType)
                        .event(event)
                        .fromStatus(fromStatus)
                        .toStatus(toStatus)
                        .assignedAdminId(assignedAdminId)
                        .details(details)
                        .build()
        );
    }
}

