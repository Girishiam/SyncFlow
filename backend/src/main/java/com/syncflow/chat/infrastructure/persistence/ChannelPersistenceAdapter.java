package com.syncflow.chat.infrastructure.persistence;

import com.syncflow.chat.application.port.out.ChannelRepositoryPort;
import com.syncflow.model.Channel;
import com.syncflow.repository.ChannelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ChannelPersistenceAdapter implements ChannelRepositoryPort {

    private final ChannelRepository channelRepository;

    @Override
    public List<Channel> findAll() {
        return channelRepository.findAll();
    }

    @Override
    public List<Channel> findVisibleTo(java.util.UUID userId) {
        return channelRepository.findVisibleTo(userId);
    }

    @Override
    public Optional<Channel> findById(Long id) {
        return channelRepository.findById(id);
    }

    @Override
    public Channel save(Channel channel) {
        return channelRepository.save(channel);
    }

    @Override
    public void deleteById(Long id) {
        channelRepository.deleteById(id);
    }

    @Override
    public boolean existsByName(String name) {
        return channelRepository.existsByName(name);
    }
}
