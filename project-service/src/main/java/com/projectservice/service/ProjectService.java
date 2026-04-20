package com.projectservice.service;

import java.util.List;
import com.projectservice.entity.Project;

public interface ProjectService {
	Project createProject(Project project);

	List<Project> getProjectsByOwner(int ownerId);

	List<Project> getPublicProjects(int currentUserId);

	Project getProjectById(int projectId);

	Project forkProject(int sourceProjectId, int newOwnerId);

	void starProject(int projectId, int userId);

	void archiveProject(int projectId);

	List<Project> searchProjects(String name, int currentUserId);

	List<Project> getProjectsByLanguage(String language);

	Project updateProject(int projectId, Project project);

	void deleteProject(int projectId);
}