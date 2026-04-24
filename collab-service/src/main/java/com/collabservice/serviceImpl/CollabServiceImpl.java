package com.collabservice.serviceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.collabservice.entity.CollabSession;
import com.collabservice.entity.Participant;
import com.collabservice.repository.CollabRepository;
import com.collabservice.repository.ParticipantRepository;
import com.collabservice.service.CollabService;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class CollabServiceImpl implements CollabService {

	@Autowired
	private CollabRepository collabRepo;

	@Autowired
	private ParticipantRepository participantRepo;

	private final String[] COLORS = { "#F97316", "#06B6D4", "#8B5CF6", "#EC4899", "#10B981", "#EAB308" };

	@Override
	public CollabSession createSession(CollabSession session) {
		session.setSessionId(UUID.randomUUID().toString()); //
		session.setStatus("ACTIVE");
		session.setCreatedAt(LocalDateTime.now());
		return collabRepo.save(session);
	}

	@Override
	public Participant joinSession(String sessionId, int userId, String role) {
		// Logic: Assign a rotating unique color based on current participant count
		long count = participantRepo.countBySessionId(sessionId);
		String color = COLORS[(int) (count % COLORS.length)];

		Participant participant = new Participant();
		participant.setSessionId(sessionId);
		participant.setUserId(userId);
		participant.setRole(role);
		participant.setColor(color); 
		participant.setJoinedAt(LocalDateTime.now());

		return participantRepo.save(participant);
	}

	@Override
	@Transactional
	public void leaveSession(String sessionId, int userId) {
		participantRepo.deleteBySessionIdAndUserId(sessionId, userId);
	}

	@Override
	public void updateCursor(String sessionId, int userId, int line, int col) {
		// Real-life logic: In a high-traffic app, cursor updates are often
		// handled via WebSocket directly without constant DB writes.
		// However, per class diagram requirements, we persist coordinates.
		List<Participant> members = participantRepo.findBySessionId(sessionId);
		members.stream().filter(p -> p.getUserId() == userId).findFirst().ifPresent(p -> {
			p.setCursorLine(line);
			p.setCursorCol(col);
			participantRepo.save(p);
		});
	}

	@Override
	public void endSession(String sessionId) {
		collabRepo.findById(sessionId).ifPresent(s -> {
			s.setStatus("ENDED");
			s.setEndedAt(LocalDateTime.now());
			collabRepo.save(s);
		});
	}

	@Override
	public Optional<CollabSession> getSessionById(String sessionId) {
		return collabRepo.findById(sessionId);
	}

	@Override
	public List<CollabSession> getSessionsByProject(int projectId) {
		return collabRepo.findByProjectId(projectId);
	}

	@Override
	public List<Participant> getParticipants(String sessionId) {
		return participantRepo.findBySessionId(sessionId);
	}
}