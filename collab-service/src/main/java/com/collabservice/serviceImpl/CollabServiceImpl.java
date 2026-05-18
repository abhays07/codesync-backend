package com.collabservice.serviceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.collabservice.entity.*;
import com.collabservice.repository.*;
import com.collabservice.service.CollabService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class CollabServiceImpl implements CollabService {

	@Autowired
	private CollabRepository collabRepo;

	@Autowired
	private ParticipantRepository participantRepo;

	@Autowired
	private SimpMessagingTemplate messagingTemplate;

	// Vibrant colors for remote cursors in the editor
	private final String[] COLORS = { "#F97316", "#06B6D4", "#8B5CF6", "#EC4899", "#10B981", "#EAB308" };

	@Override
	@Transactional
	public CollabSession createSession(CollabSession session) {
		// Fix: Check if an active session already exists for this file
		Optional<CollabSession> existingSession = collabRepo.findFirstByFileIdAndStatus(session.getFileId(), "ACTIVE");
		if (existingSession.isPresent()) {
			return existingSession.get();
		}

		session.setSessionId(UUID.randomUUID().toString());
		session.setStatus("ACTIVE");
		session.setCreatedAt(LocalDateTime.now());
		return collabRepo.save(session);
	}

	@Override
	@Transactional
	public Participant joinSession(String sessionId, int userId, String role) {
		// Logic: Ensure session exists before joining
		CollabSession session = collabRepo.findById(sessionId)
				.orElseThrow(() -> new com.collabservice.exception.CollabException("Join Failed: Session not found or expired"));
		if (session.getSessionId() == null) {
			throw new com.collabservice.exception.CollabException("Join Failed: Invalid Session ID");
		}

		// Fix: Prevent duplicate participant entries if they reconnect
		Optional<Participant> existing = participantRepo.findBySessionId(sessionId)
				.stream().filter(p -> p.getUserId() == userId).findFirst();
		if (existing.isPresent()) {
			return existing.get();
		}

		// Assign a rotating color based on current participant count
		long count = participantRepo.countBySessionId(sessionId);
		String color = COLORS[(int) (count % COLORS.length)];

		Participant participant = new Participant();
		participant.setSessionId(sessionId);
		participant.setUserId(userId);
		participant.setRole(role);
		participant.setColor(color);
		participant.setJoinedAt(LocalDateTime.now());

		Participant savedParticipant = participantRepo.save(participant);

		Map<String, Object> payload = new HashMap<>();
		payload.put("type", "PARTICIPANT_JOIN");
		payload.put("userId", userId);
		messagingTemplate.convertAndSend("/topic/session/" + sessionId, payload);

		return savedParticipant;
	}

	@Override
	@Transactional
	public void leaveSession(String sessionId, int userId) {
		participantRepo.deleteBySessionIdAndUserId(sessionId, userId);
		
		Map<String, Object> payload = new HashMap<>();
		payload.put("type", "PARTICIPANT_LEAVE");
		payload.put("userId", userId);
		messagingTemplate.convertAndSend("/topic/session/" + sessionId, payload);
	}

	@Override
	@Transactional
	public void updateCursor(String sessionId, int userId, int line, int col) {
		// Updates the cursor coordinates for live synchronization
		participantRepo.findBySessionId(sessionId).stream().filter(p -> p.getUserId() == userId).findFirst()
				.ifPresent(p -> {
					p.setCursorLine(line);
					p.setCursorCol(col);
					participantRepo.save(p);

					Map<String, Object> payload = new HashMap<>();
					payload.put("type", "CURSOR_UPDATE");
					payload.put("userId", userId);
					payload.put("line", line);
					payload.put("col", col);
					payload.put("username", p.getUsername());
					payload.put("color", p.getColor());
					messagingTemplate.convertAndSend("/topic/session/" + sessionId, payload);
				});
	}

	@Override
	@Transactional
	public void endSession(String sessionId) {
		collabRepo.findById(sessionId).ifPresent(s -> {
			s.setStatus("ENDED");
			s.setEndedAt(LocalDateTime.now());
			collabRepo.save(s);
		});
	}

	@Override
	public Optional<CollabSession> getSessionById(String id) {
		return collabRepo.findById(id);
	}

	@Override
	public List<CollabSession> getSessionsByProject(int id) {
		return collabRepo.findByProjectId(id);
	}

	@Override
	public List<Participant> getParticipants(String id) {
		return participantRepo.findBySessionId(id);
	}
}