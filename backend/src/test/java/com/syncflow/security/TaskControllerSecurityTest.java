package com.syncflow.security;

import com.syncflow.common.web.GlobalExceptionHandler;
import com.syncflow.dto.TaskDTO;
import com.syncflow.task.adapter.web.TaskController;
import com.syncflow.task.application.port.in.TaskUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that task endpoints are protected by both the role permission and the project-scoped
 * resource check, and that failures produce structured error bodies.
 */
@WebMvcTest(TaskController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class TaskControllerSecurityTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private TaskUseCase taskUseCase;
    @MockBean(name = "access")
    private AccessGuard access;
    @MockBean
    private JwtUtils jwtUtils;
    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    private static final String MOVE_BODY = "{\"targetStageId\": 5}";

    @Test
    void anonymousRequestsGet401() throws Exception {
        mvc.perform(get("/v1/tasks/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        verifyNoInteractions(taskUseCase);
    }

    @Test
    @WithMockUser
    void missingPermissionGets403() throws Exception {
        when(access.can(anyString())).thenReturn(false);
        when(access.task(anyLong())).thenReturn(true);

        mvc.perform(put("/v1/tasks/1/move").contentType(MediaType.APPLICATION_JSON).content(MOVE_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        verifyNoInteractions(taskUseCase);
    }

    @Test
    @WithMockUser
    void foreignProjectGets403EvenWithPermission() throws Exception {
        when(access.can("TASK:EDIT")).thenReturn(true);
        when(access.task(1L)).thenReturn(false);

        mvc.perform(put("/v1/tasks/1/move").contentType(MediaType.APPLICATION_JSON).content(MOVE_BODY))
                .andExpect(status().isForbidden());
        verifyNoInteractions(taskUseCase);
    }

    @Test
    @WithMockUser
    void permittedRequestReachesTheUseCase() throws Exception {
        when(access.can("TASK:EDIT")).thenReturn(true);
        when(access.task(1L)).thenReturn(true);
        when(taskUseCase.moveTask(anyLong(), any())).thenReturn(TaskDTO.builder().id(1L).build());

        mvc.perform(put("/v1/tasks/1/move").contentType(MediaType.APPLICATION_JSON).content(MOVE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
        verify(taskUseCase).moveTask(anyLong(), any());
    }

    @Test
    @WithMockUser
    void invalidBodyGetsStructured400() throws Exception {
        when(access.can(anyString())).thenReturn(true);
        when(access.task(anyLong())).thenReturn(true);

        mvc.perform(put("/v1/tasks/1/move").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }
}
