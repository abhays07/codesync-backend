package com.collabservice.resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.collabservice.entity.CollabSession;
import com.collabservice.entity.Participant;
import com.collabservice.service.CollabService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/")
public class CollabResource {

	@Autowired
	private CollabService collabService;

	@PostMapping("/create")
	public ResponseEntity<CollabSession> createSession(@RequestBody CollabSession session) {
		return ResponseEntity.ok(collabService.createSession(session));
	}

	@GetMapping("/{sessionId}")
	public ResponseEntity<CollabSession> getById(@PathVariable String sessionId) {
		return collabService.getSessionById(sessionId).map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
	}

	@PostMapping("/{sessionId}/join")
	public ResponseEntity<Participant> join(@PathVariable String sessionId, @RequestParam int userId,
			@RequestParam String role) {
		return ResponseEntity.ok(collabService.joinSession(sessionId, userId, role));
	}

	@DeleteMapping("/{sessionId}/leave")
	public ResponseEntity<Void> leave(@PathVariable String sessionId, @RequestParam int userId) {
		collabService.leaveSession(sessionId, userId);
		return ResponseEntity.ok().build();
	}

	@PutMapping("/{sessionId}/cursor")
	public ResponseEntity<Void> updateCursor(@PathVariable String sessionId, @RequestBody Map<String, Integer> coords) {
		collabService.updateCursor(sessionId, coords.get("userId"), coords.get("line"), coords.get("col"));
		return ResponseEntity.ok().build();
	}

	@GetMapping("/{sessionId}/participants")
	public ResponseEntity<List<Participant>> getParticipants(@PathVariable String sessionId) {
		return ResponseEntity.ok(collabService.getParticipants(sessionId));
	}

	@PostMapping("/{sessionId}/end")
	public ResponseEntity<Void> endSession(@PathVariable String sessionId) {
		collabService.endSession(sessionId);
		return ResponseEntity.ok().build();
	}
}