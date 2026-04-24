package com.collabservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.collabservice.entity.Participant;

import java.util.List;

@Repository
public interface ParticipantRepository extends JpaRepository<Participant, Long> {
	List<Participant> findBySessionId(String sessionId);

	void deleteBySessionIdAndUserId(String sessionId, int userId);

	long countBySessionId(String sessionId);
}