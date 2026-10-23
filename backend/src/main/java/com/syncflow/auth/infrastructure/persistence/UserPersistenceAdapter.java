package com.syncflow.auth.infrastructure.persistence;

import com.syncflow.model.enums.UserStatus;

import com.syncflow.auth.application.port.out.UserRepositoryPort;
import com.syncflow.model.User;
import com.syncflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserPersistenceAdapter implements UserRepositoryPort {

    private final UserRepository userRepository;

    @Override
    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public Boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public User save(User user) {
        return userRepository.save(user);
    }

    @Override
    public long countByRoleId(Long roleId) {
        return userRepository.countByRoleId(roleId);
    }

    @Override
    public List<User> findByStatus(UserStatus status) {
        return userRepository.findByStatus(status);
    }

    @Override
    public List<User> findByRoleNameAndStatus(String roleName, UserStatus status) {
        return userRepository.findByRole_NameAndStatus(roleName, status);
    }

    @Override
    public List<User> findActiveByMentionToken(String token) {
        String escaped = token.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return userRepository.findActiveByMentionToken(token, escaped + "@%", escaped + " %");
    }
}
