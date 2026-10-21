package com.syncflow.dto;

import lombok.*;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskAttachmentDTO {
    private Long id;
    private Long taskId;
    private String fileName;
    private String fileType;
    private String fileSize;
    private String fileUrl;
    private String uploadedBy;
    private Instant uploadedAt;
    private String timeAgo;
}
