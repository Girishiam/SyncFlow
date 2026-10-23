package com.syncflow.task.application.port.out;

import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;

/** Binary storage for uploaded task attachments. */
public interface AttachmentStoragePort {

    record StoredFile(String key, long sizeBytes) {
    }

    /** Stores the content under a server-generated key; removed again if the transaction rolls back. */
    StoredFile store(Long taskId, InputStreamSource content, long sizeBytes);

    Resource load(String key);

    /** Deletes the stored file once the surrounding transaction commits. */
    void deleteAfterCommit(String key);
}
