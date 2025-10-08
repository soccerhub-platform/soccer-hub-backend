package kz.edu.soccerhub.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Map;

@Getter
public abstract class SoccerHubException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;
    private final Map<String, Object> metadata;

    protected SoccerHubException(String message, String errorCode, HttpStatus httpStatus) {
        this(message, errorCode, httpStatus, null);
    }

    protected  SoccerHubException(String message, String errorCode, HttpStatus httpStatus, Map<String, Object> metadata) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.metadata = metadata;
    }

}
