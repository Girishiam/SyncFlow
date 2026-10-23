package com.syncflow.security;

import com.syncflow.model.Channel;
import com.syncflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Resource-level authorization used from {@code @PreAuthorize} expressions as {@code @access}.
 *
 * <p>Permission checks ({@link #can(String)}) answer "may this role perform the action at all",
 * while the resource checks answer "may this user touch this particular project/channel".
 * Unknown ids resolve to {@code true} so that the service layer can answer with a 404.</p>
 */
@Component("access")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccessGuard {

    private final CurrentUser currentUser;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TaskRepository taskRepository;
    private final SprintRepository sprintRepository;
    private final AutomationRuleRepository automationRuleRepository;
    private final WebhookSubscriptionRepository webhookSubscriptionRepository;
    private final TaskDependencyRepository taskDependencyRepository;
    private final TaskTimeLogRepository taskTimeLogRepository;
    private final ChannelRepository channelRepository;
    private final StageRepository stageRepository;

    public boolean can(String authority) {
        return currentUser.has(authority);
    }

    public boolean isAdmin() {
        return currentUser.isAdmin();
    }

    public boolean project(Long projectId) {
        return currentUser.get().map(u -> project(u, projectId)).orElse(false);
    }

    public boolean project(UserDetailsImpl user, Long projectId) {
        if (user == null) return false;
        if (projectId == null || CurrentUser.isAdmin(user)) return true;
        if (!projectRepository.existsById(projectId)) return true;
        return projectRepository.isAccessibleBy(projectId, user.getId());
    }

    /** Admins, or a MANAGER / LEAD of this project, can change who is on the team. */
    public boolean manageTeam(Long projectId) {
        return currentUser.get().map(u -> {
            if (CurrentUser.isAdmin(u)) return true;
            if (!projectRepository.existsById(projectId)) return true;
            return projectMemberRepository.findByProjectIdAndUserId(projectId, u.getId())
                    .map(m -> m.getRole().canManageTeam())
                    .orElse(false);
        }).orElse(false);
    }

    public boolean task(Long taskId) {
        return viaProject(taskRepository.findProjectIdById(taskId));
    }

    public boolean stage(Long stageId) {
        return viaProject(stageRepository.findProjectIdById(stageId));
    }

    public boolean sprint(Long sprintId) {
        return viaProject(sprintRepository.findById(sprintId).map(s -> s.getProjectId()));
    }

    public boolean automation(Long ruleId) {
        return viaProject(automationRuleRepository.findProjectIdById(ruleId));
    }

    public boolean webhook(Long webhookId) {
        return viaProject(webhookSubscriptionRepository.findProjectIdById(webhookId));
    }

    public boolean dependency(Long dependencyId) {
        return viaProject(taskDependencyRepository.findById(dependencyId)
                .flatMap(d -> taskRepository.findProjectIdById(d.getSourceTaskId())));
    }

    public boolean timeLog(Long logId) {
        return viaProject(taskTimeLogRepository.findProjectIdById(logId));
    }

    public boolean channel(Long channelId) {
        return currentUser.get().map(u -> channel(u, channelId)).orElse(false);
    }

    /** Public channels are open to everyone; private channels only to members. Project channels also require project access. */
    public boolean channel(UserDetailsImpl user, Long channelId) {
        if (user == null) return false;
        if (CurrentUser.isAdmin(user)) return true;
        Optional<Channel> channel = channelRepository.findById(channelId);
        if (channel.isEmpty()) return true;
        Channel c = channel.get();
        if (c.getProject() != null && !project(user, c.getProject().getId())) {
            return false;
        }
        return !Boolean.TRUE.equals(c.getIsPrivate()) || channelRepository.isMember(channelId, user.getId());
    }

    private boolean viaProject(Optional<Long> projectId) {
        return projectId.map(this::project).orElseGet(() -> currentUser.get().isPresent());
    }
}
