package com.syncflow.chat.application.port.out;

import com.syncflow.model.MessageSeenReceipt;
import java.util.List;

public interface MessageSeenReceiptRepositoryPort {
    List<MessageSeenReceipt> findByMessageId(Long messageId);
    MessageSeenReceipt save(MessageSeenReceipt receipt);
}
