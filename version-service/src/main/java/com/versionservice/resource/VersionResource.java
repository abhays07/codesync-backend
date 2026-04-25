package com.versionservice.resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.versionservice.entity.Snapshot;
import com.versionservice.service.VersionService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/")
public class VersionResource {

	@Autowired
	private VersionService versionService;

	@PostMapping("/snapshot")
	public ResponseEntity<Snapshot> save(@RequestBody Snapshot snapshot) {
		return ResponseEntity.ok(versionService.createSnapshot(snapshot));
	}

	@GetMapping("/history/{fileId}")
	public ResponseEntity<List<Snapshot>> getHistory(@PathVariable Integer fileId) {
		return ResponseEntity.ok(versionService.getFileHistory(fileId));
	}

	@GetMapping("/diff")
	public ResponseEntity<Map<String, Object>> getDiff(@RequestParam Long base, @RequestParam Long head) {
		return ResponseEntity.ok(versionService.compareSnapshots(base, head));
	}

	@PostMapping("/restore/{snapshotId}")
	public ResponseEntity<Snapshot> restore(@PathVariable Long snapshotId, @RequestParam Integer userId) {
		return ResponseEntity.ok(versionService.restoreVersion(snapshotId, userId));
	}
}