package kz.edu.soccerhub.common.dto.analytics;

public record LeadTimingOutput(
        SlaPercentileOutput firstContactMinutes,
        SlaPercentileOutput qualificationHours,
        SlaPercentileOutput trialScheduledHours
) {
}

