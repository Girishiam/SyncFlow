package com.syncflow.repository;

import com.syncflow.model.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChannelRepository extends JpaRepository<Channel, Long> {
    List<Channel> findByProjectId(Long projectId);
    List<Channel> findByProjectIsNull();
    Optional<Channel> findByName(String name);
}
