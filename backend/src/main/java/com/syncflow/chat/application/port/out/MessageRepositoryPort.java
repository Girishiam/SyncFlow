package com.syncflow.chat.application.port.out;

import com.syncflow.model.Message;
import java.util.List;
import java.util.Optional;

public interface MessageRepositoryPort {
    List<Message> findByChannelIdOrderByCreatedAtAsc(Long channelId);
    Optional<Message> findById(Long id);
    Message save(Message message);
    void deleteByChannelId(Long channelId);
}
