package kz.edu.soccerhub.common.dto.lead;

import kz.edu.soccerhub.crm.domain.model.enums.TimePreference;
import lombok.Builder;
import java.util.List;

@Builder
public record LeadPreferencesCommand(Long version, List<String> preferredDays,
        TimePreference timePreference, String experience, String notes) {}
