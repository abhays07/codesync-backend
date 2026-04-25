package com.notificationservice.serviceImpl;

import com.notificationservice.entity.Notification;

import com.notificationservice.repository.NotificationRepository;
import com.notificationservice.service.EmailService;
import com.notificationservice.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

	@Autowired
	private NotificationRepository repository;

	@Autowired
	private SimpMessagingTemplate messagingTemplate;

	@Autowired
	private EmailService emailService;

	@Override
	public Notification sendProjectNotification(Notification notification, List<String> memberEmails) {
		// 1. Persist the record in the database for the history tab
		Notification saved = repository.save(notification);

		// 2. Real-time push via WebSocket to the recipient's UI
		messagingTemplate.convertAndSendToUser(String.valueOf(notification.getRecipientId()), "/queue/notifications",
				saved);

		// 3. Email Dispatch logic
		if (memberEmails != null && !memberEmails.isEmpty()) {

			String msg = notification.getMessage(); // e.g. "Hey shifa, your request to join TIT Syllabus has been
													// Approved."
			boolean isApproved = msg.toLowerCase().contains("approved");

			// Extract variables directly from the message!
			String requesterName = "Developer";
			String projectName = "Workspace Project";

			try {
				if (msg.startsWith("Hey ")) {
					int commaIdx = msg.indexOf(",");
					if (commaIdx != -1) {
						requesterName = msg.substring(4, commaIdx);
					}
				}

				int joinIdx = msg.indexOf("join ");
				int hasBeenIdx = msg.indexOf(" has been");
				if (joinIdx != -1 && hasBeenIdx != -1 && hasBeenIdx > joinIdx) {
					projectName = msg.substring(joinIdx + 5, hasBeenIdx);
				}
			} catch (Exception e) {
				System.err.println("Failed to parse names for email. Using defaults.");
			}

			// Call your amazing new Email Service!
			emailService.sendHtmlEmail(memberEmails, requesterName, projectName, isApproved);
		}

		return saved;
	}

	@Override
	public List<Notification> getUserNotifications(Integer userId) {
		return repository.findByRecipientIdOrderByCreatedAtDesc(userId);
	}

	@Override
	public void markAsRead(Long notificationId) {
		repository.findById(notificationId).ifPresent(n -> {
			n.setRead(true);
			repository.save(n);
		});
	}

	@Override
	public void markAllAsRead(Integer userId) {
		List<Notification> unread = repository.findByRecipientId(userId);
		unread.forEach(n -> n.setRead(true));
		repository.saveAll(unread);
	}

	@Override
	public void deleteNotification(Long id) {
		repository.deleteById(id);
	}

	@Override
	public void clearAllNotifications(Integer userId) {
		List<Notification> all = repository.findByRecipientId(userId);
		repository.deleteAll(all);
	}

}