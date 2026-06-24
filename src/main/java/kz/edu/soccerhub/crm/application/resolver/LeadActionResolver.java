package kz.edu.soccerhub.crm.application.resolver;

import kz.edu.soccerhub.common.dto.lead.LeadActionOutput;
import kz.edu.soccerhub.common.dto.lead.LeadActionType;
import kz.edu.soccerhub.crm.application.state.LeadEvent;
import kz.edu.soccerhub.crm.domain.model.Lead;
import kz.edu.soccerhub.crm.domain.model.enums.LeadStatus;
import kz.edu.soccerhub.crm.domain.model.enums.LeadTrialStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class LeadActionResolver {

    public List<LeadActionOutput> resolve(Lead lead, UUID currentAdminId) {
        LeadStatus status = lead.getStatus();
        boolean branchAllowed = true;

        return switch (status) {

            case NEW -> List.of(
                    primary(LeadActionType.CONTACT_LEAD, "Связаться", LeadEvent.CONTACT, true, branchAllowed),
                    secondary(LeadActionType.CLOSE_LEAD, "Отказ / Закрыть", LeadEvent.REJECT, true, true, branchAllowed)
            );

            case CONTACTED -> List.of(
                    primary(LeadActionType.QUALIFY_LEAD, "Квалифицировать", LeadEvent.QUALIFY, true, branchAllowed),
                    secondary(LeadActionType.CLOSE_LEAD, "Отказ / Закрыть", LeadEvent.REJECT, true, true, branchAllowed)
            );

            case QUALIFIED -> List.of(
                    primary(LeadActionType.SCHEDULE_TRIAL, "Назначить пробное", LeadEvent.SCHEDULE_TRIAL, hasParticipants(lead), branchAllowed),
                    secondary(LeadActionType.CLOSE_LEAD, "Отказ / Закрыть", LeadEvent.REJECT, true, true, branchAllowed)
            );

            case TRIAL_SCHEDULED -> List.of(
                    primary(LeadActionType.MARK_TRIAL_DONE, "Отметить: пришел", LeadEvent.COMPLETE_TRIAL, true, branchAllowed),
                    secondary(LeadActionType.RESCHEDULE_TRIAL, "Перенести пробное", null, false, true, branchAllowed),
                    secondary(LeadActionType.CANCEL_TRIAL, "Отменить пробное", LeadEvent.CANCEL_TRIAL, false, true, branchAllowed),
                    secondary(LeadActionType.MARK_NO_SHOW, "Не пришел", LeadEvent.NO_SHOW, true, true, branchAllowed)
            );

            case TRIAL_DONE -> lead.getContractId() == null
                    ? List.of(
                    primary(LeadActionType.CONVERT_TO_CONTRACT, "Оформить договор", null, hasCompletedTrial(lead), branchAllowed),
                    secondary(LeadActionType.CLOSE_LEAD, "Отказ после пробного", LeadEvent.POST_TRIAL_REJECT, true, true, branchAllowed)
            )
                    : List.of(
                    primary(LeadActionType.ADD_PAYMENT, "Добавить оплату", null, hasPaymentRequestReady(lead), branchAllowed),
                    secondary(LeadActionType.CLOSE_LEAD, "Отказ после пробного", LeadEvent.POST_TRIAL_REJECT, true, true, branchAllowed)
            );

            case WAITING_PAYMENT -> List.of(
                    primary(LeadActionType.ADD_PAYMENT, "Добавить оплату", null, lead.getContractId() != null, branchAllowed),
                    secondary(LeadActionType.CLOSE_LEAD, "Отказ / Закрыть", LeadEvent.REJECT, true, true, branchAllowed)
            );

            case WON, LOST -> List.of();
        };
    }

    private LeadActionOutput primary(LeadActionType type, String label, LeadEvent event, boolean businessEnabled, boolean isOwner) {
        return new LeadActionOutput(type, label, event, true, false, isOwner && businessEnabled);
    }

    private LeadActionOutput secondary(
            LeadActionType type,
            String label,
            LeadEvent event,
            boolean danger,
            boolean businessEnabled,
            boolean isOwner
    ) {
        return new LeadActionOutput(type, label, event, false, danger, isOwner && businessEnabled);
    }

    private boolean hasParticipants(Lead lead) {
        return lead.getParticipants() != null && !lead.getParticipants().isEmpty();
    }

    private boolean hasCompletedTrial(Lead lead) {
        return lead.getTrial() != null && lead.getTrial().getStatus() == LeadTrialStatus.COMPLETED;
    }

    private boolean hasPaymentRequestReady(Lead lead) {
        return hasCompletedTrial(lead) && lead.getContractId() != null;
    }
}
