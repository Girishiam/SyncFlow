package com.syncflow.security;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
public class CustomPermissionEvaluator implements PermissionEvaluator {

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl userDetails)) {
            return false;
        }

        // System Admin bypasses all checks
        if ("ADMIN".equalsIgnoreCase(userDetails.getUser().getRole() != null ? userDetails.getUser().getRole().getName() : "")) {
            return true;
        }

        String requiredPerm = permission.toString().toUpperCase();
        return checkUserPermission(userDetails, requiredPerm);
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl userDetails)) {
            return false;
        }

        if ("ADMIN".equalsIgnoreCase(userDetails.getUser().getRole() != null ? userDetails.getUser().getRole().getName() : "")) {
            return true;
        }

        String compositePerm = targetType.toUpperCase() + ":" + permission.toString().toUpperCase();
        return checkUserPermission(userDetails, compositePerm);
    }

    @Cacheable(value = "user_permissions", key = "#userDetails.username + '_' + #requiredPerm")
    public boolean checkUserPermission(UserDetailsImpl userDetails, String requiredPerm) {
        return userDetails.getPermissionStrings().contains(requiredPerm);
    }
}
