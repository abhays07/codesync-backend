package com.versionservice.serviceTest;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.versionservice.entity.Snapshot;
import com.versionservice.serviceImpl.VersionServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class VersionServiceImplTest {

    @Mock
    private com.versionservice.repository.SnapshotRepository repository;

    @InjectMocks
    private VersionServiceImpl versionService;

    private Snapshot baseSnapshot;
    private Snapshot headSnapshot;

    @BeforeEach
    void setUp() {
        baseSnapshot = Snapshot.builder()
                .id(1L)
                .fileId(10)
                .content("public class Hello {\n    // Old Code\n}")
                .build();

        headSnapshot = Snapshot.builder()
                .id(2L)
                .fileId(10)
                .content("public class Hello {\n    // New Code\n}")
                .build();
    }

    @Test
    void testCreateSnapshot_IntegrityAndHashing() {
        // Arrange
        when(repository.save(any(Snapshot.class))).thenAnswer(i -> i.getArguments()[0]);
        when(repository.findFirstByFileIdOrderByCreatedAtDesc(10)).thenReturn(Optional.empty());

        // Act
        Snapshot result = versionService.createSnapshot(baseSnapshot);

        // Assert
        assertNotNull(result.getHash());
        assertEquals(64, result.getHash().length()); // SHA-256 is 64 chars hex
        verify(repository).save(any(Snapshot.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testCompareSnapshots_MyersDiffLogic() {
        // Arrange
        when(repository.findById(1L)).thenReturn(Optional.of(baseSnapshot));
        when(repository.findById(2L)).thenReturn(Optional.of(headSnapshot));

        // Act
        Map<String, Object> diffResult = versionService.compareSnapshots(1L, 2L);

        // Assert
        assertTrue(diffResult.containsKey("changes"));
        List<String> changes = (List<String>) diffResult.get("changes");
        
        // Myers diff should catch the line change
        assertFalse(changes.isEmpty());
        assertTrue(changes.get(0).contains("New Code") || changes.get(0).contains("CHANGE"));
    }

    @Test
    void testRestoreVersion_NonDestructiveFlow() {
        // Arrange
        when(repository.findById(1L)).thenReturn(Optional.of(baseSnapshot));
        when(repository.save(any(Snapshot.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        Snapshot restored = versionService.restoreVersion(1L, 500);

        // Assert
        assertEquals(baseSnapshot.getContent(), restored.getContent());
        assertTrue(restored.getCommitMessage().contains("Restored"));
        assertEquals(500, restored.getUserId());
        // Verify it was treated as a NEW snapshot creation
        verify(repository, times(1)).save(any(Snapshot.class));
    }
}