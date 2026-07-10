package kz.edu.soccerhub.common.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

public class ConflictException extends SoccerHubException {

    public ConflictException(String message, String errorCode, Map<String, Object> metadata) {
        super(message, errorCode, HttpStatus.CONFLICT, metadata);
    }
}
