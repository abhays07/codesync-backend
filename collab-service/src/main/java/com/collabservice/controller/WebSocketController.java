package com.collabservice.controller;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.util.Map;

@Controller
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/session/{sessionId}")
    public void broadcastSessionEvent(@DestinationVariable String sessionId, Map<String, Object> payload) {
        // Forward code updates or any other direct STOMP messages to all subscribers
        messagingTemplate.convertAndSend("/topic/session/" + sessionId, payload);
    }
}
