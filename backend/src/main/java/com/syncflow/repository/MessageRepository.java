package com.syncflow.repository;

import com.syncflow.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByChannelIdOrderByCreatedAtAsc(Long channelId);
    void deleteByChannelId(Long channelId);

    /** Newest messages first; callers reverse for display. */
    List<Message> findByChannelIdOrderByIdDesc(Long channelId, Pageable pageable);

    List<Message> findByChannelIdAndIdLessThanOrderByIdDesc(Long channelId, Long beforeId, Pageable pageable);
}
