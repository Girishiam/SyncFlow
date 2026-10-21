package com.syncflow.auth.application.port.out;

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
}
