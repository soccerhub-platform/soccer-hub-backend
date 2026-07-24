package kz.edu.soccerhub.crm.domain.model.enums;

public enum LeadStatus {

    NEW,
    IN_PROGRESS,
    TRIAL_SCHEDULED,
    DECISION_PENDING,
    CONVERTED,
    LOST,

    ;

    public boolean isFinal() {
        return this == CONVERTED || this == LOST;
    }
}
