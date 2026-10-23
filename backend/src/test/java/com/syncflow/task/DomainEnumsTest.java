package com.syncflow.task;

import com.syncflow.common.domain.DomainException;
import com.syncflow.model.enums.DependencyType;
import com.syncflow.model.enums.NotificationPriority;
import com.syncflow.model.enums.TaskPriority;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DomainEnumsTest {

    @Test
    void priorityParsingIsLenientAboutCaseAndDefaultsToMedium() {
        assertThat(TaskPriority.parse(" high ")).isEqualTo(TaskPriority.HIGH);
        assertThat(TaskPriority.parse(null)).isEqualTo(TaskPriority.MEDIUM);
        assertThatThrownBy(() -> TaskPriority.parse("urgent")).isInstanceOf(DomainException.class);
    }

    @Test
    void dependencyInverseDescribesTheLinkFromTheOtherSide() {
        assertThat(DependencyType.BLOCKS.inverse()).isEqualTo(DependencyType.BLOCKED_BY);
        assertThat(DependencyType.BLOCKED_BY.inverse()).isEqualTo(DependencyType.BLOCKS);
        assertThat(DependencyType.RELATES_TO.inverse()).isEqualTo(DependencyType.RELATES_TO);
        assertThatThrownBy(() -> DependencyType.parse("DEPENDS")).isInstanceOf(DomainException.class);
    }

    @Test
    void notificationPriorityFollowsTaskPriority() {
        assertThat(NotificationPriority.forTask(TaskPriority.CRITICAL)).isEqualTo(NotificationPriority.CRITICAL);
        assertThat(NotificationPriority.forTask(TaskPriority.HIGH)).isEqualTo(NotificationPriority.WARNING);
        assertThat(NotificationPriority.forTask(TaskPriority.LOW)).isEqualTo(NotificationPriority.INFO);
    }
}
