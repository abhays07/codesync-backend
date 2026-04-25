package com.commentservice.resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.commentservice.entity.Comment;
import com.commentservice.service.CommentService;

import java.util.List;

@RestController
@RequestMapping("/")
public class CommentResource {

	@Autowired
	private CommentService commentService;

	@PostMapping("/add")
	public ResponseEntity<Comment> add(@RequestBody Comment comment) {
		return ResponseEntity.ok(commentService.addComment(comment));
	}

	@GetMapping("/file/{fileId}")
	public ResponseEntity<List<Comment>> getByFile(@PathVariable Integer fileId) {
		return ResponseEntity.ok(commentService.getCommentsByFile(fileId));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		commentService.deleteComment(id);
		return ResponseEntity.noContent().build();
	}
}