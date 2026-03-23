package kz.edu.soccerhub.crm.domain.model.enums;

public enum LeadStatus {

    NEW,
    CONTACTED,
    QUALIFIED,
    TRIAL_SCHEDULED,
    TRIAL_DONE,
    WAITING_PAYMENT,
    WON,
    LOST;

    public boolean isFinal() {
        return this == WON || this == LOST;
    }
}