package com.commentservice.serviceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.commentservice.entity.Comment;
import com.commentservice.repository.CommentRepository;
import com.commentservice.service.CommentService;

import java.util.List;

@Service
public class CommentServiceImpl implements CommentService {

	@Autowired
	private CommentRepository repository;

	@Override
	public Comment addComment(Comment comment) {
		// Standard save logic - timestamps handled by @PrePersist in Entity
		return repository.save(comment);
	}

	@Override
	public List<Comment> getCommentsByFile(Integer fileId) {
		return repository.findByFileIdOrderByCreatedAtAsc(fileId);
	}

	@Override
	public void deleteComment(Long commentId) {
		repository.deleteById(commentId);
	}

	@Override
	public Comment updateComment(Long id, String content) {
		Comment existing = repository.findById(id).orElseThrow(() -> new RuntimeException("Comment not found"));
		existing.setContent(content);
		return repository.save(existing);
	}
}