package com.syncflow.repository;

import com.syncflow.model.Stage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StageRepository extends JpaRepository<Stage, Long> {
    List<Stage> findByProjectIdOrderByPositionAsc(Long projectId);

    @org.springframework.data.jpa.repository.Query("SELECT s.project.id FROM Stage s WHERE s.id = :id")
    java.util.Optional<Long> findProjectIdById(@org.springframework.data.repository.query.Param("id") Long id);
}
