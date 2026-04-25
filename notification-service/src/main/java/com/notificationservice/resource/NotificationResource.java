package com.notificationservice.resource;

import com.notificationservice.entity.Notification;
import com.notificationservice.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/")
public class NotificationResource {

	@Autowired
	private NotificationService notificationService;

	@PostMapping("/send")
	public ResponseEntity<Notification> send(@RequestBody Notification notification,
			@RequestParam(required = false) List<String> emails) {
		return ResponseEntity.ok(notificationService.sendProjectNotification(notification, emails));
	}

	@GetMapping("/user/{userId}")
	public ResponseEntity<List<Notification>> get(@PathVariable Integer userId) {
		return ResponseEntity.ok(notificationService.getUserNotifications(userId));
	}

	@PatchMapping("/read/{id}")
	public ResponseEntity<Void> markRead(@PathVariable Long id) {
		notificationService.markAsRead(id);
		return ResponseEntity.ok().build();
	}

	@PatchMapping("/read-all/{userId}")
	public ResponseEntity<Void> markAllRead(@PathVariable Integer userId) {
		notificationService.markAllAsRead(userId);
		return ResponseEntity.ok().build();
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteNotification(@PathVariable Long id) {
		notificationService.deleteNotification(id);
		return ResponseEntity.ok().build();
	}

	@DeleteMapping("/user/{userId}")
	public ResponseEntity<Void> clearAllNotifications(@PathVariable Integer userId) {
		notificationService.clearAllNotifications(userId);
		return ResponseEntity.ok().build();
	}

}