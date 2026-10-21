package com.syncflow.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDTO {
    private Long id;
    private Long channelId;
    private String channelName;
    private UUID senderId;
    private String sender;
    private String text;
    private String time;
    @Builder.Default
    private List<String> seenBy = new ArrayList<>();
}
