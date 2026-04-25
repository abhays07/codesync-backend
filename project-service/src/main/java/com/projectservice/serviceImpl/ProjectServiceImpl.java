package com.projectservice.serviceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.projectservice.entity.*;
import com.projectservice.repository.*;
import com.projectservice.service.ProjectService;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProjectServiceImpl implements ProjectService {

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private ProjectStarRepository starRepository;

	@Autowired
	private ProjectMemberRepository memberRepository;

	@Autowired
	private RestTemplate restTemplate;

	/**
	 * Populates the 'isStarredByMe' transient flag for UI heart-icons.
	 * Optimization: Uses a native query to fetch only relevant stars in one hit.
	 */
	private void applyStarStatus(List<Project> projects, int userId) {
		if (projects == null || projects.isEmpty())
			return;

		List<Integer> projectIds = projects.stream().map(Project::getProjectId).collect(Collectors.toList());
		Set<Integer> starredProjectIds = starRepository.findStarredProjectIds(projectIds, userId);

		projects.forEach(p -> p.setStarredByMe(starredProjectIds.contains(p.getProjectId())));
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
	@Transactional
	public void starProject(int projectId, int userId) {
		Project p = getProjectById(projectId);
		Optional<ProjectStar> existingStar = starRepository.findByProjectIdAndUserId(projectId, userId);

		if (existingStar.isPresent()) {
			starRepository.delete(existingStar.get());
			p.setStarCount(Math.max(0, p.getStarCount() - 1));
		} else {
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
		return projectRepository.findById(projectId)
				.orElseThrow(() -> new RuntimeException("Project ID " + projectId + " not found"));
	}

	@Override
	@Transactional
	public Project forkProject(int sourceId, int newOwnerId) {
		Project source = getProjectById(sourceId);
		if (!"PUBLIC".equals(source.getVisibility())) {
			throw new RuntimeException("Collaboration Error: Only public projects can be forked.");
		}

		Project forked = Project.builder().name(source.getName() + "-fork")
				.description("Forked from " + source.getName()).ownerId(newOwnerId).language(source.getLanguage())
				.visibility("PRIVATE").createdAt(LocalDateTime.now()).isArchived(false).build();

		Project savedFork = projectRepository.save(forked);

		// Synchronous inter-service call to File-Service for deep cloning
		try {
			String fileServiceUrl = "http://file-service/api/v1/files/clone?sourceId=" + sourceId + "&targetId="
					+ savedFork.getProjectId();
			restTemplate.postForEntity(fileServiceUrl, null, Void.class);
		} catch (Exception e) {
			throw new RuntimeException("Filesystem cloning failed for forked project: " + e.getMessage());
		}

		source.setForkCount(source.getForkCount() + 1);
		projectRepository.save(source);
		return savedFork;
	}

	@Override
	public void requestCollaboration(int projectId, int userId, String username) {
		Project project = getProjectById(projectId);
		if (project.getOwnerId() == userId)
			return;

		Optional<ProjectMember> existing = memberRepository.findByProjectIdAndUserId(projectId, userId);
		if (existing.isEmpty()) {
			ProjectMember request = new ProjectMember();
			request.setProjectId(projectId);
			request.setUserId(userId);
			request.setUsername(username);
			request.setRole("PENDING");
			memberRepository.save(request);
		}
	}

	@Override
	public boolean hasEditAccess(int projectId, int userId) {
		Project project = projectRepository.findById(projectId).orElse(null);
		if (project != null && project.getOwnerId() == userId)
			return true;
		return memberRepository.existsByProjectIdAndUserIdAndRole(projectId, userId, "EDITOR");
	}

	// Remaining methods (update, delete, archive, getMembers) remain logically same
	// but cleaned
	@Override
	public void archiveProject(int id) {
		Project p = getProjectById(id);
		p.setArchived(true);
		projectRepository.save(p);
	}

	@Override
	public void deleteProject(int id) {
		projectRepository.deleteById(id);
	}

	@Override
	public List<ProjectMember> getProjectMembers(int id) {
		return memberRepository.findByProjectId(id).stream().filter(m -> "EDITOR".equals(m.getRole()))
				.collect(Collectors.toList());
	}

	@Override
	public void approveCollaborator(int pId, int uId) {
		memberRepository.findByProjectIdAndUserId(pId, uId).ifPresent(m -> {
			m.setRole("EDITOR");
			memberRepository.save(m);
		});
	}

	@Override
	public List<ProjectMember> getPendingRequests(int id) {
		return memberRepository.findByProjectId(id).stream().filter(m -> "PENDING".equals(m.getRole()))
				.collect(Collectors.toList());
	}

	@Override
	public void removeProjectMember(int pId, int uId) {
		memberRepository.findByProjectIdAndUserId(pId, uId).ifPresent(memberRepository::delete);
	}

	@Override
	public List<Project> getProjectsByLanguage(String lang) {
		return projectRepository.findByLanguage(lang);
	}

	@Override
	public List<Project> searchProjects(String name, int uid) {
		List<Project> projects = projectRepository.findByNameContainingIgnoreCase(name);
		applyStarStatus(projects, uid);
		return projects;
	}

	@Override
	public Project updateProject(int id, Project details) {
		Project p = getProjectById(id);
		p.setName(details.getName());
		p.setDescription(details.getDescription());
		p.setVisibility(details.getVisibility());
		return projectRepository.save(p);
	}
}