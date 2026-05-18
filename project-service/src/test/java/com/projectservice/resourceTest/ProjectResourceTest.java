package com.projectservice.resourceTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectservice.entity.Project;
import com.projectservice.entity.ProjectMember;
import com.projectservice.resource.ProjectResource;
import com.projectservice.service.ProjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class ProjectResourceTest {

    private MockMvc mockMvc;

    @Mock
    private ProjectService projectService;

    @InjectMocks
    private ProjectResource projectResource;

    private ObjectMapper objectMapper = new ObjectMapper();

    private Project mockProject;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(projectResource).build();
        
        mockProject = new Project();
        mockProject.setProjectId(1);
        mockProject.setName("Test Project");
        mockProject.setOwnerId(10);
        mockProject.setVisibility("PUBLIC");
    }

    @Test
    void testCreateProject() throws Exception {
        when(projectService.createProject(any(Project.class))).thenReturn(mockProject);

        mockMvc.perform(post("/api/v1/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mockProject)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Project"))
                .andExpect(jsonPath("$.projectId").value(1));
    }

    @Test
    void testGetByOwner() throws Exception {
        when(projectService.getProjectsByOwner(10)).thenReturn(Arrays.asList(mockProject));

        mockMvc.perform(get("/api/v1/projects/owner/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].name").value("Test Project"));
    }

    @Test
    void testGetPublic() throws Exception {
        when(projectService.getPublicProjects(20)).thenReturn(Arrays.asList(mockProject));

        mockMvc.perform(get("/api/v1/projects/public?currentUserId=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1));
    }

    @Test
    void testToggleStar() throws Exception {
        doNothing().when(projectService).starProject(1, 20);

        mockMvc.perform(put("/api/v1/projects/1/star?userId=20"))
                .andExpect(status().isNoContent());
    }

    @Test
    void testSearch() throws Exception {
        when(projectService.searchProjects("Test", 20)).thenReturn(Arrays.asList(mockProject));

        mockMvc.perform(get("/api/v1/projects/search?name=Test&userId=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1));
    }

    @Test
    void testGetById() throws Exception {
        when(projectService.getProjectById(1)).thenReturn(mockProject);

        mockMvc.perform(get("/api/v1/projects/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Project"));
    }

    @Test
    void testFork() throws Exception {
        Project forked = new Project();
        forked.setProjectId(2);
        forked.setName("Forked Project");
        
        when(projectService.forkProject(1, 30, "forker")).thenReturn(forked);

        mockMvc.perform(post("/api/v1/projects/1/fork?userId=30&username=forker"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(2))
                .andExpect(jsonPath("$.name").value("Forked Project"));
    }

    @Test
    void testUpdateProject() throws Exception {
        when(projectService.updateProject(eq(1), any(Project.class))).thenReturn(mockProject);

        mockMvc.perform(put("/api/v1/projects/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mockProject)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Project"));
    }

    @Test
    void testRequestAccess() throws Exception {
        doNothing().when(projectService).requestCollaboration(1, 20, "collabUser");

        mockMvc.perform(post("/api/v1/projects/1/members/request?userId=20&username=collabUser"))
                .andExpect(status().isOk());
    }

    @Test
    void testApproveAccess() throws Exception {
        doNothing().when(projectService).approveCollaborator(1, 20);

        mockMvc.perform(post("/api/v1/projects/1/members/approve?userId=20"))
                .andExpect(status().isOk());
    }

    @Test
    void testCheckEditAccess() throws Exception {
        when(projectService.hasEditAccess(1, 20)).thenReturn(true);

        mockMvc.perform(get("/api/v1/projects/1/access?userId=20"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void testGetPendingRequests() throws Exception {
        ProjectMember member = new ProjectMember();
        member.setUsername("pendingUser");
        when(projectService.getPendingRequests(1)).thenReturn(Arrays.asList(member));

        mockMvc.perform(get("/api/v1/projects/1/requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].username").value("pendingUser"));
    }

    @Test
    void testGetMembers() throws Exception {
        ProjectMember member = new ProjectMember();
        member.setUsername("editorUser");
        when(projectService.getProjectMembers(1)).thenReturn(Arrays.asList(member));

        mockMvc.perform(get("/api/v1/projects/1/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].username").value("editorUser"));
    }

    @Test
    void testRemoveMember() throws Exception {
        doNothing().when(projectService).removeProjectMember(1, 20);

        mockMvc.perform(delete("/api/v1/projects/1/members/20"))
                .andExpect(status().isNoContent());
    }

    @Test
    void testArchive() throws Exception {
        doNothing().when(projectService).archiveProject(1);

        mockMvc.perform(put("/api/v1/projects/1/archive"))
                .andExpect(status().isNoContent());
    }

    @Test
    void testDelete() throws Exception {
        doNothing().when(projectService).deleteProject(1);

        mockMvc.perform(delete("/api/v1/projects/1"))
                .andExpect(status().isNoContent());
    }
}
