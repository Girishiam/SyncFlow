package com.syncflow.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskCommentDTO {
    private Long id;
    private Long taskId;
    private UUID authorId;
    private String authorName;
    private String authorRole;
    private String authorAvatar;
    private String content;
    private String time;
    private String attachmentUrl;
}
