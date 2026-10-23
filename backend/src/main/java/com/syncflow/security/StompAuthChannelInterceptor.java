package com.syncflow.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Authenticates STOMP sessions and authorises every subscription.
 *
 * <ul>
 *   <li>CONNECT must carry {@code Authorization: Bearer <access token>}.</li>
 *   <li>SUBSCRIBE is limited to the user's own queue, project topics the user can access and
 *       channels the user may read.</li>
 *   <li>Clients may not SEND; all writes go through the authenticated REST API.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Pattern PROJECT_TOPIC = Pattern.compile("^/topic/projects\\.(\\d+)\\.(board|blockers)$");
    private static final Pattern CHANNEL_TOPIC = Pattern.compile("^/topic/channel\\.(\\d+)$");

    private final AuthTokenFilter authTokenFilter;
    private final AccessGuard accessGuard;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        switch (accessor.getCommand()) {
            case CONNECT -> authenticate(accessor);
            case SUBSCRIBE -> authorizeSubscription(accessor);
            case SEND -> throw new AccessDeniedException("Clients may not publish over STOMP");
            default -> {
            }
        }
        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new AccessDeniedException("Missing bearer token on STOMP CONNECT");
        }
        UserDetailsImpl user = authTokenFilter.authenticate(header.substring(7))
                .map(UserDetailsImpl.class::cast)
                .orElseThrow(() -> new AccessDeniedException("Invalid or expired token"));
        accessor.setUser(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        UserDetailsImpl user = principal(accessor.getUser());
        String destination = accessor.getDestination();
        if (user == null || destination == null) {
            throw new AccessDeniedException("Unauthenticated subscription");
        }

        if (destination.equals("/user/queue/notifications")) {
            return;
        }

        Matcher project = PROJECT_TOPIC.matcher(destination);
        if (project.matches()) {
            long projectId = Long.parseLong(project.group(1));
            if (CurrentUser.has(user, Permissions.PROJECT_VIEW) && accessGuard.project(user, projectId)) {
                return;
            }
            throw denied(user, destination);
        }

        Matcher chat = CHANNEL_TOPIC.matcher(destination);
        if (chat.matches()) {
            if (accessGuard.channel(user, Long.parseLong(chat.group(1)))) {
                return;
            }
            throw denied(user, destination);
        }

        throw denied(user, destination);
    }

    private AccessDeniedException denied(UserDetailsImpl user, String destination) {
        log.warn("Denied STOMP subscription of {} to {}", user.getEmail(), destination);
        return new AccessDeniedException("Subscription not permitted: " + destination);
    }

    private static UserDetailsImpl principal(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken token
                && token.getPrincipal() instanceof UserDetailsImpl details) {
            return details;
        }
        return null;
    }
}
