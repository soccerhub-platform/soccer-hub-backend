package kz.edu.soccerhub.common.dto.lead;

import jakarta.validation.constraints.NotNull;
import kz.edu.soccerhub.crm.application.state.LeadEvent;

public record LeadEventInput(
        @NotNull(message = "Event is required")
        LeadEvent event
) {
}

