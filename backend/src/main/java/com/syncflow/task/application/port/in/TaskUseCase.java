package com.syncflow.task.application.port.in;

import com.syncflow.dto.AddCommentRequest;
import com.syncflow.dto.CreateTaskRequest;
import com.syncflow.dto.MoveTaskRequest;
import com.syncflow.dto.SetBlockerRequest;
import com.syncflow.dto.TaskCommentDTO;
import com.syncflow.dto.TaskDTO;

import com.syncflow.dto.AssignTaskRequest;
import com.syncflow.dto.UpdateDeadlineRequest;

import java.util.List;
import java.util.UUID;

public interface TaskUseCase {
    TaskDTO createTask(CreateTaskRequest request);
    TaskDTO moveTask(Long id, MoveTaskRequest request);
    TaskDTO updateProgress(Long id, Integer progress);
    TaskDTO setBlocker(Long id, SetBlockerRequest request);
    TaskDTO getTaskDetails(Long id);
    List<TaskCommentDTO> getTaskComments(Long taskId);
    TaskCommentDTO addComment(Long taskId, AddCommentRequest request, UUID authorId);
    TaskDTO assignTask(Long id, AssignTaskRequest request);
    TaskDTO updateDeadline(Long id, UpdateDeadlineRequest request);

    // Subtasks & Deliverable QA Checklist
    List<com.syncflow.dto.SubtaskDTO> getSubtasks(Long taskId);
    com.syncflow.dto.SubtaskDTO addSubtask(Long taskId, String title, String authorName);
    com.syncflow.dto.SubtaskDTO toggleSubtask(Long taskId, Long subtaskId, String actorName);
    void deleteSubtask(Long taskId, Long subtaskId);

    // Artifacts & File Attachments
    List<com.syncflow.dto.TaskAttachmentDTO> getAttachments(Long taskId);
    com.syncflow.dto.TaskAttachmentDTO addAttachment(Long taskId, String fileName, String fileType, String fileSize, String fileUrl, String uploadedBy);
    void deleteAttachment(Long taskId, Long attachmentId);
}
