package com.fileservice.resourceTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fileservice.dto.FileNode;
import com.fileservice.entity.CodeFile;
import com.fileservice.entity.Folder;
import com.fileservice.resource.FileResource;
import com.fileservice.service.FileService;

@ExtendWith(MockitoExtension.class)
class FileResourceTest {

    private MockMvc mockMvc;

    @Mock
    private FileService fileService;

    @InjectMocks
    private FileResource fileResource;

    private ObjectMapper objectMapper;
    private CodeFile file;
    private Folder folder;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(fileResource).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

        file = new CodeFile();
        file.setFileId(10L);
        file.setName("App.java");
        file.setExtension("java");
        file.setContent("public class App {}");
        file.setProjectId(1);

        folder = new Folder();
        folder.setFolderId(20L);
        folder.setName("src");
        folder.setProjectId(1);
    }

    @Test
    void testGetProjectTree_Success() throws Exception {
        List<FileNode> tree = new ArrayList<>();
        when(fileService.getProjectTree(1)).thenReturn(tree);

        mockMvc.perform(get("/api/v1/files/tree/1"))
                .andExpect(status().isOk());
    }

    @Test
    void testCreateFile_Success() throws Exception {
        when(fileService.createFile(any(CodeFile.class))).thenReturn(file);

        mockMvc.perform(post("/api/v1/files/file")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(file)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("App.java"));
    }

    @Test
    void testUpdateFileContent_Success() throws Exception {
        when(fileService.updateFileContent(eq(10L), anyString(), eq(5))).thenReturn(file);

        mockMvc.perform(put("/api/v1/files/file/10/content")
                .contentType(MediaType.APPLICATION_JSON)
                .content("\"public class App {}\"")
                .param("userId", "5"))
                .andExpect(status().isOk());
    }

    @Test
    void testCreateFolder_Success() throws Exception {
        when(fileService.createFolder(any(Folder.class))).thenReturn(folder);

        mockMvc.perform(post("/api/v1/files/folder")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(folder)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("src"));
    }

    @Test
    void testCloneProjectFiles_Success() throws Exception {
        doNothing().when(fileService).cloneProjectFiles(1, 2);

        mockMvc.perform(post("/api/v1/files/clone")
                .param("sourceId", "1")
                .param("targetId", "2"))
                .andExpect(status().isOk());
    }

    @Test
    void testDeleteFile_Success() throws Exception {
        doNothing().when(fileService).deleteFile(10L);

        mockMvc.perform(delete("/api/v1/files/file/10"))
                .andExpect(status().isNoContent());
    }

    @Test
    void testDeleteFolder_Success() throws Exception {
        doNothing().when(fileService).deleteFolder(20L);

        mockMvc.perform(delete("/api/v1/files/folder/20"))
                .andExpect(status().isNoContent());
    }

    @Test
    void testRenameFile_Success() throws Exception {
        when(fileService.renameFile(eq(10L), eq("Main.java"))).thenReturn(file);

        mockMvc.perform(patch("/api/v1/files/file/10/rename")
                .param("newName", "Main.java"))
                .andExpect(status().isOk());
    }

    @Test
    void testRenameFolder_Success() throws Exception {
        when(fileService.renameFolder(eq(20L), eq("resources"))).thenReturn(folder);

        mockMvc.perform(patch("/api/v1/files/folder/20/rename")
                .param("newName", "resources"))
                .andExpect(status().isOk());
    }

    @Test
    void testSearchInProject_Success() throws Exception {
        when(fileService.searchInProject(1, "App")).thenReturn(List.of(file));

        mockMvc.perform(get("/api/v1/files/search/1")
                .param("q", "App"))
                .andExpect(status().isOk());
    }
}
