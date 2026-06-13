package kz.edu.soccerhub.admin.application.dto.lead;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kz.edu.soccerhub.common.dto.lead.LeadParticipantInput;
import kz.edu.soccerhub.common.dto.lead.LeadPrimaryContactInput;
import kz.edu.soccerhub.crm.domain.model.enums.LeadType;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record AdminLeadCreateInput(
        @NotNull(message = "Lead type is required")
        LeadType leadType,

        @Valid
        @NotNull(message = "Primary contact is required")
        LeadPrimaryContactInput primaryContact,

        @Size(max = 1000, message = "Comment is too long")
        String comment,

        @Valid
        @NotNull(message = "Participants are required")
        List<LeadParticipantInput> participants,

        @NotNull(message = "Branch id is required")
        UUID branchId
) {
}
