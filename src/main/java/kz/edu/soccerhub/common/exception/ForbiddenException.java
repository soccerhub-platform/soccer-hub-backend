package kz.edu.soccerhub.common.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends SoccerHubException {

    public ForbiddenException(String message) {
        super(message, "FORBIDDEN", HttpStatus.FORBIDDEN);
    }
}
