package com.executionservice.listener;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.executionservice.config.RabbitMQConfig;
import com.executionservice.entity.ExecutionJob;
import com.executionservice.repository.ExecutionRepository;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ExecutionJobListener {

	@Autowired
	private DockerClient dockerClient;

	@Autowired
	private ExecutionRepository repository;

	@RabbitListener(queues = RabbitMQConfig.EXECUTION_QUEUE)
	public void processJob(String jobId) {
		ExecutionJob job = repository.findById(jobId).orElseThrow();
		job.setStatus("RUNNING");
		repository.save(job);

		String containerId = null;
		long startTime = System.currentTimeMillis();

		try {
			String image = getDockerImage(job.getLanguage());

			// Create Container with strict resource limits 
			HostConfig hostConfig = HostConfig.newHostConfig()
					.withMemory(256 * 1024 * 1024L) // Max 256MB RAM
					.withCpuQuota(100000L)          // CPU Limit
					.withNetworkMode("none");       // No network access

			CreateContainerResponse container = dockerClient.createContainerCmd(image)
					.withHostConfig(hostConfig)
					.withAttachStdout(true)
					.withAttachStderr(true)
					.withCmd(getExecutionCommand(job.getLanguage(), job.getSourceCode()))
					.exec();

			containerId = container.getId();

			dockerClient.startContainerCmd(containerId).exec();

			WaitContainerResultCallback waitCallback = new WaitContainerResultCallback();
			dockerClient.waitContainerCmd(containerId).exec(waitCallback);

			// Enforce the 10-second execution limit 
			boolean finished = waitCallback.awaitCompletion(10, TimeUnit.SECONDS);

			if (!finished) {
				job.setStatus("TIMED_OUT");
				job.setStderr("Execution exceeded the 10-second limit.");
				dockerClient.stopContainerCmd(containerId).exec();
			} else {
				StringBuilder stdout = new StringBuilder();
				StringBuilder stderr = new StringBuilder();

				dockerClient.logContainerCmd(containerId)
						.withStdOut(true)
						.withStdErr(true)
						.withFollowStream(true)
						.exec(new ResultCallback.Adapter<Frame>() {
							@Override
							public void onNext(Frame frame) {
								if (frame.getStreamType().name().equals("STDOUT")) {
									stdout.append(new String(frame.getPayload()));
								} else if (frame.getStreamType().name().equals("STDERR")) {
									stderr.append(new String(frame.getPayload()));
								}
							}
						}).awaitCompletion();

				job.setStdout(stdout.toString());
				job.setStderr(stderr.toString());
				job.setStatus("COMPLETED");
			}

		} catch (Exception e) {
			log.error("Failed to execute job {}: {}", jobId, e.getMessage());
			job.setStatus("FAILED");
			job.setStderr(e.getMessage());
		} finally {
			if (containerId != null) {
				try {
					dockerClient.removeContainerCmd(containerId).withForce(true).exec();
				} catch (Exception e) {
					log.warn("Failed to remove container {}: {}", containerId, e.getMessage());
				}
			}
			job.setExecutionTimeMs(System.currentTimeMillis() - startTime);
			job.setCompletedAt(LocalDateTime.now());
			repository.save(job);
		}
	}

	private String getDockerImage(String language) {
		return switch (language.toLowerCase()) {
			case "java" -> "amazoncorretto:17-alpine";
			case "javascript", "nodejs" -> "node:alpine";
			case "cpp", "c" -> "gcc:latest";
			default -> "python:3.9-slim";
		};
	}

	private String[] getExecutionCommand(String language, String code) {
		return switch (language.toLowerCase()) {
			case "javascript", "nodejs" -> new String[] { "node", "-e", code };
			case "java" -> new String[] { "/bin/sh", "-c", 
				"echo '" + code.replace("'", "'\\''") + "' > Main.java && javac Main.java && java Main" };
			case "cpp" -> new String[] { "/bin/sh", "-c", 
				"echo '" + code.replace("'", "'\\''") + "' > solution.cpp && g++ solution.cpp -o solution && ./solution" };
			default -> new String[] { "python", "-c", code };
		};
	}
}