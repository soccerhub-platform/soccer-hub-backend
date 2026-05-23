package kz.edu.soccerhub.common.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

public class AnalyticsRangeTooLargeException extends SoccerHubException {

    public AnalyticsRangeTooLargeException(int maxDays, long actualDays) {
        super(
                "Analytics date range is too large",
                "ANALYTICS_RANGE_TOO_LARGE",
                HttpStatus.BAD_REQUEST,
                Map.of("maxDays", maxDays, "actualDays", actualDays)
        );
    }
}
