package com.executionservice.resourceTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.executionservice.entity.ExecutionJob;
import com.executionservice.resource.ExecutionResource;
import com.executionservice.service.ExecutionService;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class ExecutionResourceTest {

    private MockMvc mockMvc;

    @Mock
    private ExecutionService execService;

    @InjectMocks
    private ExecutionResource executionResource;

    private ObjectMapper objectMapper = new ObjectMapper();

    private ExecutionJob testJob;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(executionResource).build();

        testJob = new ExecutionJob();
        testJob.setJobId("job-123");
        testJob.setSourceCode("print('Hello World')");
        testJob.setLanguage("python");
        testJob.setUserId(101);
        testJob.setStatus("QUEUED");
    }

    @Test
    void testSubmitJob() throws Exception {
        when(execService.submitExecution(any(ExecutionJob.class))).thenReturn(testJob);

        mockMvc.perform(post("/api/v1/executions/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testJob)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value("job-123"))
                .andExpect(jsonPath("$.status").value("QUEUED"));
    }

    @Test
    void testGetJobStatus_Found() throws Exception {
        when(execService.getJobById("job-123")).thenReturn(Optional.of(testJob));

        mockMvc.perform(get("/api/v1/executions/job-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value("job-123"));
    }

    @Test
    void testGetJobStatus_NotFound() throws Exception {
        when(execService.getJobById("job-unknown")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/executions/job-unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetUserHistory() throws Exception {
        when(execService.getExecutionsByUser(101)).thenReturn(Arrays.asList(testJob));

        mockMvc.perform(get("/api/v1/executions/user/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].jobId").value("job-123"));
    }
}
