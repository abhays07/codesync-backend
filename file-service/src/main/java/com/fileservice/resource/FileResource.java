package com.fileservice.resource;

import com.fileservice.dto.FileNode;
import com.fileservice.entity.CodeFile;
import com.fileservice.entity.Folder;
import com.fileservice.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/files")
public class FileResource {

	@Autowired
	private FileService fileService;

	@PostMapping("/folder")
	public ResponseEntity<Folder> createFolder(@RequestBody Folder folder) {
		return ResponseEntity.ok(fileService.createFolder(folder));
	}

	@PostMapping("/file")
	public ResponseEntity<CodeFile> createFile(@RequestBody CodeFile file) {
		return ResponseEntity.ok(fileService.createFile(file));
	}

	@GetMapping("/tree/{projectId}")
	public ResponseEntity<List<FileNode>> getProjectTree(@PathVariable int projectId) {
		return ResponseEntity.ok(fileService.getProjectTree(projectId));
	}

	@PutMapping("/file/{fileId}/content")
	public ResponseEntity<CodeFile> updateFileContent(@PathVariable Long fileId, @RequestBody String content,
			@RequestParam int userId) {

		String cleanedContent = content;
		if (content != null && content.startsWith("\"") && content.endsWith("\"")) {
			cleanedContent = content.substring(1, content.length() - 1);
		}

		return ResponseEntity.ok(fileService.updateFileContent(fileId, cleanedContent, userId));
	}

	@PatchMapping("/file/{fileId}/rename")
	public ResponseEntity<CodeFile> renameFile(@PathVariable Long fileId, @RequestParam String newName) {
		return ResponseEntity.ok(fileService.renameFile(fileId, newName));
	}

	@PatchMapping("/folder/{folderId}/rename")
	public ResponseEntity<Folder> renameFolder(@PathVariable Long folderId, @RequestParam String newName) {
		return ResponseEntity.ok(fileService.renameFolder(folderId, newName));
	}

	@DeleteMapping("/file/{fileId}")
	public ResponseEntity<Void> deleteFile(@PathVariable Long fileId) {
		fileService.deleteFile(fileId);
		return ResponseEntity.ok().build();
	}

	@DeleteMapping("/folder/{folderId}")
	public ResponseEntity<Void> deleteFolder(@PathVariable Long folderId) {
		fileService.deleteFolder(folderId);
		return ResponseEntity.ok().build();
	}

	@GetMapping("/search/{projectId}")
	public ResponseEntity<List<CodeFile>> searchInProject(@PathVariable int projectId, @RequestParam String q) {
		return ResponseEntity.ok(fileService.searchInProject(projectId, q));
	}

	@PostMapping("/clone")
	public ResponseEntity<Void> cloneProjectFiles(@RequestParam int sourceId, @RequestParam int targetId) {
		fileService.cloneProjectFiles(sourceId, targetId);
		return ResponseEntity.ok().build();
	}
}