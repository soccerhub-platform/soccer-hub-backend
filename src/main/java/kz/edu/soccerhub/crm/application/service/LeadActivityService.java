package kz.edu.soccerhub.crm.application.service;

import kz.edu.soccerhub.common.dto.admin.AdminDto;
import kz.edu.soccerhub.common.dto.lead.LeadActivityOutput;
import kz.edu.soccerhub.common.port.AdminPort;
import kz.edu.soccerhub.crm.domain.model.Lead;
import kz.edu.soccerhub.crm.domain.model.LeadActivity;
import kz.edu.soccerhub.crm.domain.model.enums.LeadActivityType;
import kz.edu.soccerhub.crm.domain.model.enums.LeadStatus;
import kz.edu.soccerhub.crm.domain.repository.LeadActivityRepository;
import kz.edu.soccerhub.crm.application.state.LeadEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LeadActivityService {

    private final LeadActivityRepository leadActivityRepository;
    private final AdminPort adminPort;

    @Transactional(readOnly = true)
    public List<LeadActivityOutput> getLeadActivities(UUID leadId) {
        return leadActivityRepository.findByLeadIdOrderByCreatedAtDesc(leadId).stream()
                .sorted(Comparator.comparing(LeadActivity::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toOutput)
                .toList();
    }

    @Transactional
    public void logLeadCreated(Lead lead) {
        save(
                lead.getId(),
                LeadActivityType.LEAD_CREATED,
                null,
                null,
                lead.getStatus(),
                lead.getAssignedAdminId(),
                lead.getAssignedAdminId(),
                "Lead created"
        );
    }

    @Transactional
    public void logLeadAssigned(Lead lead, UUID previousAssignedAdminId, UUID actorAdminId) {
        save(
                lead.getId(),
                LeadActivityType.ASSIGNED_ADMIN_CHANGED,
                null,
                null,
                null,
                lead.getAssignedAdminId(),
                actorAdminId,
                "Assigned admin changed from " + previousAssignedAdminId + " to " + lead.getAssignedAdminId()
        );
    }

    @Transactional
    public void logStatusChanged(Lead lead, LeadEvent event, LeadStatus fromStatus, UUID actorAdminId) {
        logStatusChanged(lead, event, fromStatus, actorAdminId, null);
    }

    @Transactional
    public void logStatusChanged(
            Lead lead,
            LeadEvent event,
            LeadStatus fromStatus,
            UUID actorAdminId,
            String detailsOverride
    ) {
        save(
                lead.getId(),
                LeadActivityType.STATUS_CHANGED,
                event,
                fromStatus,
                lead.getStatus(),
                lead.getAssignedAdminId(),
                actorAdminId,
                detailsOverride != null ? detailsOverride : "Lead status changed via event " + event
        );
    }

    @Transactional
    public void logLeadConverted(Lead lead, UUID actorAdminId) {
        save(
                lead.getId(),
                LeadActivityType.LEAD_CONVERTED,
                null,
                null,
                lead.getStatus(),
                lead.getAssignedAdminId(),
                actorAdminId,
                "Lead converted to client " + lead.getClientId()
        );
    }

    private void save(
            UUID leadId,
            LeadActivityType activityType,
            LeadEvent event,
            LeadStatus fromStatus,
            LeadStatus toStatus,
            UUID assignedAdminId,
            UUID actorAdminId,
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
                        .actorAdminId(actorAdminId)
                        .details(details)
                        .build()
        );
    }

    private LeadActivityOutput toOutput(LeadActivity activity) {
        return new LeadActivityOutput(
                resolveType(activity),
                resolveDescription(activity),
                activity.getCreatedAt(),
                resolveActorName(
                        activity.getActorAdminId() != null
                                ? activity.getActorAdminId()
                                : activity.getAssignedAdminId()
                )
        );
    }

    private String resolveType(LeadActivity activity) {
        if (activity.getEvent() != null) {
            return activity.getEvent().name();
        }
        return activity.getActivityType().name();
    }

    private String resolveDescription(LeadActivity activity) {
        if (activity.getActivityType() == LeadActivityType.LEAD_CREATED) {
            return "Лид создан";
        }

        if (activity.getEvent() == null) {
            return switch (activity.getActivityType()) {
                case ASSIGNED_ADMIN_CHANGED -> "Назначен ответственный администратор";
                case LEAD_CONVERTED -> "Лид конвертирован в клиента";
                default -> defaultDescription(activity);
            };
        }

        return switch (activity.getEvent()) {
            case QUALIFY -> "Квалифицирован";
            case SCHEDULE_TRIAL -> "Назначено пробное";
            case COMPLETE_TRIAL -> "Пробное завершено";
            case NO_SHOW -> "Не пришел";
            case REQUEST_PAYMENT -> "Запрошена оплата";
            case POST_TRIAL_REJECT -> "Отказ после пробного";
            case CONFIRM_PAYMENT -> "Оплата подтверждена";
            case CONTACT -> "Связались с клиентом";
            case REJECT -> "Лид закрыт";
        };
    }

    private String defaultDescription(LeadActivity activity) {
        if (activity.getDetails() == null || activity.getDetails().isBlank()) {
            return "Событие по лиду";
        }
        return activity.getDetails();
    }

    private String resolveActorName(UUID assignedAdminId) {
        if (assignedAdminId == null) {
            return null;
        }

        return adminPort.findById(assignedAdminId)
                .map(this::toDisplayName)
                .orElse(null);
    }

    private String toDisplayName(AdminDto adminDto) {
        String fullName = ((adminDto.firstName() == null ? "" : adminDto.firstName()) + " "
                + (adminDto.lastName() == null ? "" : adminDto.lastName())).trim();

        if (!fullName.isBlank()) {
            return fullName;
        }

        return adminDto.email();
    }
}
