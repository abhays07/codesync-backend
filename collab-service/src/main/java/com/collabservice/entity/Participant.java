package com.collabservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "participants")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Participant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long participantId;
    
    @Column(length = 36)
    private String sessionId;
    
    private int userId;
    
    @Column(length = 20)
    private String role; // HOST, EDITOR, VIEWER 
    
    @Column(length = 7)
    private String color; // Hex code for cursor (e.g., #FF0000)
    
    private int cursorLine;
    private int cursorCol;
    
    private LocalDateTime joinedAt = LocalDateTime.now();
    private LocalDateTime leftAt;
}