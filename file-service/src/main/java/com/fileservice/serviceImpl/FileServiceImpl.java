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
	public Folder createFolder(Folder folder) {
		folder.setDeleted(false);
		return folderRepository.save(folder);
	}

	@Override
	public CodeFile createFile(CodeFile file) {
		file.setDeleted(false);
		file.setCreatedAt(LocalDateTime.now());
		file.setUpdatedAt(LocalDateTime.now());
		return fileRepository.save(file);
	}

	@Override
	public CodeFile updateFileContent(Long fileId, String content, int userId) {
		CodeFile file = fileRepository.findById(fileId).orElseThrow(() -> new RuntimeException("File not found"));
		file.setContent(content);
		file.setLastEditedBy(userId);
		file.setUpdatedAt(LocalDateTime.now());
		file.setSize((long) content.length());
		return fileRepository.save(file);
	}

	@Override
	public List<FileNode> getProjectTree(int projectId) {
		if (folderRepository.findByProjectId(projectId).isEmpty()
				&& fileRepository.findByProjectIdAndIsDeletedFalse(projectId).isEmpty()) {
			initializeNewProject(projectId);
		}

		List<FileNode> rootNodes = new ArrayList<>();
		List<Folder> rootFolders = folderRepository.findByProjectIdAndParentFolderIdIsNullAndIsDeletedFalse(projectId);
		for (Folder folder : rootFolders) {
			rootNodes.add(buildFolderTree(folder));
		}

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
		readme.setContent("# Welcome to CodeSync\nStart coding by creating new files.");
		readme.setProjectId(projectId);
		readme.setPath("README.md");
		fileRepository.save(readme);
	}

	private FileNode buildFolderTree(Folder folder) {
		FileNode node = new FileNode();
		node.setId("folder-" + folder.getFolderId());
		node.setName(folder.getName());
		node.setType("FOLDER");

		List<Folder> subFolders = folderRepository.findByParentFolderIdAndIsDeletedFalse(folder.getFolderId());
		for (Folder sub : subFolders) {
			node.getChildren().add(buildFolderTree(sub));
		}

		List<CodeFile> files = fileRepository.findByFolderIdAndIsDeletedFalse(folder.getFolderId());
		for (CodeFile f : files) {
			node.getChildren().add(convertToFileNode(f));
		}
		return node;
	}

	private FileNode convertToFileNode(CodeFile file) {
		FileNode node = new FileNode();
		node.setId("file-" + file.getFileId());
		node.setName(file.getName());
		node.setType("FILE");
		node.setContent(file.getContent());
		return node;
	}

	@Override
	public void deleteFile(Long fileId) {
		CodeFile file = fileRepository.findById(fileId).orElseThrow(() -> new RuntimeException("File not found"));
		file.setDeleted(true);
		fileRepository.save(file);
	}

	@Override
	@Transactional
	public void deleteFolder(Long folderId) {
		Folder folder = folderRepository.findById(folderId).orElseThrow(() -> new RuntimeException("Folder not found"));
		folder.setDeleted(true);
		folderRepository.save(folder);

		List<CodeFile> files = fileRepository.findByFolderIdAndIsDeletedFalse(folderId);
		files.forEach(f -> f.setDeleted(true));
		fileRepository.saveAll(files);

		List<Folder> subFolders = folderRepository.findByParentFolderIdAndIsDeletedFalse(folderId);
		for (Folder sub : subFolders) {
			deleteFolder(sub.getFolderId());
		}
	}

	@Override
	public CodeFile renameFile(Long fileId, String newName) {
		CodeFile file = fileRepository.findById(fileId).orElseThrow(() -> new RuntimeException("File not found"));
		file.setName(newName);
		if (newName.contains(".")) {
			file.setExtension(newName.substring(newName.lastIndexOf(".") + 1));
		}
		return fileRepository.save(file);
	}

	@Override
	public Folder renameFolder(Long folderId, String newName) {
		Folder folder = folderRepository.findById(folderId).orElseThrow(() -> new RuntimeException("File not found"));
		folder.setName(newName);
		return folderRepository.save(folder);
	}

	@Override
	public List<CodeFile> searchInProject(int projectId, String query) {
		return fileRepository.findByProjectIdAndContentContainingAndIsDeletedFalse(projectId, query);
	}

	// Inside FileServiceImpl.java

	@Override
	@Transactional
	public void cloneProjectFiles(int sourceProjectId, int targetProjectId) {
		// 1. Clone root folders recursively
		List<Folder> rootFolders = folderRepository
				.findByProjectIdAndParentFolderIdIsNullAndIsDeletedFalse(sourceProjectId);
		for (Folder f : rootFolders) {
			cloneFolderRecursive(f, null, targetProjectId);
		}

		// 2. Clone root files
		List<CodeFile> rootFiles = fileRepository.findByProjectIdAndFolderIdIsNullAndIsDeletedFalse(sourceProjectId);
		for (CodeFile file : rootFiles) {
			CodeFile newFile = new CodeFile();
			newFile.setName(file.getName());
			newFile.setContent(file.getContent());
			newFile.setExtension(file.getExtension());
			newFile.setPath(file.getPath());
			newFile.setSize(file.getSize());
			newFile.setProjectId(targetProjectId);
			newFile.setCreatedById(file.getCreatedById());
			newFile.setDeleted(false);
			createFile(newFile);
		}
	}

	private void cloneFolderRecursive(Folder sourceFolder, Long parentFolderId, int targetProjectId) {
		Folder newFolder = new Folder();
		newFolder.setName(sourceFolder.getName());
		newFolder.setProjectId(targetProjectId);
		newFolder.setParentFolderId(parentFolderId);
		newFolder.setDeleted(false);
		newFolder = createFolder(newFolder);

		// Recursively clone sub-folders
		List<Folder> subFolders = folderRepository.findByParentFolderIdAndIsDeletedFalse(sourceFolder.getFolderId());
		for (Folder sub : subFolders) {
			cloneFolderRecursive(sub, newFolder.getFolderId(), targetProjectId);
		}

		// Clone all files resting inside this folder
		List<CodeFile> files = fileRepository.findByFolderIdAndIsDeletedFalse(sourceFolder.getFolderId());
		for (CodeFile f : files) {
			CodeFile newFile = new CodeFile();
			newFile.setName(f.getName());
			newFile.setContent(f.getContent()); // FIXED: changed newCode to newFile
			newFile.setExtension(f.getExtension());
			newFile.setPath(f.getPath());
			newFile.setSize(f.getSize());
			newFile.setProjectId(targetProjectId);
			newFile.setFolderId(newFolder.getFolderId().intValue()); // Mapping to the newly created folder ID
			newFile.setDeleted(false);
			createFile(newFile);
		}
	}
}