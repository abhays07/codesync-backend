package com.fileservice.serviceImpl;

import com.fileservice.entity.CodeFile;
import com.fileservice.entity.Folder;
import com.fileservice.dto.FileNode;
import com.fileservice.repository.CodeFileRepository;
import com.fileservice.repository.FolderRepository;
import com.fileservice.service.FileService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class FileServiceImpl implements FileService {

	@Autowired
	private FolderRepository folderRepository;

	@Autowired
	private CodeFileRepository fileRepository;

	@Override
	@Transactional
	public Folder createFolder(Folder folder) {
		folder.setDeleted(false);
		return folderRepository.save(folder);
	}

	@Override
	@Transactional
	public CodeFile createFile(CodeFile file) {
		file.setDeleted(false);
		file.setCreatedAt(LocalDateTime.now());
		file.setUpdatedAt(LocalDateTime.now());
		return fileRepository.save(file);
	}

	@Override
	@Transactional
	public CodeFile updateFileContent(Long fileId, String content, int userId) {
		CodeFile file = fileRepository.findById(fileId)
				.orElseThrow(() -> new RuntimeException("Update Failed: File not found"));

		file.setContent(content);
		file.setLastEditedBy(userId);
		file.setUpdatedAt(LocalDateTime.now());
		file.setSize((long) (content != null ? content.length() : 0));
		return fileRepository.save(file);
	}

	@Override
	public List<FileNode> getProjectTree(int projectId) {
		// Auto-initialize if project is empty
		if (folderRepository.findByProjectId(projectId).isEmpty()
				&& fileRepository.findByProjectIdAndIsDeletedFalse(projectId).isEmpty()) {
			initializeNewProject(projectId);
		}

		List<FileNode> rootNodes = new ArrayList<>();

		// 1. Fetch Root Folders
		List<Folder> rootFolders = folderRepository.findByProjectIdAndParentFolderIdIsNullAndIsDeletedFalse(projectId);
		for (Folder folder : rootFolders) {
			rootNodes.add(buildFolderTree(folder));
		}

		// 2. Fetch Root Files
		List<CodeFile> rootFiles = fileRepository.findByProjectIdAndFolderIdIsNullAndIsDeletedFalse(projectId);
		for (CodeFile file : rootFiles) {
			rootNodes.add(convertToFileNode(file));
		}

		return rootNodes;
	}

	private void initializeNewProject(int projectId) {
		CodeFile readme = new CodeFile();
		readme.setName("README.md");
		readme.setExtension("md");
		readme.setContent("# Welcome to CodeSync\nCreated at: " + LocalDateTime.now());
		readme.setProjectId(projectId);
		readme.setPath("README.md");
		readme.setSize((long) readme.getContent().length());
		// Set safe default IDs to prevent constraint violations
		readme.setCreatedById(1);
		readme.setLastEditedBy(1);
		fileRepository.save(readme);
	}

	private FileNode buildFolderTree(Folder folder) {
		FileNode node = FileNode.builder().id("folder-" + folder.getFolderId()).name(folder.getName()).type("FOLDER")
				.children(new ArrayList<>()).build();

		// Recursive call for subfolders
		folderRepository.findByParentFolderIdAndIsDeletedFalse(folder.getFolderId())
				.forEach(sub -> node.getChildren().add(buildFolderTree(sub)));

		// Add files within this folder
		fileRepository.findByFolderIdAndIsDeletedFalse(folder.getFolderId())
				.forEach(f -> node.getChildren().add(convertToFileNode(f)));

		return node;
	}

	private FileNode convertToFileNode(CodeFile file) {
		return FileNode.builder().id("file-" + file.getFileId()).name(file.getName()).type("FILE")
				.content(file.getContent()).build();
	}

	@Override
	@Transactional
	public void deleteFolder(Long folderId) {
		Folder folder = folderRepository.findById(folderId)
				.orElseThrow(() -> new RuntimeException("Delete Failed: Folder not found"));

		// Soft delete the folder
		folder.setDeleted(true);
		folderRepository.save(folder);

		// Soft delete all files inside
		List<CodeFile> files = fileRepository.findByFolderIdAndIsDeletedFalse(folderId);
		files.forEach(f -> f.setDeleted(true));
		fileRepository.saveAll(files);

		// Recursively handle subfolders
		folderRepository.findByParentFolderIdAndIsDeletedFalse(folderId)
				.forEach(sub -> deleteFolder(sub.getFolderId()));
	}

	@Override
	@Transactional
	public void cloneProjectFiles(int sourceProjectId, int targetProjectId) {
		// Clone Hierarchy starting from root folders
		folderRepository.findByProjectIdAndParentFolderIdIsNullAndIsDeletedFalse(sourceProjectId)
				.forEach(f -> cloneFolderRecursive(f, null, targetProjectId));

		// Clone Root Files
		fileRepository.findByProjectIdAndFolderIdIsNullAndIsDeletedFalse(sourceProjectId).forEach(file -> {
			CodeFile newFile = new CodeFile();
			newFile.setName(file.getName());
			newFile.setContent(file.getContent());
			newFile.setExtension(file.getExtension());
			newFile.setProjectId(targetProjectId);
			newFile.setCreatedById(file.getCreatedById());
			createFile(newFile);
		});
	}

	private void cloneFolderRecursive(Folder sourceFolder, Long targetParentId, int targetProjectId) {
		Folder newFolder = new Folder();
		newFolder.setName(sourceFolder.getName());
		newFolder.setProjectId(targetProjectId);
		newFolder.setParentFolderId(targetParentId);
		newFolder = createFolder(newFolder);

		final Long currentNewFolderId = newFolder.getFolderId();

		// Subfolders
		folderRepository.findByParentFolderIdAndIsDeletedFalse(sourceFolder.getFolderId())
				.forEach(sub -> cloneFolderRecursive(sub, currentNewFolderId, targetProjectId));

		// Files
		fileRepository.findByFolderIdAndIsDeletedFalse(sourceFolder.getFolderId()).forEach(f -> {
			CodeFile nf = new CodeFile();
			nf.setName(f.getName());
			nf.setContent(f.getContent());
			nf.setExtension(f.getExtension());
			nf.setProjectId(targetProjectId);
			nf.setFolderId(currentNewFolderId.intValue());
			createFile(nf);
		});
	}

	@Override
	public void deleteFile(Long id) {
		fileRepository.findById(id).ifPresent(f -> {
			f.setDeleted(true);
			fileRepository.save(f);
		});
	}

	@Override
	public CodeFile renameFile(Long id, String name) {
		CodeFile f = fileRepository.findById(id).orElseThrow();
		f.setName(name);
		return fileRepository.save(f);
	}

	@Override
	public Folder renameFolder(Long id, String name) {
		Folder f = folderRepository.findById(id).orElseThrow();
		f.setName(name);
		return folderRepository.save(f);
	}

	@Override
	public List<CodeFile> searchInProject(int pid, String q) {
		return fileRepository.findByProjectIdAndContentContainingIgnoreCaseAndIsDeletedFalse(pid, q);
	}
}