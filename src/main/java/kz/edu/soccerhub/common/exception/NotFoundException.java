package kz.edu.soccerhub.common.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

public class NotFoundException extends SoccerHubException {

    public NotFoundException(String message, Object value) {
        super(message, "NOT_FOUND", HttpStatus.NOT_FOUND, Map.of("value", value));
    }

}
