package com.syncflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSummaryDTO {
    private long unreadTotal;
    private long unreadMentions;
    private long unreadTasks;
    private long unreadDeadlines;
    private long unreadBlockers;
}
