package com.notificationservice.serviceTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.notificationservice.entity.Notification;
import com.notificationservice.repository.NotificationRepository;
import com.notificationservice.service.EmailService;
import com.notificationservice.serviceImpl.NotificationServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Arrays;
import java.util.List;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceImplTest {

	@Mock
	private NotificationRepository repository;

	@Mock
	private SimpMessagingTemplate messagingTemplate;

	@Mock
	private EmailService emailService;

	@InjectMocks
	private NotificationServiceImpl notificationService;

	private Notification testNotification;

	@BeforeEach
	void setUp() {
		testNotification = Notification.builder().recipientId(101).senderName("System").message("New Comment Added")
				.type("COMMENT_ADDED").build();
	}

	@Test
	void testSendProjectNotification_FullFlow() {
		// Arrange
		List<String> emails = Arrays.asList("member1@gmail.com", "member2@gmail.com");
		when(repository.save(any(Notification.class))).thenReturn(testNotification);

		// Act
		Notification result = notificationService.sendProjectNotification(testNotification, emails);

		// Assert
		assertNotNull(result);

		// Verify Database Save
		verify(repository, times(1)).save(testNotification);

		// Verify WebSocket Push
		verify(messagingTemplate, times(1)).convertAndSendToUser(eq("101"), eq("/queue/notifications"),
				any(Notification.class));

		// Verify Email Dispatch (Updated for HTML Email method)
		verify(emailService, times(1)).sendHtmlEmail(eq(emails), anyString(), anyString(), anyBoolean());
	}

	@Test
	void testSendProjectNotification_NoEmails() {
		// Arrange
		when(repository.save(any(Notification.class))).thenReturn(testNotification);

		// Act
		notificationService.sendProjectNotification(testNotification, null);

		// Assert (Updated for HTML Email method)
		verify(emailService, never()).sendHtmlEmail(any(), anyString(), anyString(), anyBoolean());
	}
}
