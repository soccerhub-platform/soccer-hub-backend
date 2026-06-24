package kz.edu.soccerhub.common.dto.lead;

import kz.edu.soccerhub.crm.application.state.LeadEvent;

public record LeadActionOutput(
		LeadActionType type,
		String label,
        LeadEvent event,
		boolean primary,
		boolean danger,
		boolean enabled
) {
}
