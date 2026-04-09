package kz.edu.soccerhub.common.dto.lead;

public record LeadActionOutput(
		String type,
		String label,
		boolean primary,
		boolean danger,
		boolean enabled
) {
}

