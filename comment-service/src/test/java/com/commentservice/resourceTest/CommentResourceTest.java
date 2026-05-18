package com.commentservice.resourceTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
import com.commentservice.entity.Comment;
import com.commentservice.resource.CommentResource;
import com.commentservice.service.CommentService;

@ExtendWith(MockitoExtension.class)
public class CommentResourceTest {

    private MockMvc mockMvc;

    @Mock
    private CommentService commentService;

    @InjectMocks
    private CommentResource commentResource;

    private ObjectMapper objectMapper;
    private Comment comment;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(commentResource).build();
        objectMapper = new ObjectMapper();

        comment = Comment.builder()
                .id(1L)
                .fileId(101)
                .userId(1)
                .username("Abhay")
                .content("Refactor this method")
                .lineNumber(15)
                .build();
    }

    @Test
    void testAddComment_Success() throws Exception {
        when(commentService.addComment(any(Comment.class))).thenReturn(comment);

        mockMvc.perform(post("/api/v1/comments/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(comment)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("Abhay"));
    }

    @Test
    void testGetFileReviewThreads_Success() throws Exception {
        List<Comment> list = List.of(comment);
        when(commentService.getCommentsByFile(101)).thenReturn(list);

        mockMvc.perform(get("/api/v1/comments/file/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("Abhay"))
                .andExpect(jsonPath("$[0].lineNumber").value(15));
    }

    @Test
    void testEditComment_Success() throws Exception {
        String newContent = "Updated content";
        Comment updated = Comment.builder()
                .id(1L)
                .fileId(101)
                .userId(1)
                .username("Abhay")
                .content(newContent)
                .lineNumber(15)
                .build();

        when(commentService.updateComment(eq(1L), any(String.class))).thenReturn(updated);

        mockMvc.perform(put("/api/v1/comments/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(newContent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(newContent));
    }

    @Test
    void testRemoveComment_Success() throws Exception {
        doNothing().when(commentService).deleteComment(1L);

        mockMvc.perform(delete("/api/v1/comments/1"))
                .andExpect(status().isNoContent());

        verify(commentService, times(1)).deleteComment(1L);
    }
}
