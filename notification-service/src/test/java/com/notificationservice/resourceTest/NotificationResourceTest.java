package com.notificationservice.resourceTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.ArrayList;
import java.util.List;

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
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.notificationservice.entity.Notification;
import com.notificationservice.resource.NotificationResource;
import com.notificationservice.service.NotificationService;

@ExtendWith(MockitoExtension.class)
class NotificationResourceTest {

    private MockMvc mockMvc;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationResource notificationResource;

    private ObjectMapper objectMapper;
    private Notification notification;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(notificationResource).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        notification = new Notification();
        notification.setId(100L);
        notification.setRecipientId(1);
        notification.setMessage("Unit testing notification");
        notification.setType("INFO");
        notification.setRead(false);
    }

    @Test
    void testDispatchNotification_Success() throws Exception {
        when(notificationService.sendProjectNotification(any(Notification.class), any())).thenReturn(notification);

        mockMvc.perform(post("/api/v1/notifications/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(notification)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Unit testing notification"));
    }

    @Test
    void testGetNotifications_Success() throws Exception {
        when(notificationService.getUserNotifications(1)).thenReturn(List.of(notification));

        mockMvc.perform(get("/api/v1/notifications/user/1"))
                .andExpect(status().isOk());
    }

    @Test
    void testMarkAsRead_Success() throws Exception {
        doNothing().when(notificationService).markAsRead(100L);

        mockMvc.perform(patch("/api/v1/notifications/read/100"))
                .andExpect(status().isNoContent());
    }

    @Test
    void testMarkAllAsRead_Success() throws Exception {
        doNothing().when(notificationService).markAllAsRead(1);

        mockMvc.perform(patch("/api/v1/notifications/read-all/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void testDeleteNotification_Success() throws Exception {
        doNothing().when(notificationService).deleteNotification(100L);

        mockMvc.perform(delete("/api/v1/notifications/100"))
                .andExpect(status().isNoContent());
    }

    @Test
    void testClearHistory_Success() throws Exception {
        doNothing().when(notificationService).clearAllNotifications(1);

        mockMvc.perform(delete("/api/v1/notifications/user/1"))
                .andExpect(status().isNoContent());
    }
}
