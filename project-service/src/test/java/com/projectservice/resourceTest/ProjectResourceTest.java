package com.projectservice.resourceTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectservice.entity.Project;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class ProjectResourceTest {

    private MockMvc mockMvc;

    @Mock
    private ProjectService projectService;

    @InjectMocks
    private ProjectResource projectResource;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(projectResource).build();
    }

    @Test
    void createProject_ShouldReturnCreatedProject() throws Exception {
        Project project = new Project();
        project.setName("Test Project");
        project.setLanguage("Java");
        project.setOwnerId(1);

        Project savedProject = new Project();
        savedProject.setProjectId(100);
        savedProject.setName("Test Project");
        savedProject.setLanguage("Java");
        savedProject.setOwnerId(1);

        when(projectService.createProject(any(Project.class))).thenReturn(savedProject);

        mockMvc.perform(post("/api/v1/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(project)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(100))
                .andExpect(jsonPath("$.name").value("Test Project"));
    }
}
