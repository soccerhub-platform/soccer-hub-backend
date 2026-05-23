package kz.edu.soccerhub.common.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.UUID;

public class AnalyticsForbiddenBranchException extends SoccerHubException {

    public AnalyticsForbiddenBranchException(UUID branchId) {
        super(
                "No access to requested branch analytics",
                "ANALYTICS_FORBIDDEN_BRANCH",
                HttpStatus.FORBIDDEN,
                Map.of("branchId", branchId)
        );
    }
}
