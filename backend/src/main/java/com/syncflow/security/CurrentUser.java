package com.syncflow.security;

import com.syncflow.common.domain.ForbiddenException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Access to the authenticated principal of the current request.
 */
@Component
public class CurrentUser {

    public Optional<UserDetailsImpl> get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl details) {
            return Optional.of(details);
        }
        return Optional.empty();
    }

    public UserDetailsImpl require() {
        return get().orElseThrow(() -> new ForbiddenException("An authenticated user is required"));
    }

    public static boolean isAdmin(UserDetailsImpl user) {
        return user != null && user.getUser().getRole() != null
                && Permissions.ADMIN_ROLE.equalsIgnoreCase(user.getUser().getRole().getName());
    }

    public static boolean has(UserDetailsImpl user, String authority) {
        return user != null && (isAdmin(user) || user.getPermissionStrings().contains(authority));
    }

    public boolean isAdmin() {
        return get().map(CurrentUser::isAdmin).orElse(false);
    }

    public boolean has(String authority) {
        return get().map(u -> has(u, authority)).orElse(false);
    }
}
