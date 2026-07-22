package kz.edu.soccerhub.admin.application.dto.client;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import kz.edu.soccerhub.common.dto.client.ClientStudentRelationshipType;

import java.time.LocalDate;

public record AdminCreateClientStudentInput(
        @NotBlank String firstName,
        String lastName,
        @NotNull @Past LocalDate birthDate,
        @NotNull ClientStudentRelationshipType relationshipType,
        boolean primaryContact,
        boolean primaryPayer,
        boolean legalRepresentative,
        boolean receivesNotifications,
        @NotNull LocalDate startedAt
) {
}
