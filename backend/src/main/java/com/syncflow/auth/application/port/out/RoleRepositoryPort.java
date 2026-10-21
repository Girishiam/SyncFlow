package com.syncflow.auth.application.port.out;

import com.syncflow.model.Role;
import java.util.Optional;

public interface RoleRepositoryPort {
    Optional<Role> findByName(String name);
    Role save(Role role);
}
