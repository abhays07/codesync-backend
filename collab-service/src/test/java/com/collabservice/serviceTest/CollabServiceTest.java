package com.collabservice.serviceTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.List;
import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.collabservice.entity.CollabSession;
import com.collabservice.entity.Participant;
import com.collabservice.repository.CollabRepository;
import com.collabservice.repository.ParticipantRepository;
import com.collabservice.serviceImpl.CollabServiceImpl;

@ExtendWith(MockitoExtension.class)
public class CollabServiceTest {

	@Mock
	private CollabRepository collabRepo;

	@Mock
	private ParticipantRepository participantRepo;

	@InjectMocks
	private CollabServiceImpl collabService;

	private CollabSession sampleSession;
	private Participant sampleParticipant;

	@BeforeEach
	void setUp() {
		sampleSession = new CollabSession();
		sampleSession.setProjectId(101);
		sampleSession.setFileId(202);

		sampleParticipant = new Participant();
		sampleParticipant.setUserId(1);
		sampleParticipant.setSessionId("session-uuid");
	}

	@Test
	void testCreateSession_ShouldReturnActiveSession() {
		// Arrange
		when(collabRepo.save(any(CollabSession.class))).thenReturn(sampleSession);

		// Act
		CollabSession created = collabService.createSession(sampleSession);

		// Assert
		assertNotNull(created.getSessionId());
		assertEquals("ACTIVE", created.getStatus());
		verify(collabRepo, times(1)).save(any(CollabSession.class));
	}

	@Test
	void testJoinSession_ShouldAssignColorBasedOnCount() {
		// Arrange
		String sessionId = "test-session";
		// Mock count as 1, so the index should be 1 (second color in the palette)
		when(participantRepo.countBySessionId(sessionId)).thenReturn(1L);
		when(participantRepo.save(any(Participant.class))).thenAnswer(i -> i.getArguments()[0]);

		// Act
		Participant p = collabService.joinSession(sessionId, 5, "EDITOR");

		// Assert
		assertEquals("#06B6D4", p.getColor()); // The second color in your COLORS array
		assertEquals(5, p.getUserId());
		verify(participantRepo).save(any(Participant.class));
	}

	@Test
	void testUpdateCursor_ShouldUpdateCoordinatesInList() {
		// Arrange
		String sessionId = "test-session";
		int userId = 1;
		List<Participant> participants = new ArrayList<>();
		participants.add(sampleParticipant);

		when(participantRepo.findBySessionId(sessionId)).thenReturn(participants);

		// Act
		collabService.updateCursor(sessionId, userId, 15, 40);

		// Assert
		assertEquals(15, sampleParticipant.getCursorLine());
		assertEquals(40, sampleParticipant.getCursorCol());
		verify(participantRepo).save(sampleParticipant);
	}

	@Test
	void testLeaveSession_ShouldInvokeDelete() {
		// Act
		collabService.leaveSession("session-id", 1);

		// Assert
		verify(participantRepo, times(1)).deleteBySessionIdAndUserId("session-id", 1);
	}

	@Test
	void testEndSession_ShouldMarkStatusAsEnded() {
		// Arrange
		String sid = "uuid-123";
		when(collabRepo.findById(sid)).thenReturn(Optional.of(sampleSession));

		// Act
		collabService.endSession(sid);

		// Assert
		assertEquals("ENDED", sampleSession.getStatus());
		assertNotNull(sampleSession.getEndedAt());
		verify(collabRepo).save(sampleSession);
	}
}