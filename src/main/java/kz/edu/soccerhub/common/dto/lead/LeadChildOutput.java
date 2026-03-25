package kz.edu.soccerhub.common.dto.lead;

import kz.edu.soccerhub.crm.domain.model.enums.Gender;

import java.util.UUID;

public record LeadChildOutput(
        UUID id,
        String childName,
        Integer childAge,
        Gender gender,
        String experience
) {
}

