package com.fileservice.serviceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fileservice.dto.FileNode;
import com.fileservice.entity.CodeFile;
import com.fileservice.entity.Folder;
import com.fileservice.repository.CodeFileRepository;
import com.fileservice.repository.FolderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class FileServiceImplTest {

	@Mock
	private FolderRepository folderRepository;

	@Mock
	private CodeFileRepository fileRepository;

	@InjectMocks
	private FileServiceImpl fileService;

	private Folder mockFolder;
	private CodeFile mockFile;
	private final int projectId = 1;

	@BeforeEach
	void setUp() {
		mockFolder = new Folder();
		mockFolder.setFolderId(1L);
		mockFolder.setName("src");
		mockFolder.setProjectId(projectId);
		mockFolder.setDeleted(false);

		mockFile = new CodeFile();
		mockFile.setFileId(1L);
		mockFile.setName("App.java");
		mockFile.setExtension("java");
		mockFile.setContent("System.out.println();");
		mockFile.setProjectId(projectId);
		mockFile.setDeleted(false);
	}

	@Test
	void testCreateFolder() {
		when(folderRepository.save(any(Folder.class))).thenReturn(mockFolder);
		Folder result = fileService.createFolder(new Folder());
		assertNotNull(result);
		assertFalse(result.isDeleted());
		verify(folderRepository).save(any());
	}

	@Test
	void testUpdateFileContent() {
		when(fileRepository.findById(1L)).thenReturn(Optional.of(mockFile));
		when(fileRepository.save(any(CodeFile.class))).thenReturn(mockFile);

		CodeFile result = fileService.updateFileContent(1L, "new content", 123);

		assertEquals("new content", result.getContent());
		assertEquals(123, result.getLastEditedBy());
		assertEquals(11, result.getSize()); // "new content" length
		verify(fileRepository).save(mockFile);
	}

	@Test
	void testGetProjectTree_InitializesEmptyProject() {
		// Mock empty repository responses
		when(folderRepository.findByProjectId(projectId)).thenReturn(Collections.emptyList());
		when(fileRepository.findByProjectIdAndIsDeletedFalse(projectId)).thenReturn(Collections.emptyList());

		// Mock responses for building the tree after initialization
		when(folderRepository.findByProjectIdAndParentFolderIdIsNullAndIsDeletedFalse(projectId))
				.thenReturn(Collections.emptyList());
		when(fileRepository.findByProjectIdAndFolderIdIsNullAndIsDeletedFalse(projectId))
				.thenReturn(Collections.emptyList());

		fileService.getProjectTree(projectId);

		// Verify README was saved
		verify(fileRepository, atLeastOnce()).save(argThat(file -> file.getName().equals("README.md")));
	}

	@Test
	void testGetProjectTree_RecursiveStructure() {
		// Root: 1 Folder ("src")
		when(folderRepository.findByProjectIdAndParentFolderIdIsNullAndIsDeletedFalse(projectId))
				.thenReturn(List.of(mockFolder));

		// Inside "src": 1 File ("App.java")
		when(folderRepository.findByParentFolderIdAndIsDeletedFalse(1L)).thenReturn(Collections.emptyList());
		when(fileRepository.findByFolderIdAndIsDeletedFalse(1L)).thenReturn(List.of(mockFile));

		List<FileNode> tree = fileService.getProjectTree(projectId);

		assertEquals(1, tree.size());
		assertEquals("folder-1", tree.get(0).getId());
		assertEquals(1, tree.get(0).getChildren().size());
		assertEquals("App.java", tree.get(0).getChildren().get(0).getName());
	}

	@Test
	void testDeleteFolder_SoftDeleteRecursive() {
		when(folderRepository.findById(1L)).thenReturn(Optional.of(mockFolder));
		// Mock sub-contents
		when(fileRepository.findByFolderIdAndIsDeletedFalse(1L)).thenReturn(List.of(mockFile));
		when(folderRepository.findByParentFolderIdAndIsDeletedFalse(1L)).thenReturn(Collections.emptyList());

		fileService.deleteFolder(1L);

		assertTrue(mockFolder.isDeleted());
		assertTrue(mockFile.isDeleted());
		verify(folderRepository).save(mockFolder);
		verify(fileRepository).saveAll(anyList());
	}

	@Test
	void testRenameFile_WithExtensionUpdate() {
		when(fileRepository.findById(1L)).thenReturn(Optional.of(mockFile));
		when(fileRepository.save(any(CodeFile.class))).thenReturn(mockFile);

		CodeFile result = fileService.renameFile(1L, "Main.py");

		assertEquals("Main.py", result.getName());
		assertEquals("py", result.getExtension());
	}

	@Test
	void testCloneProjectFiles() {
		int targetId = 2;

		when(folderRepository.findByProjectIdAndParentFolderIdIsNullAndIsDeletedFalse(projectId))
				.thenReturn(List.of(mockFolder));
		when(fileRepository.findByProjectIdAndFolderIdIsNullAndIsDeletedFalse(projectId))
				.thenReturn(Collections.emptyList());

		when(folderRepository.findByParentFolderIdAndIsDeletedFalse(1L)).thenReturn(Collections.emptyList());
		when(fileRepository.findByFolderIdAndIsDeletedFalse(1L)).thenReturn(List.of(mockFile));

		when(folderRepository.save(any(Folder.class))).thenAnswer(invocation -> {
			Folder folder = invocation.getArgument(0);
			if (folder.getFolderId() == null) {
				folder.setFolderId(999L);
			}
			return folder;
		});

		when(fileRepository.save(any(CodeFile.class))).thenAnswer(i -> i.getArgument(0));

		assertDoesNotThrow(() -> fileService.cloneProjectFiles(projectId, targetId));

		verify(folderRepository, atLeastOnce()).save(argThat(f -> f.getProjectId() == targetId));
	}

	@Test
	void testSearchInProject() {
		when(fileRepository.findByProjectIdAndContentContainingAndIsDeletedFalse(projectId, "System"))
				.thenReturn(List.of(mockFile));

		List<CodeFile> results = fileService.searchInProject(projectId, "System");

		assertEquals(1, results.size());
		assertTrue(results.get(0).getContent().contains("System"));
	}

	@Test
	void testFileNotFoundException() {
		when(fileRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(RuntimeException.class, () -> {
			fileService.updateFileContent(99L, "text", 1);
		});
	}
}