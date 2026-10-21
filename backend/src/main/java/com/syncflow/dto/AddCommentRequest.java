package com.syncflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddCommentRequest {
    @NotBlank(message = "Comment content cannot be empty")
    private String content;
    private String attachmentUrl;
}
