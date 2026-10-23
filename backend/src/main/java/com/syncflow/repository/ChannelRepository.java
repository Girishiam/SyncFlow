package com.syncflow.repository;

import com.syncflow.model.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChannelRepository extends JpaRepository<Channel, Long> {
    List<Channel> findByProject_Id(Long projectId);
    List<Channel> findByProjectIsNull();
    Optional<Channel> findByName(String name);
    boolean existsByName(String name);

    @Query("SELECT COUNT(c) > 0 FROM Channel c JOIN c.members m WHERE c.id = :channelId AND m.id = :userId")
    boolean isMember(@Param("channelId") Long channelId, @Param("userId") UUID userId);

    /** Public channels plus private channels the user belongs to. */
    @Query("""
            SELECT DISTINCT c FROM Channel c LEFT JOIN c.members m
            WHERE c.isPrivate = false OR m.id = :userId
            ORDER BY c.id
            """)
    List<Channel> findVisibleTo(@Param("userId") UUID userId);
}
