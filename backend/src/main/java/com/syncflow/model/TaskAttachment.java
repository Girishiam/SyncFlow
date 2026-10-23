package com.syncflow.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "task_attachments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_type", nullable = false, length = 50)
    private String fileType; // image, pdf, code, link, log

    @Column(name = "file_size", length = 50)
    private String fileSize;

    /** External link for link attachments, or the API download path for uploaded files. */
    @Column(name = "file_url", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String fileUrl;

    /** Relative path of the stored file under the attachments directory; null for link attachments. */
    @Column(name = "storage_key", length = 255)
    private String storageKey;

    @Column(name = "content_type", length = 150)
    private String contentType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "uploaded_by", nullable = false, length = 150)
    private String uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    @Builder.Default
    private Instant uploadedAt = Instant.now();
}
