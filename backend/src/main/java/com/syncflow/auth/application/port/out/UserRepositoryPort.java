package com.syncflow.auth.application.port.out;

import com.syncflow.model.enums.UserStatus;

import com.syncflow.model.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepositoryPort {
    Optional<User> findById(UUID id);
    Optional<User> findByEmail(String email);
    Boolean existsByEmail(String email);
    List<User> findAll();
    User save(User user);

    long countByRoleId(Long roleId);

    List<User> findByStatus(UserStatus status);

    List<User> findByRoleNameAndStatus(String roleName, UserStatus status);

    /**
     * Active users addressed by an @mention token: full name without spaces or with dots, first
     * name, e-mail local part, or role name (with or without underscores). Token is lower case.
     */
    List<User> findActiveByMentionToken(String token);
}
