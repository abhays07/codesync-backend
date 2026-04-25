package com.versionservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "snapshots")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Builder
public class Snapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer fileId;
    private Integer userId;
    private String username;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String commitMessage;
    
    // For data integrity (Requirement 2.7)
    private String hash; 
    
    // For the Version History DAG (Directed Acyclic Graph)
    private Long parentSnapshotId; 

    private LocalDateTime createdAt = LocalDateTime.now();
}