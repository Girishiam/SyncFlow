package com.syncflow.auth.application.port.out;

import com.syncflow.model.Permission;
import java.util.List;
import java.util.Optional;

public interface PermissionRepositoryPort {
    List<Permission> findAll();
    Optional<Permission> findById(Long id);
    List<Permission> findAllById(Iterable<Long> ids);
}
