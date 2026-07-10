package kz.edu.soccerhub.media.domain.exception;

import kz.edu.soccerhub.common.exception.SoccerHubException;
import org.springframework.http.HttpStatus;

public class MediaValidationException extends SoccerHubException {

    public MediaValidationException(
            String message,
            String errorCode
    ) {
        super(message, errorCode, HttpStatus.BAD_REQUEST);
    }
}
