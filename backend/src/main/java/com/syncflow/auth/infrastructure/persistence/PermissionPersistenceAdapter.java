package com.syncflow.auth.infrastructure.persistence;

import com.syncflow.auth.application.port.out.PermissionRepositoryPort;
import com.syncflow.model.Permission;
import com.syncflow.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PermissionPersistenceAdapter implements PermissionRepositoryPort {

    private final PermissionRepository permissionRepository;

    @Override
    public List<Permission> findAll() {
        return permissionRepository.findAll();
    }

    @Override
    public Optional<Permission> findById(Long id) {
        return permissionRepository.findById(id);
    }

    @Override
    public List<Permission> findAllById(Iterable<Long> ids) {
        return permissionRepository.findAllById(ids);
    }
}
