package kz.edu.soccerhub.crm.application.resolver;

import kz.edu.soccerhub.common.dto.lead.LeadActionOutput;
import kz.edu.soccerhub.crm.application.state.LeadEvent;
import kz.edu.soccerhub.crm.domain.model.Lead;
import kz.edu.soccerhub.crm.domain.model.enums.LeadStatus;
import kz.edu.soccerhub.crm.domain.model.enums.LeadTrialStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
public class LeadActionResolver {

    public List<LeadActionOutput> resolve(Lead lead, UUID currentAdminId) {
        LeadStatus status = lead.getStatus();
        boolean isOwner = lead.getAssignedAdminId() == null
                || Objects.equals(lead.getAssignedAdminId(), currentAdminId);

        return switch (status) {

            case NEW -> List.of(
                    primary(LeadEvent.CONTACT, "Связаться", true, isOwner),
                    secondary(LeadEvent.REJECT, "Отказ / Закрыть", true, true, isOwner)
            );

            case CONTACTED -> List.of(
                    primary(LeadEvent.QUALIFY, "Квалифицировать", true, isOwner),
                    secondary(LeadEvent.REJECT, "Отказ / Закрыть", true, true, isOwner)
            );

            case QUALIFIED -> List.of(
                    primary(LeadEvent.SCHEDULE_TRIAL, "Назначить пробное", hasChildren(lead), isOwner),
                    secondary(LeadEvent.REJECT, "Отказ / Закрыть", true, true, isOwner)
            );

            case TRIAL_SCHEDULED -> List.of(
                    primary(LeadEvent.COMPLETE_TRIAL, "Отметить: пришел", true, isOwner),
                    secondary(LeadEvent.NO_SHOW, "Не пришел", true, true, isOwner)
            );

            case TRIAL_DONE -> List.of(
                    primary(LeadEvent.REQUEST_PAYMENT, "Отправить на оплату", hasCompletedTrial(lead), isOwner),
                    secondary(LeadEvent.POST_TRIAL_REJECT, "Отказ после пробного", true, true, isOwner)
            );

            case WAITING_PAYMENT -> List.of(
                    primary(LeadEvent.CONFIRM_PAYMENT, "Подтвердить оплату", status == LeadStatus.WAITING_PAYMENT, isOwner),
                    secondary(LeadEvent.REJECT, "Отказ / Закрыть", true, true, isOwner)
            );

            case WON, LOST -> List.of();
        };
    }

    private LeadActionOutput primary(LeadEvent event, String label, boolean businessEnabled, boolean isOwner) {
        return new LeadActionOutput(event.name(), label, true, false, isOwner && businessEnabled);
    }

    private LeadActionOutput secondary(
            LeadEvent event,
            String label,
            boolean danger,
            boolean businessEnabled,
            boolean isOwner
    ) {
        return new LeadActionOutput(event.name(), label, false, danger, isOwner && businessEnabled);
    }

    private boolean hasChildren(Lead lead) {
        return lead.getChildren() != null && !lead.getChildren().isEmpty();
    }

    private boolean hasCompletedTrial(Lead lead) {
        return lead.getTrial() != null && lead.getTrial().getStatus() == LeadTrialStatus.COMPLETED;
    }
}
