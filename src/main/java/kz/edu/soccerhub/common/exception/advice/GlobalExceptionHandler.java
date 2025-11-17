package kz.edu.soccerhub.common.exception.advice;

import jakarta.servlet.http.HttpServletRequest;
import kz.edu.soccerhub.common.exception.SoccerHubException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(SoccerHubException.class)
    public ResponseEntity<?> handleBaseException(SoccerHubException ex, HttpServletRequest request) {
        String traceId = MDC.get("traceId");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", ex.getErrorCode());
        body.put("message", ex.getMessage());
        body.put("path", request.getRequestURI());
        body.put("trace", traceId == null ? "N/A" : traceId);
        if (ex.getMetadata() != null) {
            body.put("details", ex.getMetadata());
        }

        return ResponseEntity.status(ex.getHttpStatus()).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneric(Exception ex) {
        log.error(ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                             .body(Map.of("error", "INTERNAL_ERROR", "message", ex.getMessage()));
    }
}