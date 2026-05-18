package com.collabservice.exceptionTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.collabservice.exception.CollabException;
import com.collabservice.exception.GlobalExceptionHandler;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void testHandleCollabException() {
        CollabException ex = new CollabException("Custom collab error");
        ResponseEntity<Map<String, String>> response = handler.handleCollabException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Custom collab error", response.getBody().get("message"));
    }

    @Test
    void testHandleRuntimeException() {
        RuntimeException ex = new RuntimeException("Runtime error");
        ResponseEntity<Map<String, String>> response = handler.handleRuntimeException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Runtime error", response.getBody().get("message"));
    }

    @Test
    void testHandleGeneralException() {
        Exception ex = new Exception("General error");
        ResponseEntity<Map<String, String>> response = handler.handleGeneralException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Real-time sync error. Please refresh your editor.", response.getBody().get("message"));
    }
}
