package kz.edu.soccerhub.admin.api.dto;

import jakarta.validation.constraints.*;
import kz.edu.soccerhub.crm.domain.model.enums.TimePreference;
import lombok.Builder;
import java.util.List;

@Builder
public record AdminLeadPreferencesInput(
        @NotNull @PositiveOrZero Long version,
        @NotNull @Size(max = 7) List<@NotNull @Pattern(regexp = "MON|TUE|WED|THU|FRI|SAT|SUN") String> preferredDays,
        TimePreference timePreference,
        @Pattern(regexp = "BEGINNER|INTERMEDIATE|ADVANCED|^$") String experience,
        @Size(max = 1000) String notes) {}
