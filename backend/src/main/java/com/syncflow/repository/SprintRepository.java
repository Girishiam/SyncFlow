package com.syncflow.repository;

import com.syncflow.model.Sprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SprintRepository extends JpaRepository<Sprint, Long> {
    List<Sprint> findByProjectIdOrderByStartDateDesc(Long projectId);
    Optional<Sprint> findFirstByProjectIdAndStatus(Long projectId, String status);
    List<Sprint> findByProjectIdAndStatus(Long projectId, String status);
}
