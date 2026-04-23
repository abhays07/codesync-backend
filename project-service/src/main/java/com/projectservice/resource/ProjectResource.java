package com.projectservice.resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.projectservice.entity.Project;
import com.projectservice.service.ProjectService;
import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectResource {

	@Autowired
	private ProjectService projectService;

	@PostMapping
	public ResponseEntity<Project> create(@RequestBody Project project) {
		return ResponseEntity.ok(projectService.createProject(project));
	}

	@GetMapping("/owner/{ownerId}")
	public ResponseEntity<List<Project>> getByOwner(@PathVariable int ownerId) {
		return ResponseEntity.ok(projectService.getProjectsByOwner(ownerId));
	}

	@GetMapping("/public")
	public ResponseEntity<List<Project>> getPublic(@RequestParam int currentUserId) {
		return ResponseEntity.ok(projectService.getPublicProjects(currentUserId));
	}

	@PutMapping("/{id}/star")
	public ResponseEntity<Void> star(@PathVariable int id, @RequestParam int userId) {
		projectService.starProject(id, userId);
		return ResponseEntity.ok().build();
	}

	@GetMapping("/search")
	public ResponseEntity<List<Project>> search(@RequestParam String name, @RequestParam int userId) {
		return ResponseEntity.ok(projectService.searchProjects(name, userId));
	}

	@GetMapping("/language/{language}")
	public ResponseEntity<List<Project>> getByLanguage(@PathVariable String language) {
		return ResponseEntity.ok(projectService.getProjectsByLanguage(language));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable int id) {
		projectService.deleteProject(id);
		return ResponseEntity.ok().build();
	}

	@PutMapping("/{id}/archive")
	public ResponseEntity<Void> archive(@PathVariable int id) {
		projectService.archiveProject(id);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/{id}/fork")
	public ResponseEntity<Project> fork(@PathVariable int id, @RequestParam int userId) {
		return ResponseEntity.ok(projectService.forkProject(id, userId));
	}

	@GetMapping("/{id}")
	public ResponseEntity<Project> getById(@PathVariable int id) {
		// This provides the project data for the EditorPage
		return ResponseEntity.ok(projectService.getProjectById(id));
	}
}