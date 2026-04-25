package com.projectservice.serviceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.projectservice.entity.Project;
import com.projectservice.entity.ProjectMember;
import com.projectservice.entity.ProjectStar;
import com.projectservice.repository.ProjectMemberRepository;
import com.projectservice.repository.ProjectRepository;
import com.projectservice.repository.ProjectStarRepository;
import com.projectservice.service.ProjectService;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProjectServiceImpl implements ProjectService {

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private ProjectStarRepository starRepository;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private ProjectMemberRepository memberRepository;

	/**
	 * Populates the transient 'isStarredByMe' flag for UI consistency
	 */
	// Inside ProjectServiceImpl.java

	private void applyStarStatus(List<Project> projects, int userId) {
		if (projects == null || projects.isEmpty())
			return;

		// 1. Get all IDs to check
		List<Integer> projectIds = projects.stream().map(Project::getProjectId).collect(Collectors.toList());

		// 2. Query the DB (using the fixed native query from Step 1)
		Set<Integer> starredProjectIds = starRepository.findStarredProjectIds(projectIds, userId);

		// 3. Populate transient flag
		projects.forEach(p -> {
			boolean status = starredProjectIds.contains(p.getProjectId());
			p.setStarredByMe(status);
			// Console log for your debugging
			System.out.println("DEBUG: Project " + p.getProjectId() + " | StarredByMe: " + status);
		});
	}

	@Override
	public Project createProject(Project project) {
		return projectRepository.save(project);
	}

	@Override
	public List<Project> getProjectsByOwner(int ownerId) {
		List<Project> projects = projectRepository.findByOwnerId(ownerId);
		applyStarStatus(projects, ownerId);
		return projects;
	}

	@Override
	public List<Project> getPublicProjects(int currentUserId) {
		List<Project> projects = projectRepository.findByVisibility("PUBLIC");
		applyStarStatus(projects, currentUserId);
		return projects;
	}

	@Override
	public List<Project> searchProjects(String name, int currentUserId) {
		List<Project> projects = projectRepository.findByNameContainingIgnoreCase(name);
		applyStarStatus(projects, currentUserId);
		return projects;
	}

	@Override
	@Transactional
	public void starProject(int projectId, int userId) {
		Project p = getProjectById(projectId);
		Optional<ProjectStar> existingStar = starRepository.findByProjectIdAndUserId(projectId, userId);

		if (existingStar.isPresent()) {
			// UNSTAR logic
			starRepository.delete(existingStar.get());
			p.setStarCount(Math.max(0, p.getStarCount() - 1));
		} else {
			// STAR logic
			ProjectStar newStar = new ProjectStar();
			newStar.setProjectId(projectId);
			newStar.setUserId(userId);
			starRepository.save(newStar);
			p.setStarCount(p.getStarCount() + 1);
		}
		projectRepository.save(p);
	}

	@Override
	public Project getProjectById(int projectId) {
		return projectRepository.findById(projectId).orElseThrow(() -> new RuntimeException("Project not found"));
	}

	@Override
	public void archiveProject(int projectId) {
		Project p = getProjectById(projectId);
		p.setArchived(true);
		projectRepository.save(p);
	}

	@Override
	public List<Project> getProjectsByLanguage(String language) {
		return projectRepository.findByLanguage(language);
	}

	@Override
	public Project updateProject(int projectId, Project projectDetails) {
		Project project = getProjectById(projectId);
		project.setName(projectDetails.getName());
		project.setDescription(projectDetails.getDescription());
		project.setVisibility(projectDetails.getVisibility());
		project.setUpdatedAt(LocalDateTime.now());
		return projectRepository.save(project);
	}

	@Override
	public void deleteProject(int projectId) {
		projectRepository.deleteById(projectId);
	}

	@Override
	@Transactional
	public Project forkProject(int sourceId, int newOwnerId) {
		Project source = getProjectById(sourceId);
		if (!"PUBLIC".equals(source.getVisibility())) {
			throw new RuntimeException("Only public projects can be forked.");
		}

		// 1. Prepare and save the metadata first to generate the new projectId
		Project forked = new Project();
		forked.setName(source.getName() + "-fork");
		forked.setDescription("Forked from " + source.getName());
		forked.setOwnerId(newOwnerId);
		forked.setLanguage(source.getLanguage());
		forked.setVisibility("PRIVATE"); // Forks default to private
		forked.setCreatedAt(LocalDateTime.now());
		forked.setUpdatedAt(LocalDateTime.now());
		forked.setArchived(false);

		Project savedFork = projectRepository.save(forked);

		// 2. Trigger the File-Service to deep-clone the filesystem
		// Synchronous inter-service call using RestTemplate
		try {
			String fileServiceUrl = "http://FILE-SERVICE/api/v1/files/clone?sourceId=" + sourceId + "&targetId="
					+ savedFork.getProjectId();

			restTemplate.postForEntity(fileServiceUrl, null, Void.class);

		} catch (Exception e) {
			// Log error but allow metadata to remain or throw to rollback @Transactional
			throw new RuntimeException("File system cloning failed: " + e.getMessage());
		}

		// 3. Update original project metrics
		source.setForkCount(source.getForkCount() + 1);
		projectRepository.save(source);

		return savedFork;
	}

	@Override
	public void requestCollaboration(int projectId, int userId, String username) {
		// Check if already a member or owner
		Project project = getProjectById(projectId);
		if (project.getOwnerId() == userId)
			return;

		Optional<ProjectMember> existing = memberRepository.findByProjectIdAndUserId(projectId, userId);
		if (existing.isEmpty()) {
			ProjectMember request = new ProjectMember();
			request.setProjectId(projectId);
			request.setUserId(userId);
			request.setUsername(username);
			request.setRole("PENDING"); // Mark as pending for owner approval
			memberRepository.save(request);

			// TODO: In real-life, trigger a notification to the owner here
		}
	}

	@Override
	@Transactional
	public void approveCollaborator(int projectId, int userId) {
		ProjectMember member = memberRepository.findByProjectIdAndUserId(projectId, userId)
				.orElseThrow(() -> new RuntimeException("Request not found"));
		member.setRole("EDITOR");
		memberRepository.save(member);
	}

	@Override
	public List<ProjectMember> getPendingRequests(int projectId) {
		return memberRepository.findByProjectId(projectId).stream().filter(m -> "PENDING".equals(m.getRole()))
				.collect(Collectors.toList());
	}

	@Override
	public boolean hasEditAccess(int projectId, int userId) {
		Project project = projectRepository.findById(projectId).orElse(null);
		if (project != null && project.getOwnerId() == userId)
			return true;

		return memberRepository.existsByProjectIdAndUserIdAndRole(projectId, userId, "EDITOR");
	}

	@Override
	public List<ProjectMember> getProjectMembers(int projectId) {
		// Fetches all members and filters for those with active access (EDITOR role)
		return memberRepository.findByProjectId(projectId).stream().filter(m -> "EDITOR".equals(m.getRole()))
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public void removeProjectMember(int projectId, int userId) {
		ProjectMember member = memberRepository.findByProjectIdAndUserId(projectId, userId)
				.orElseThrow(() -> new RuntimeException("Project member not found"));

		memberRepository.delete(member);
	}

}