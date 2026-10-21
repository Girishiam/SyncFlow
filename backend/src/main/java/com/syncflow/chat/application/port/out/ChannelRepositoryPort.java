package com.syncflow.chat.application.port.out;

import com.syncflow.model.Channel;
import java.util.List;
import java.util.Optional;

public interface ChannelRepositoryPort {
    List<Channel> findAll();
    Optional<Channel> findById(Long id);
    Channel save(Channel channel);
}
