package com.syncflow.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.syncflow.model.enums.*;

import com.syncflow.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    long countByRoleId(Long roleId);

    List<User> findByStatus(UserStatus status);

    List<User> findByRole_NameAndStatus(String roleName, UserStatus status);

    /** See UserRepositoryPort#findActiveByMentionToken; the LIKE patterns are pre-escaped. */
    @Query("""
            SELECT DISTINCT u FROM User u LEFT JOIN u.role r
            WHERE u.status = com.syncflow.model.enums.UserStatus.ACTIVE AND (
                   LOWER(u.email) LIKE :emailPattern ESCAPE '\\'
                OR LOWER(REPLACE(u.fullName, ' ', '')) = :token
                OR LOWER(REPLACE(u.fullName, ' ', '.')) = :token
                OR LOWER(u.fullName) = :token
                OR LOWER(u.fullName) LIKE :firstNamePattern ESCAPE '\\'
                OR LOWER(r.name) = :token
                OR LOWER(REPLACE(r.name, '_', '')) = :token)
            """)
    List<User> findActiveByMentionToken(@Param("token") String token,
                                        @Param("emailPattern") String emailPattern,
                                        @Param("firstNamePattern") String firstNamePattern);
}
