package com.fileservice.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class FileNode {
    private String id;       // prefixed id like "file-1" or "folder-1"
    private String name;
    private String type;     // "FILE" or "FOLDER"
    private String content;  // actual code (only for files)
    private List<FileNode> children = new ArrayList<>();
}