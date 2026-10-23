package com.syncflow.task;

import com.syncflow.dto.WorkflowGateValidationResultDTO;
import com.syncflow.model.*;
import com.syncflow.model.enums.DependencyType;
import com.syncflow.task.application.port.out.SubtaskRepositoryPort;
import com.syncflow.task.application.port.out.TaskAttachmentRepositoryPort;
import com.syncflow.task.application.port.out.TaskDependencyRepositoryPort;
import com.syncflow.task.application.port.out.TaskRepositoryPort;
import com.syncflow.task.application.service.WorkflowGateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WorkflowGateServiceTest {

    private SubtaskRepositoryPort subtasks;
    private TaskAttachmentRepositoryPort attachments;
    private TaskDependencyRepositoryPort dependencies;
    private TaskRepositoryPort tasks;
    private WorkflowGateService service;
    private Task task;

    @BeforeEach
    void setUp() {
        subtasks = mock(SubtaskRepositoryPort.class);
        attachments = mock(TaskAttachmentRepositoryPort.class);
        dependencies = mock(TaskDependencyRepositoryPort.class);
        tasks = mock(TaskRepositoryPort.class);
        service = new WorkflowGateService(subtasks, attachments, dependencies, tasks);
        task = Task.builder().id(1L).title("T").isBlocked(false).build();
        when(dependencies.findBySourceTaskId(1L)).thenReturn(List.of());
        when(dependencies.findByTargetTaskId(1L)).thenReturn(List.of());
        when(attachments.findByTaskIdOrderByUploadedAtDesc(1L)).thenReturn(List.of());
    }

    @Test
    void stageWithoutGatesAlwaysAllows() {
        Stage free = Stage.builder().name("Review / QA").build(); // name no longer implies a gate
        when(subtasks.findByTaskIdOrderByPositionAsc(1L)).thenReturn(List.of(Subtask.builder().isCompleted(false).build()));

        WorkflowGateValidationResultDTO result = service.evaluate(task, free);

        assertThat(result.getAllowed()).isTrue();
        verifyNoInteractions(subtasks);
    }

    @Test
    void checklistGateCountsOpenItems() {
        Stage review = Stage.builder().name("Anything").requiresChecklistComplete(true).build();
        when(subtasks.findByTaskIdOrderByPositionAsc(1L)).thenReturn(List.of(
                Subtask.builder().isCompleted(true).build(),
                Subtask.builder().isCompleted(false).build()));

        WorkflowGateValidationResultDTO result = service.evaluate(task, review);

        assertThat(result.getAllowed()).isFalse();
        assertThat(result.getUncompletedSubtasksCount()).isEqualTo(1);
    }

    @Test
    void deliverableGateAcceptsPullRequestLink() {
        Stage done = Stage.builder().name("Done").isDoneStage(true).requiresDeliverable(true).build();
        task.setGitPrUrl("https://github.com/org/repo/pull/1");

        assertThat(service.evaluate(task, done).getAllowed()).isTrue();
    }

    @Test
    void noBlockersGateIgnoresFinishedBlockers() {
        Stage done = Stage.builder().name("Done").isDoneStage(true).requiresNoBlockers(true).build();
        Stage doneStage = Stage.builder().isDoneStage(true).build();
        Stage openStage = Stage.builder().isDoneStage(false).build();
        when(dependencies.findBySourceTaskId(1L)).thenReturn(List.of(
                TaskDependency.builder().sourceTaskId(1L).targetTaskId(2L).dependencyType(DependencyType.BLOCKED_BY).build()));
        when(dependencies.findByTargetTaskId(1L)).thenReturn(List.of(
                TaskDependency.builder().sourceTaskId(3L).targetTaskId(1L).dependencyType(DependencyType.BLOCKS).build()));
        when(tasks.findAllById(any())).thenReturn(List.of(
                Task.builder().id(2L).title("Finished").stage(doneStage).build(),
                Task.builder().id(3L).title("Still open").stage(openStage).build()));

        WorkflowGateValidationResultDTO result = service.evaluate(task, done);

        assertThat(result.getAllowed()).isFalse();
        assertThat(result.getBlockingTaskTitles()).containsExactly("Still open");
    }
}
