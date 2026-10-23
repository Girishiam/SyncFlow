package com.syncflow.chat.application.port.out;

import com.syncflow.model.MessageSeenReceipt;
import java.util.List;

public interface MessageSeenReceiptRepositoryPort {
    List<MessageSeenReceipt> findByMessageId(Long messageId);

    List<MessageSeenReceipt> findByMessageIds(java.util.Collection<Long> messageIds);
    MessageSeenReceipt save(MessageSeenReceipt receipt);
    void deleteByChannelId(Long channelId);
}
