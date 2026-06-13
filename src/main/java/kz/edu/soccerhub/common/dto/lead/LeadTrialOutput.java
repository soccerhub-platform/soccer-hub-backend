package kz.edu.soccerhub.common.dto.lead;

import kz.edu.soccerhub.crm.domain.model.enums.LeadTrialStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record LeadTrialOutput(
        UUID id,
        UUID leadId,
        UUID participantId,
        UUID groupId,
        UUID coachId,
        LocalDate trialDate,
        LocalTime startTime,
        LocalTime endTime,
        String comment,
        LeadTrialStatus status
) {
}
