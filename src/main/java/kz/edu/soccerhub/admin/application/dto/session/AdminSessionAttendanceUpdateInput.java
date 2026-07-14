package kz.edu.soccerhub.admin.application.dto.session;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import kz.edu.soccerhub.coach.domain.model.enums.TrainingSessionAttendanceStatus;

import java.util.List;
import java.util.UUID;

public record AdminSessionAttendanceUpdateInput(
        @Valid
        @NotNull
        @NotEmpty
        List<Entry> entries
) {
    public record Entry(
            @NotNull UUID playerId,
            @NotNull TrainingSessionAttendanceStatus status,
            String comment
    ) {}
}
