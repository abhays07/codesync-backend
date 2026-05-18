package com.collabservice.resourceTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.collabservice.entity.CollabSession;
import com.collabservice.entity.Participant;
import com.collabservice.resource.CollabResource;
import com.collabservice.service.CollabService;

@ExtendWith(MockitoExtension.class)
class CollabResourceTest {

    private MockMvc mockMvc;

    @Mock
    private CollabService collabService;

    @InjectMocks
    private CollabResource collabResource;

    private ObjectMapper objectMapper;
    private CollabSession session;
    private Participant participant;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(collabResource).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

        session = new CollabSession();
        session.setSessionId("session-uuid");
        session.setProjectId(1);
        session.setFileId(2);
        session.setStatus("ACTIVE");

        participant = new Participant();
        participant.setSessionId("session-uuid");
        participant.setUserId(10);
        participant.setRole("EDITOR");
        participant.setColor("#F97316");
    }

    @Test
    void testCreateSession_Success() throws Exception {
        when(collabService.createSession(any(CollabSession.class))).thenReturn(session);

        mockMvc.perform(post("/api/v1/sessions/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("session-uuid"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void testGetSession_Success() throws Exception {
        when(collabService.getSessionById("session-uuid")).thenReturn(Optional.of(session));

        mockMvc.perform(get("/api/v1/sessions/session-uuid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("session-uuid"));
    }

    @Test
    void testGetSession_NotFound() throws Exception {
        when(collabService.getSessionById("unknown-id")).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> {
            mockMvc.perform(get("/api/v1/sessions/unknown-id"));
        });
    }

    @Test
    void testJoinSession_Success() throws Exception {
        when(collabService.joinSession("session-uuid", 10, "EDITOR")).thenReturn(participant);

        mockMvc.perform(post("/api/v1/sessions/session-uuid/join")
                .param("userId", "10")
                .param("role", "EDITOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(10))
                .andExpect(jsonPath("$.color").value("#F97316"));
    }

    @Test
    void testUpdateCursor_Success() throws Exception {
        Map<String, Object> coords = new HashMap<>();
        coords.put("userId", 10);
        coords.put("line", 15);
        coords.put("col", 40);

        doNothing().when(collabService).updateCursor("session-uuid", 10, 15, 40);

        mockMvc.perform(put("/api/v1/sessions/session-uuid/cursor")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(coords)))
                .andExpect(status().isNoContent());

        verify(collabService, times(1)).updateCursor("session-uuid", 10, 15, 40);
    }

    @Test
    void testGetActiveParticipants_Success() throws Exception {
        List<Participant> list = List.of(participant);
        when(collabService.getParticipants("session-uuid")).thenReturn(list);

        mockMvc.perform(get("/api/v1/sessions/session-uuid/participants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(10));
    }

    @Test
    void testLeaveSession_Success() throws Exception {
        doNothing().when(collabService).leaveSession("session-uuid", 10);

        mockMvc.perform(delete("/api/v1/sessions/session-uuid/leave")
                .param("userId", "10"))
                .andExpect(status().isNoContent());

        verify(collabService, times(1)).leaveSession("session-uuid", 10);
    }
}
