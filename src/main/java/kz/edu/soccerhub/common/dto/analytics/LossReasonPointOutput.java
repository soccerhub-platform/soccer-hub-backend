package kz.edu.soccerhub.common.dto.analytics;

public record LossReasonPointOutput(
        String lostReasonCode,
        String name,
        long count,
        double share,
        double trend
) {
}
