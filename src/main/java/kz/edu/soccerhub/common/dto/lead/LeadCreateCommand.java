package kz.edu.soccerhub.common.dto.lead;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kz.edu.soccerhub.crm.domain.model.enums.LeadType;

import java.util.List;
import java.util.UUID;

public record LeadCreateCommand(
        @NotNull(message = "Lead type is required")
        LeadType leadType,

        @Valid
        @NotNull(message = "Primary contact is required")
        LeadPrimaryContactInput primaryContact,

        @Size(max = 1000, message = "Comment is too long")
        String comment,

        UUID assignedAdminId,

        @NotNull(message = "Branch id is required")
        UUID branchId,

        @Valid
        @NotEmpty(message = "At least one participant is required")
        List<LeadParticipantInput> participants
) {}
