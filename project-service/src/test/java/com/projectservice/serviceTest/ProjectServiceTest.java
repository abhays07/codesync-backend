package com.projectservice.serviceTest;

import com.projectservice.entity.Project;
import com.projectservice.entity.ProjectMember;
import com.projectservice.entity.ProjectStar;
import com.projectservice.repository.ProjectMemberRepository;
import com.projectservice.repository.ProjectRepository;
import com.projectservice.repository.ProjectStarRepository;
import com.projectservice.serviceImpl.ProjectServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectStarRepository starRepository;

    @Mock
    private ProjectMemberRepository memberRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ProjectServiceImpl projectService;

    private Project mockProject;

    @BeforeEach
    void setUp() {
        mockProject = new Project();
        mockProject.setProjectId(1);
        mockProject.setName("Test Project");
        mockProject.setOwnerId(10);
        mockProject.setOwnerUsername("testuser");
        mockProject.setVisibility("PUBLIC");
        mockProject.setLanguage("java");
        mockProject.setStarCount(5);
    }

    @Test
    void testCreateProject() {
        when(projectRepository.save(any(Project.class))).thenReturn(mockProject);
        Project saved = projectService.createProject(mockProject);
        assertNotNull(saved);
        assertEquals("Test Project", saved.getName());
    }

    @Test
    void testGetProjectsByOwner() {
        when(projectRepository.findByOwnerId(10)).thenReturn(Arrays.asList(mockProject));
        when(starRepository.findStarredProjectIds(anyList(), eq(10))).thenReturn(Collections.singleton(1));
        
        List<Project> projects = projectService.getProjectsByOwner(10);
        assertEquals(1, projects.size());
        assertTrue(projects.get(0).isStarredByMe());
    }

    @Test
    void testGetPublicProjects() {
        when(projectRepository.findByVisibility("PUBLIC")).thenReturn(Arrays.asList(mockProject));
        List<Project> projects = projectService.getPublicProjects(20);
        assertEquals(1, projects.size());
    }

    @Test
    void testStarProject_AddNewStar() {
        when(projectRepository.findById(1)).thenReturn(Optional.of(mockProject));
        when(starRepository.findByProjectIdAndUserId(1, 20)).thenReturn(Optional.empty());

        projectService.starProject(1, 20);

        verify(starRepository, times(1)).save(any(ProjectStar.class));
        assertEquals(6, mockProject.getStarCount());
        verify(projectRepository, times(1)).save(mockProject);
    }

    @Test
    void testStarProject_RemoveExistingStar() {
        ProjectStar existingStar = new ProjectStar();
        when(projectRepository.findById(1)).thenReturn(Optional.of(mockProject));
        when(starRepository.findByProjectIdAndUserId(1, 20)).thenReturn(Optional.of(existingStar));

        projectService.starProject(1, 20);

        verify(starRepository, times(1)).delete(existingStar);
        assertEquals(4, mockProject.getStarCount());
        verify(projectRepository, times(1)).save(mockProject);
    }

    @Test
    void testForkProject() {
        when(projectRepository.findById(1)).thenReturn(Optional.of(mockProject));
        
        Project forkedProject = new Project();
        forkedProject.setProjectId(2);
        forkedProject.setName("Test Project-fork");
        
        when(projectRepository.save(any(Project.class))).thenReturn(forkedProject);
        when(restTemplate.postForEntity(anyString(), any(), eq(Void.class))).thenReturn(ResponseEntity.ok().build());

        Project result = projectService.forkProject(1, 30, "forker");
        
        assertEquals(2, result.getProjectId());
        assertEquals("Test Project-fork", result.getName());
        verify(restTemplate, times(1)).postForEntity(anyString(), any(), eq(Void.class));
    }

    @Test
    void testForkProject_PrivateFails() {
        mockProject.setVisibility("PRIVATE");
        when(projectRepository.findById(1)).thenReturn(Optional.of(mockProject));

        assertThrows(IllegalStateException.class, () -> projectService.forkProject(1, 30, "forker"));
    }

    @Test
    void testRequestCollaboration_NewRequest() {
        when(projectRepository.findById(1)).thenReturn(Optional.of(mockProject));
        when(memberRepository.findByProjectIdAndUserId(1, 20)).thenReturn(Optional.empty());

        projectService.requestCollaboration(1, 20, "collabUser");

        verify(memberRepository, times(1)).save(any(ProjectMember.class));
    }

    @Test
    void testHasEditAccess_Owner() {
        when(projectRepository.findById(1)).thenReturn(Optional.of(mockProject));
        assertTrue(projectService.hasEditAccess(1, 10)); // 10 is owner
    }

    @Test
    void testHasEditAccess_Editor() {
        when(projectRepository.findById(1)).thenReturn(Optional.of(mockProject));
        when(memberRepository.existsByProjectIdAndUserIdAndRole(1, 20, "EDITOR")).thenReturn(true);
        assertTrue(projectService.hasEditAccess(1, 20));
    }

    @Test
    void testArchiveProject() {
        when(projectRepository.findById(1)).thenReturn(Optional.of(mockProject));
        projectService.archiveProject(1);
        assertTrue(mockProject.isArchived());
        verify(projectRepository, times(1)).save(mockProject);
    }

    @Test
    void testDeleteProject() {
        projectService.deleteProject(1);
        verify(projectRepository, times(1)).deleteById(1);
    }

    @Test
    void testGetProjectMembers() {
        ProjectMember editor = new ProjectMember();
        editor.setRole("EDITOR");
        when(memberRepository.findByProjectId(1)).thenReturn(Arrays.asList(editor));
        when(projectRepository.findById(1)).thenReturn(Optional.of(mockProject));

        List<ProjectMember> members = projectService.getProjectMembers(1);
        assertEquals(2, members.size()); // Owner + Editor
        assertEquals("OWNER", members.get(0).getRole());
    }

    @Test
    void testApproveCollaborator() {
        ProjectMember pending = new ProjectMember();
        pending.setRole("PENDING");
        when(memberRepository.findByProjectIdAndUserId(1, 20)).thenReturn(Optional.of(pending));

        projectService.approveCollaborator(1, 20);

        assertEquals("EDITOR", pending.getRole());
        verify(memberRepository, times(1)).save(pending);
    }

    @Test
    void testUpdateProject() {
        Project updates = new Project();
        updates.setName("Updated Name");
        updates.setDescription("New Desc");
        updates.setVisibility("PRIVATE");

        when(projectRepository.findById(1)).thenReturn(Optional.of(mockProject));
        when(projectRepository.save(any(Project.class))).thenReturn(mockProject);

        projectService.updateProject(1, updates);

        assertEquals("Updated Name", mockProject.getName());
        assertEquals("PRIVATE", mockProject.getVisibility());
        verify(projectRepository, times(1)).save(mockProject);
    }
}