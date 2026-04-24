package com.commentservice.serviceTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.commentservice.entity.Comment;
import com.commentservice.repository.CommentRepository;
import com.commentservice.serviceImpl.CommentServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class CommentServiceImplTest {

	@Mock
	private CommentRepository repository;

	@InjectMocks
	private CommentServiceImpl commentService;

	private Comment testComment;

	@BeforeEach
	void setUp() {
		testComment = Comment.builder().id(1L).fileId(101).userId(1).username("Abhay").content("Initial review comment")
				.lineNumber(15).build();
	}

	@Test
	void testAddComment_Success() {
		// Arrange
		when(repository.save(any(Comment.class))).thenReturn(testComment);

		// Act
		Comment savedComment = commentService.addComment(testComment);

		// Assert
		assertNotNull(savedComment);
		assertEquals("Abhay", savedComment.getUsername());
		verify(repository, times(1)).save(testComment);
	}

	@Test
	void testGetCommentsByFile_Success() {
		// Arrange
		List<Comment> mockComments = Arrays.asList(testComment);
		when(repository.findByFileIdOrderByCreatedAtAsc(101)).thenReturn(mockComments);

		// Act
		List<Comment> result = commentService.getCommentsByFile(101);

		// Assert
		assertEquals(1, result.size());
		assertEquals(101, result.get(0).getFileId());
		verify(repository, times(1)).findByFileIdOrderByCreatedAtAsc(101);
	}

	@Test
	void testUpdateComment_Success() {
		// Arrange
		String newContent = "Updated review comment";
		when(repository.findById(1L)).thenReturn(Optional.of(testComment));
		when(repository.save(any(Comment.class))).thenAnswer(i -> i.getArguments()[0]);

		// Act
		Comment updatedComment = commentService.updateComment(1L, newContent);

		// Assert
		assertEquals(newContent, updatedComment.getContent());
		verify(repository).findById(1L);
		verify(repository).save(any(Comment.class));
	}

	@Test
	void testUpdateComment_NotFound() {
		// Arrange
		when(repository.findById(1L)).thenReturn(Optional.empty());

		// Act & Assert
		assertThrows(RuntimeException.class, () -> {
			commentService.updateComment(1L, "Content");
		});
	}

	@Test
	void testDeleteComment_Success() {
		// Act
		commentService.deleteComment(1L);

		// Assert
		verify(repository, times(1)).deleteById(1L);
	}
}