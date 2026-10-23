package com.syncflow.task.infrastructure.storage;

import com.syncflow.common.domain.DomainException;
import com.syncflow.common.tx.AfterCommit;
import com.syncflow.config.SyncFlowProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.core.io.InputStreamSource;
import com.syncflow.task.application.port.out.AttachmentStoragePort;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Stores uploaded task attachments on the local filesystem under {@code syncflow.storage.attachments-dir}.
 * Storage keys are generated server-side ({@code <taskId>/<uuid>}), so user-supplied file names never
 * influence filesystem paths.
 */
@Component
@Slf4j
public class AttachmentStorage implements AttachmentStoragePort {

    private final Path root;

    public AttachmentStorage(SyncFlowProperties properties) throws IOException {
        this.root = Path.of(properties.getStorage().getAttachmentsDir()).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    @Override
    public StoredFile store(Long taskId, InputStreamSource content, long sizeBytes) {
        if (content == null || sizeBytes <= 0) {
            throw new DomainException("The uploaded file is empty");
        }
        String key = taskId + "/" + UUID.randomUUID();
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            try (InputStream in = content.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store attachment", e);
        }
        removeOnRollback(key);
        return new StoredFile(key, sizeBytes);
    }

    @Override
    public Resource load(String key) {
        Path path = resolve(key);
        if (!Files.isRegularFile(path)) {
            throw new com.syncflow.common.domain.NotFoundException("Attachment file is missing");
        }
        return new PathResource(path);
    }

    /** Deletes the file once the surrounding transaction has committed. */
    @Override
    public void deleteAfterCommit(String key) {
        if (key == null) return;
        AfterCommit.run(() -> deleteQuietly(key));
    }

    private void removeOnRollback(String key) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    deleteQuietly(key);
                }
            }
        });
    }

    private void deleteQuietly(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            log.warn("Could not delete attachment file {}: {}", key, e.getMessage());
        }
    }

    private Path resolve(String key) {
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root)) {
            throw new DomainException("Invalid attachment key");
        }
        return path;
    }
}
