package com.versionservice.serviceImpl;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import com.versionservice.entity.Snapshot;
import com.versionservice.repository.SnapshotRepository;
import com.versionservice.service.VersionService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class VersionServiceImpl implements VersionService {

	@Autowired
	private SnapshotRepository repository;

	@Override
	public Snapshot createSnapshot(Snapshot snapshot) {
		// Calculate SHA-256 Hash for integrity
		snapshot.setHash(calculateHash(snapshot.getContent()));

		// Find current latest to set as parent
		repository.findFirstByFileIdOrderByCreatedAtDesc(snapshot.getFileId())
				.ifPresent(latest -> snapshot.setParentSnapshotId(latest.getId()));

		return repository.save(snapshot);
	}

	@Override
	public Map<String, Object> compareSnapshots(Long baseId, Long headId) {
		Snapshot base = repository.findById(baseId).orElseThrow();
		Snapshot head = repository.findById(headId).orElseThrow();

		List<String> baseLines = Arrays.asList(base.getContent().split("\n"));
		List<String> headLines = Arrays.asList(head.getContent().split("\n"));

		// Myers Diff Algorithm implementation via java-diff-utils
		Patch<String> patch = DiffUtils.diff(baseLines, headLines);

		List<String> diffs = patch.getDeltas().stream().map(AbstractDelta::toString).collect(Collectors.toList());

		Map<String, Object> response = new HashMap<>();
		response.put("baseHash", base.getHash());
		response.put("headHash", head.getHash());
		response.put("changes", diffs);
		return response;
	}

	@Override
	public Snapshot restoreVersion(Long snapshotId, Integer userId) {
		Snapshot target = repository.findById(snapshotId).orElseThrow();

		// Restoration is non-destructive: create a NEW snapshot with OLD content
		Snapshot restored = Snapshot.builder().fileId(target.getFileId()).content(target.getContent())
				.commitMessage("Restored to version: " + snapshotId).userId(userId).build();

		return createSnapshot(restored);
	}

	@Override
	public List<Snapshot> getFileHistory(Integer fileId) {
		return repository.findByFileIdOrderByCreatedAtDesc(fileId);
	}

	@Override
	public Snapshot getLatestSnapshot(Integer fileId) {
		return repository.findFirstByFileIdOrderByCreatedAtDesc(fileId).orElse(null);
	}

	private String calculateHash(String content) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] encodedhash = digest.digest(content.getBytes());
			StringBuilder hexString = new StringBuilder();
			for (byte b : encodedhash) {
				String hex = Integer.toHexString(0xff & b);
				if (hex.length() == 1)
					hexString.append('0');
				hexString.append(hex);
			}
			return hexString.toString();
		} catch (Exception e) {
			return UUID.randomUUID().toString(); // Fallback
		}
	}
}