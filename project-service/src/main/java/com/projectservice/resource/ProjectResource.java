package com.projectservice.resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.projectservice.entity.Project;
import com.projectservice.entity.ProjectMember;
import com.projectservice.service.ProjectService;
import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectResource {

	@Autowired
	private ProjectService projectService;

	@PostMapping
	public ResponseEntity<Project> createProject(@RequestBody Project project) {
		// Requirement: Every project must have an owner and a language
		return ResponseEntity.ok(projectService.createProject(project));
	}

	@GetMapping("/owner/{ownerId}")
	public ResponseEntity<List<Project>> getByOwner(@PathVariable int ownerId) {
		return ResponseEntity.ok(projectService.getProjectsByOwner(ownerId));
	}

	@GetMapping("/public")
	public ResponseEntity<List<Project>> getPublic(@RequestParam int currentUserId) {
		// Fetches community projects with 'isStarredByMe' logic applied
		return ResponseEntity.ok(projectService.getPublicProjects(currentUserId));
	}

	@PutMapping("/{id}/star")
	public ResponseEntity<Void> toggleStar(@PathVariable int id, @RequestParam int userId) {
		projectService.starProject(id, userId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/search")
	public ResponseEntity<List<Project>> search(@RequestParam String name, @RequestParam int userId) {
		return ResponseEntity.ok(projectService.searchProjects(name, userId));
	}

	@GetMapping("/{id}")
	public ResponseEntity<Project> getById(@PathVariable int id) {
		// Vital for the EditorPage to load project metadata (Language, Name)
		return ResponseEntity.ok(projectService.getProjectById(id));
	}

	@PostMapping("/{id}/fork")
	public ResponseEntity<Project> fork(@PathVariable int id, @RequestParam int userId, @RequestParam String username) {
		return ResponseEntity.ok(projectService.forkProject(id, userId, username));
	}

	@PutMapping("/{id}")
	public ResponseEntity<Project> updateProject(@PathVariable int id, @RequestBody Project project) {
		return ResponseEntity.ok(projectService.updateProject(id, project));
	}

	// --- COLLABORATION ENDPOINTS ---

	@PostMapping("/{projectId}/members/request")
	public ResponseEntity<Void> requestAccess(@PathVariable int projectId, @RequestParam int userId,
			@RequestParam String username) {
		projectService.requestCollaboration(projectId, userId, username);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/{projectId}/members/approve")
	public ResponseEntity<Void> approveAccess(@PathVariable int projectId, @RequestParam int userId) {
		projectService.approveCollaborator(projectId, userId);
		return ResponseEntity.ok().build();
	}

	@GetMapping("/{projectId}/access")
	public ResponseEntity<Boolean> checkEditAccess(@PathVariable int projectId, @RequestParam int userId) {
		// Gatekeeper check: Does this user have 'EDITOR' role or is 'OWNER'?
		return ResponseEntity.ok(projectService.hasEditAccess(projectId, userId));
	}

	@GetMapping("/{projectId}/requests")
	public ResponseEntity<List<ProjectMember>> getPendingRequests(@PathVariable int projectId) {
		return ResponseEntity.ok(projectService.getPendingRequests(projectId));
	}

	@GetMapping("/{projectId}/members")
	public ResponseEntity<List<ProjectMember>> getMembers(@PathVariable int projectId) {
		return ResponseEntity.ok(projectService.getProjectMembers(projectId));
	}

	@DeleteMapping("/{projectId}/members/{userId}")
	public ResponseEntity<Void> removeMember(@PathVariable int projectId, @PathVariable int userId) {
		projectService.removeProjectMember(projectId, userId);
		return ResponseEntity.noContent().build();
	}

	@PutMapping("/{id}/archive")
	public ResponseEntity<Void> archive(@PathVariable int id) {
		projectService.archiveProject(id);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable int id) {
		projectService.deleteProject(id);
		return ResponseEntity.noContent().build();
	}
}