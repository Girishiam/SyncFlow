package com.syncflow.security;

import com.syncflow.model.Permission;
import com.syncflow.model.User;
import com.syncflow.model.UserPermissionOverride;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;
import java.util.stream.Collectors;

@AllArgsConstructor
@Getter
public class UserDetailsImpl implements UserDetails {

    private final UUID id;
    private final String email;
    private final String password;
    private final String fullName;
    private final Collection<? extends GrantedAuthority> authorities;
    private final Set<String> permissionStrings;
    private final User user;

    public static UserDetailsImpl build(User user) {
        Set<String> perms = new HashSet<>();

        // 1. Role-based permissions
        if (user.getRole() != null) {
            perms.add("ROLE_" + user.getRole().getName());
            if (user.getRole().getPermissions() != null) {
                for (Permission p : user.getRole().getPermissions()) {
                    perms.add(p.getAuthorityString());
                }
            }
        }

        // 2. Direct user overrides
        if (user.getDirectOverrides() != null) {
            for (UserPermissionOverride override : user.getDirectOverrides()) {
                if (override.getPermission() != null) {
                    String authStr = override.getPermission().getAuthorityString();
                    if (Boolean.TRUE.equals(override.getIsGranted())) {
                        perms.add(authStr);
                    } else {
                        perms.remove(authStr);
                    }
                }
            }
        }

        List<GrantedAuthority> authorities = perms.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        return new UserDetailsImpl(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getFullName(),
                authorities,
                perms,
                user
        );
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !"SUSPENDED".equalsIgnoreCase(user.getStatus());
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return !"INACTIVE".equalsIgnoreCase(user.getStatus());
    }
}
