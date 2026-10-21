package com.syncflow.auth.application.port.out;

import com.syncflow.model.Role;
import java.util.List;
import java.util.Optional;

public interface RoleRepositoryPort {
    List<Role> findAll();
    Optional<Role> findById(Long id);
    Optional<Role> findByName(String name);
    Role save(Role role);
}
