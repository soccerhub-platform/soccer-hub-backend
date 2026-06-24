package kz.edu.soccerhub.crm.application.service;

import kz.edu.soccerhub.common.dto.lead.LeadLossReasonStage;
import kz.edu.soccerhub.crm.application.state.LeadEvent;
import kz.edu.soccerhub.crm.domain.model.enums.LeadStatus;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class LeadLossReasonPolicy {

    private static final Map<LeadLossReasonStage, Set<String>> ALLOWED_CODES = Map.of(
            LeadLossReasonStage.PRE_QUALIFICATION, Set.of(
                    "PRICE",
                    "SCHEDULE_NOT_SUITABLE",
                    "LOCATION_NOT_SUITABLE",
                    "NO_RESPONSE",
                    "CHOOSE_COMPETITOR",
                    "CHANGED_MIND",
                    "PARENT_NOT_READY",
                    "MEDICAL_REASON",
                    "OTHER"
            ),
            LeadLossReasonStage.TRIAL_NO_SHOW, Set.of(
                    "SCHEDULE_NOT_SUITABLE",
                    "LOCATION_NOT_SUITABLE",
                    "NO_RESPONSE",
                    "CHANGED_MIND",
                    "MEDICAL_REASON",
                    "OTHER"
            ),
            LeadLossReasonStage.POST_TRIAL_REJECT, Set.of(
                    "PRICE",
                    "SCHEDULE_NOT_SUITABLE",
                    "LOCATION_NOT_SUITABLE",
                    "CHOOSE_COMPETITOR",
                    "CHANGED_MIND",
                    "CHILD_NOT_INTERESTED",
                    "PARENT_NOT_READY",
                    "MEDICAL_REASON",
                    "OTHER"
            ),
            LeadLossReasonStage.PAYMENT_REJECT, Set.of(
                    "PRICE",
                    "SCHEDULE_NOT_SUITABLE",
                    "LOCATION_NOT_SUITABLE",
                    "CHOOSE_COMPETITOR",
                    "CHANGED_MIND",
                    "PARENT_NOT_READY",
                    "MEDICAL_REASON",
                    "OTHER"
            )
    );

    public LeadLossReasonStage resolveStage(LeadEvent event, LeadStatus previousStatus) {
        return switch (event) {
            case NO_SHOW -> LeadLossReasonStage.TRIAL_NO_SHOW;
            case POST_TRIAL_REJECT -> LeadLossReasonStage.POST_TRIAL_REJECT;
            case REJECT -> previousStatus == LeadStatus.WAITING_PAYMENT
                    ? LeadLossReasonStage.PAYMENT_REJECT
                    : LeadLossReasonStage.PRE_QUALIFICATION;
            default -> null;
        };
    }

    public boolean isAllowed(String code, LeadLossReasonStage stage) {
        if (stage == null) {
            return true;
        }
        return ALLOWED_CODES.getOrDefault(stage, Set.of()).contains(code);
    }

    public Set<LeadLossReasonStage> resolveStagesForCode(String code) {
        EnumSet<LeadLossReasonStage> stages = EnumSet.noneOf(LeadLossReasonStage.class);
        for (Map.Entry<LeadLossReasonStage, Set<String>> entry : ALLOWED_CODES.entrySet()) {
            if (entry.getValue().contains(code)) {
                stages.add(entry.getKey());
            }
        }
        return stages;
    }
}
