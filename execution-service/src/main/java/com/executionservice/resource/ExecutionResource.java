package com.executionservice.resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.executionservice.entity.ExecutionJob;
import com.executionservice.service.ExecutionService;

import java.util.List;

@RestController
@RequestMapping("/") // Stripped by Gateway RewritePath
public class ExecutionResource {

	@Autowired
	private ExecutionService execService;

	@PostMapping("/submit")
	public ResponseEntity<ExecutionJob> submit(@RequestBody ExecutionJob job) {
		return ResponseEntity.ok(execService.submitExecution(job));
	}

	@GetMapping("/{jobId}")
	public ResponseEntity<ExecutionJob> getJob(@PathVariable String jobId) {
		return execService.getJobById(jobId).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
	}

	@GetMapping("/user/{userId}")
	public ResponseEntity<List<ExecutionJob>> getByUser(@PathVariable int userId) {
		return ResponseEntity.ok(execService.getExecutionsByUser(userId));
	}

	@PostMapping("/{jobId}/cancel")
	public ResponseEntity<Void> cancel(@PathVariable String jobId) {
		execService.cancelExecution(jobId);
		return ResponseEntity.ok().build();
	}
}