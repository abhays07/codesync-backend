package com.executionservice.serviceImpl;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.executionservice.config.RabbitMQConfig;
import com.executionservice.entity.ExecutionJob;
import com.executionservice.repository.ExecutionRepository;
import com.executionservice.service.ExecutionService;

import java.util.*;

@Service
public class ExecutionServiceImpl implements ExecutionService {

	@Autowired
	private ExecutionRepository repository;

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Override
	public ExecutionJob submitExecution(ExecutionJob job) {
		job.setJobId(UUID.randomUUID().toString());
		job.setStatus("QUEUED");
		ExecutionJob savedJob = repository.save(job);

		// Push to RabbitMQ for worker to process [cite: 510]
		rabbitTemplate.convertAndSend(RabbitMQConfig.EXECUTION_EXCHANGE, "execution.run", savedJob.getJobId());
		return savedJob;
	}

	@Override
	public Optional<ExecutionJob> getJobById(String jobId) {
		return repository.findById(jobId);
	}

	@Override
	public List<ExecutionJob> getExecutionsByUser(int userId) {
		return repository.findByUserId(userId);
	}

	@Override
	public List<ExecutionJob> getExecutionsByProject(int projectId) {
		return repository.findByProjectId(projectId);
	}

	@Override
	public void cancelExecution(String jobId) {
		repository.findById(jobId).ifPresent(job -> {
			job.setStatus("CANCELLED");
			repository.save(job);
		});
	}
}