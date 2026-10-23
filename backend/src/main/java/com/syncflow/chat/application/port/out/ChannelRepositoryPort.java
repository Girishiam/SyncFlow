package com.syncflow.chat.application.port.out;

import com.syncflow.model.Channel;
import java.util.List;
import java.util.Optional;

public interface ChannelRepositoryPort {
    List<Channel> findAll();

    List<Channel> findVisibleTo(java.util.UUID userId);
    Optional<Channel> findById(Long id);
    Channel save(Channel channel);
    void deleteById(Long id);
    boolean existsByName(String name);
}
