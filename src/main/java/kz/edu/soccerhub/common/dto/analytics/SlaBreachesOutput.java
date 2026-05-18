package kz.edu.soccerhub.common.dto.analytics;

public record SlaBreachesOutput(
        double firstContactOver2hRate,
        double qualificationOver48hRate
) {
}

