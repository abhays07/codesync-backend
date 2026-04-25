package com.executionservice.serviceTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.executionservice.config.RabbitMQConfig;
import com.executionservice.entity.ExecutionJob;
import com.executionservice.repository.ExecutionRepository;
import com.executionservice.serviceImpl.ExecutionServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class ExecutionServiceImplTest {

	@Mock
	private ExecutionRepository repository;

	@Mock
	private RabbitTemplate rabbitTemplate;

	@InjectMocks
	private ExecutionServiceImpl executionService;

	private ExecutionJob testJob;

	@BeforeEach
	void setUp() {
		testJob = new ExecutionJob();
		testJob.setSourceCode("print('Hello World')");
		testJob.setLanguage("python");
		testJob.setUserId(101);
	}

	@Test
	void testSubmitExecution_Success() {
		// Arrange
		when(repository.save(any(ExecutionJob.class))).thenAnswer(i -> i.getArguments()[0]);

		// Act
		ExecutionJob result = executionService.submitExecution(testJob);

		// Assert
		assertNotNull(result.getJobId());
		assertEquals("QUEUED", result.getStatus());

		// Verify database persistence
		verify(repository, times(1)).save(any(ExecutionJob.class));

		// Verify RabbitMQ message sending
		verify(rabbitTemplate, times(1)).convertAndSend(eq(RabbitMQConfig.EXECUTION_EXCHANGE), eq("execution.run"),
				eq(result.getJobId()));
	}

	@Test
	void testGetJobById_Found() {
		String id = UUID.randomUUID().toString();
		testJob.setJobId(id);
		when(repository.findById(id)).thenReturn(Optional.of(testJob));

		Optional<ExecutionJob> foundJob = executionService.getJobById(id);

		assertTrue(foundJob.isPresent());
		assertEquals(id, foundJob.get().getJobId());
	}

	@Test
	void testCancelExecution_Success() {
		String id = "test-job-id";
		testJob.setJobId(id);
		testJob.setStatus("QUEUED");

		when(repository.findById(id)).thenReturn(Optional.of(testJob));
		when(repository.save(any(ExecutionJob.class))).thenReturn(testJob);

		executionService.cancelExecution(id);

		assertEquals("CANCELLED", testJob.getStatus());
		verify(repository).save(testJob);
	}
}