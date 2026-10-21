package com.syncflow.repository;

import com.syncflow.model.MessageSeenReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageSeenReceiptRepository extends JpaRepository<MessageSeenReceipt, Long> {
    List<MessageSeenReceipt> findByMessageId(Long messageId);
    Optional<MessageSeenReceipt> findByMessageIdAndUserId(Long messageId, UUID userId);
    void deleteByChannelId(Long channelId);
}
