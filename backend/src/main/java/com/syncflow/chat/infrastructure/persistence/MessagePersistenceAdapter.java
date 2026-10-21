package com.syncflow.chat.infrastructure.persistence;

import com.syncflow.chat.application.port.out.MessageRepositoryPort;
import com.syncflow.chat.application.port.out.MessageSeenReceiptRepositoryPort;
import com.syncflow.model.Message;
import com.syncflow.model.MessageSeenReceipt;
import com.syncflow.repository.MessageRepository;
import com.syncflow.repository.MessageSeenReceiptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MessagePersistenceAdapter implements MessageRepositoryPort, MessageSeenReceiptRepositoryPort {

    private final MessageRepository messageRepository;
    private final MessageSeenReceiptRepository receiptRepository;

    @Override
    public List<Message> findByChannelIdOrderByCreatedAtAsc(Long channelId) {
        return messageRepository.findByChannelIdOrderByCreatedAtAsc(channelId);
    }

    @Override
    public Optional<Message> findById(Long id) {
        return messageRepository.findById(id);
    }

    @Override
    public Message save(Message message) {
        return messageRepository.save(message);
    }

    @Override
    public void deleteByChannelId(Long channelId) {
        receiptRepository.deleteByChannelId(channelId);
        messageRepository.deleteByChannelId(channelId);
    }

    @Override
    public List<MessageSeenReceipt> findByMessageId(Long messageId) {
        return receiptRepository.findByMessageId(messageId);
    }

    @Override
    public MessageSeenReceipt save(MessageSeenReceipt receipt) {
        return receiptRepository.save(receipt);
    }
}
