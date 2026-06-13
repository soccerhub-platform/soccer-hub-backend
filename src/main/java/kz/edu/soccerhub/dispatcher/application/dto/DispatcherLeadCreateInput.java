package kz.edu.soccerhub.dispatcher.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kz.edu.soccerhub.common.dto.lead.LeadParticipantInput;
import kz.edu.soccerhub.common.dto.lead.LeadPrimaryContactInput;
import kz.edu.soccerhub.crm.domain.model.enums.LeadType;

import java.util.List;
import java.util.UUID;

public record DispatcherLeadCreateInput(
        @NotNull(message = "Lead type is required")
        LeadType leadType,

        @Valid
        @NotNull(message = "Primary contact is required")
        LeadPrimaryContactInput primaryContact,

        @NotNull(message = "Branch id is required")
        UUID branchId,

        @Size(max = 1000, message = "Comment is too long")
        String comment,

        @Valid
        @NotNull(message = "Participants are required")
        List<LeadParticipantInput> participants
) {
}
