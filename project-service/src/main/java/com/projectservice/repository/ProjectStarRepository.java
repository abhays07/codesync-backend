package com.projectservice.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.projectservice.entity.ProjectStar;

public interface ProjectStarRepository extends JpaRepository<ProjectStar, Long> {
	Optional<ProjectStar> findByProjectIdAndUserId(int projectId, int userId);

	boolean existsByProjectIdAndUserId(int projectId, int userId);

	@Query(value = "SELECT project_id FROM project_stars WHERE user_id = :userId AND project_id IN (:projectIds)", nativeQuery = true)
	Set<Integer> findStarredProjectIds(@Param("projectIds") List<Integer> projectIds, @Param("userId") int userId);
}