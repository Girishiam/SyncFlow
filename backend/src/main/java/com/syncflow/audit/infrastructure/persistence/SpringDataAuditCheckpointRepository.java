package com.syncflow.audit.infrastructure.persistence;

import com.syncflow.model.AuditChainCheckpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataAuditCheckpointRepository extends JpaRepository<AuditChainCheckpoint, Integer> {
}
