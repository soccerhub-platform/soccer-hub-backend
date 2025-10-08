package kz.edu.soccerhub.common.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

public class BadRequestException extends SoccerHubException {

    public BadRequestException(String message, String value) {
        super(message, "BAD_REQUEST", HttpStatus.BAD_REQUEST, Map.of("value", value));
    }

}
