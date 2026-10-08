package kz.edu.soccerhub.common.exception.advice;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void accessDeniedIs403NotAnInternalError() {
        var response = handler.handleAccessDenied(new AccessDeniedException("private branch"));
        assertEquals(403, response.getStatusCode().value());
        assertEquals("FORBIDDEN", ((Map<?, ?>) response.getBody()).get("code"));
        assertFalse(response.getBody().toString().contains("private branch"));
    }

    @Test
    void genericErrorsDoNotExposeInternalDetails() {
        var response = handler.handleGeneric(new RuntimeException("database host and credentials"));
        assertEquals(500, response.getStatusCode().value());
        assertFalse(response.getBody().toString().contains("credentials"));
    }
}
