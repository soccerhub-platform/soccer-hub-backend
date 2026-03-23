package kz.edu.soccerhub.common.dto.lead;

import kz.edu.soccerhub.crm.domain.model.enums.LeadStatus;

import java.util.List;
import java.util.Map;

public record LeadKanbanOutput(
        Map<LeadStatus, List<LeadOutput>> columns
) {
}

